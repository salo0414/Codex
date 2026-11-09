package com.fitflow.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
/** 管理员登录会创建服务端会话；账号密码可通过环境变量覆盖默认开发凭据。 */
public class AuthController {

  @Value("${fitflow.admin.username:admin}")
  private String adminUsername;

  @Value("${fitflow.admin.password}")
  private String adminPassword;

  /** 校验管理员凭据并建立会话。 */
  @PostMapping("/login")
  public Map<String, String> login(
    @RequestBody LoginRequest credentials,
    HttpServletRequest request
  ) {
    if (
      !adminUsername.equals(credentials.username()) ||
      !adminPassword.equals(credentials.password())
    ) {
      throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED,
        "账号或密码不正确"
      );
    }
    HttpSession session = request.getSession(true);
    request.changeSessionId(); // 登录成功后轮换会话 ID，降低会话固定风险。
    session.setAttribute("fitflowAuthenticated", true);
    return Map.of("username", adminUsername);
  }

  /** 用于前端刷新页面时确认当前会话仍有效。 */
  @GetMapping("/me")
  public Map<String, String> me(HttpSession session) {
    requireLogin(session);
    return Map.of("username", adminUsername);
  }

  /** 注销会话，使当前浏览器不再访问受保护的业务接口。 */
  @PostMapping("/logout")
  public Map<String, String> logout(HttpSession session) {
    session.invalidate();
    return Map.of("message", "已退出登录");
  }

  static void requireLogin(HttpSession session) {
    if (
      session == null ||
      !Boolean.TRUE.equals(session.getAttribute("fitflowAuthenticated"))
    ) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
    }
  }

  public record LoginRequest(String username, String password) {}
}
