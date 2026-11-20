package com.fitflow.api;

import com.fitflow.model.Member;
import com.fitflow.model.UserAccount;
import com.fitflow.repo.MemberRepository;
import com.fitflow.repo.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
/** 普通用户注册和登录，并在服务端会话中保存用户身份与角色。 */
public class AuthController {

  private final UserAccountRepository users;
  private final MemberRepository members;
  private final PasswordEncoder passwords;

  public AuthController(UserAccountRepository users, MemberRepository members, PasswordEncoder passwords) {
    this.users = users;
    this.members = members;
    this.passwords = passwords;
  }

  /** 注册账户并创建待激活档案；待管理员开通有效会籍后才能预约课程。 */
  @PostMapping("/register")
  @Transactional
  public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
    if (users.existsByUsername(request.phone()) || members.existsByPhone(request.phone())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该手机号已注册");
    }
    Member member = new Member();
    member.name = request.name().trim();
    member.phone = request.phone();
    member.status = "待激活";
    member.membershipType = "未开通";
    member = members.save(member);

    UserAccount account = new UserAccount();
    account.username = request.phone();
    account.passwordHash = passwords.encode(request.password());
    account.role = "USER";
    account.memberId = member.id;
    users.save(account);
    return Map.of("message", "注册成功，请等待管理员开通会籍后预约课程");
  }

  /** 验证账户密码并建立包含身份角色的 Redis 会话。 */
  @PostMapping("/login")
  public Map<String, String> login(@Valid @RequestBody LoginRequest credentials, HttpServletRequest request) {
    UserAccount account = users.findByUsername(credentials.username()).orElseThrow(() ->
      new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码不正确")
    );
    if (!passwords.matches(credentials.password(), account.passwordHash)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码不正确");
    }
    if (!account.role.equals(credentials.role())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "所选身份与账户类型不匹配");
    }
    if (account.memberId != null && members.findById(account.memberId).map(member -> "停用".equals(member.status)).orElse(false)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账户已停用，请联系管理员");
    }
    HttpSession session = request.getSession(true);
    request.changeSessionId();
    session.setAttribute("fitflowAuthenticated", true);
    session.setAttribute("fitflowUserId", account.id);
    session.setAttribute("fitflowRole", account.role);
    session.setAttribute("fitflowMemberId", account.memberId);
    session.setAttribute("fitflowUsername", account.username);
    return Map.of("username", account.username, "role", account.role);
  }

  /** 页面初始化时返回当前账户角色和关联会员档案。 */
  @GetMapping("/me")
  public Map<String, Object> me(HttpSession session) {
    requireLogin(session);
    return Map.of(
      "username", session.getAttribute("fitflowUsername"),
      "role", session.getAttribute("fitflowRole"),
      "memberId", session.getAttribute("fitflowMemberId") == null ? 0 : session.getAttribute("fitflowMemberId")
    );
  }

  /** 注销并销毁当前 Redis 会话。 */
  @PostMapping("/logout")
  public Map<String, String> logout(HttpSession session) {
    if (session != null) session.invalidate();
    return Map.of("message", "已退出登录");
  }

  static void requireLogin(HttpSession session) {
    if (session == null || !Boolean.TRUE.equals(session.getAttribute("fitflowAuthenticated"))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
    }
  }

  static void requireAdmin(HttpSession session) {
    requireLogin(session);
    if (!"ADMIN".equals(session.getAttribute("fitflowRole"))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理员权限");
    }
  }

  public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank @Pattern(regexp = "ADMIN|USER", message = "请选择有效的登录身份") String role
  ) {}

  public record RegisterRequest(
    @NotBlank @Size(max = 80) String name,
    @NotBlank @Pattern(regexp = "1[3-9]\\d{9}", message = "请输入有效的 11 位手机号") String phone,
    @NotBlank @Size(min = 8, max = 72, message = "密码长度须为 8 至 72 个字符") String password
  ) {}
}
