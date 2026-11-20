package com.fitflow.api;

import com.fitflow.model.*;
import com.fitflow.repo.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
/** 健身房核心业务接口；AuthenticationFilter 会在请求进入此控制器前校验会话。 */
public class ApiController {

  private final MemberRepository members;
  private final FitnessClassRepository classes;
  private final TrainerRepository trainers;
  private final BookingRepository bookings;
  private final UserAccountRepository users;

  public ApiController(
    MemberRepository m,
    FitnessClassRepository c,
    TrainerRepository t,
    BookingRepository b,
    UserAccountRepository u
  ) {
    members = m;
    classes = c;
    trainers = t;
    bookings = b;
    users = u;
  }

  /** 汇总会员、课程、教练数量，并返回近期排课。 */
  @GetMapping("/dashboard")
  public Map<String, Object> dashboard(HttpSession session) {
    AuthController.requireAdmin(session);
    LocalDateTime now = LocalDateTime.now();
    return Map.of(
      "memberCount",
      members.count(),
      "activeMembers",
      members.countByStatus("正常"),
      "newToday",
      members.countNewToday(),
      "classCount",
      classes.countByStartTimeBetween(now, now.plusDays(1)),
      "trainerCount",
      trainers.count(),
      "upcomingClasses",
      classes.findTop6ByStartTimeAfterOrderByStartTimeAsc(now)
    );
  }

  /** 会员档案的查询、新增、修改与删除。 */
  @GetMapping("/members")
  public List<Member> members(HttpSession session) {
    AuthController.requireAdmin(session);
    return members.findAll();
  }

  @PostMapping("/members")
  public Member addMember(@Valid @RequestBody Member m, HttpSession session) {
    AuthController.requireAdmin(session);
    return members.save(m);
  }

  @PutMapping("/members/{id}")
  public Member updateMember(@PathVariable Long id, @Valid @RequestBody Member m, HttpSession session) {
    AuthController.requireAdmin(session);
    Member current = members.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!current.phone.equals(m.phone)) {
      users.findByMemberId(id).ifPresent(account -> {
        account.username = m.phone;
        users.save(account);
      });
    }
    m.id = id;
    return members.save(m);
  }

  @DeleteMapping("/members/{id}")
  public void deleteMember(@PathVariable Long id, HttpSession session) {
    AuthController.requireAdmin(session);
    users.deleteByMemberId(id);
    members.deleteById(id);
  }

  /** 团体课程的查询与维护。 */
  @GetMapping("/classes")
  public List<FitnessClass> classes() {
    return classes.findAll();
  }

  @PostMapping("/classes")
  public FitnessClass addClass(@RequestBody FitnessClass c, HttpSession session) {
    AuthController.requireAdmin(session);
    return classes.save(c);
  }

  @PutMapping("/classes/{id}")
  public FitnessClass updateClass(
    @PathVariable Long id,
    @RequestBody FitnessClass c,
    HttpSession session
  ) {
    AuthController.requireAdmin(session);
    if (!classes.existsById(id)) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND
    );
    c.id = id;
    return classes.save(c);
  }

  @DeleteMapping("/classes/{id}")
  public void deleteClass(@PathVariable Long id, HttpSession session) {
    AuthController.requireAdmin(session);
    classes.deleteById(id);
  }

  /** 教练资料的查询、新增与删除。 */
  @GetMapping("/trainers")
  public List<Trainer> trainers(HttpSession session) {
    AuthController.requireAdmin(session);
    return trainers.findAll();
  }

  @PostMapping("/trainers")
  public Trainer addTrainer(@RequestBody Trainer t, HttpSession session) {
    AuthController.requireAdmin(session);
    return trainers.save(t);
  }

  @DeleteMapping("/trainers/{id}")
  public void deleteTrainer(@PathVariable Long id, HttpSession session) {
    AuthController.requireAdmin(session);
    trainers.deleteById(id);
  }

  /** 预约管理：列表补充会员和课程名称，新增时校验课程容量，取消时回收名额。 */
  @GetMapping("/bookings")
  public List<Map<String, Object>> bookingList(HttpSession session) {
    if ("ADMIN".equals(session.getAttribute("fitflowRole"))) {
      return bookings.findAll().stream().map(this::bookingView).toList();
    }
    Long memberId = (Long) session.getAttribute("fitflowMemberId");
    return bookings.findAll().stream().filter(b -> b.memberId.equals(memberId)).map(this::bookingView).toList();
  }

  private Map<String, Object> bookingView(Booking b) {
    Map<String, Object> v = new LinkedHashMap<>();
    v.put("id", b.id);
    v.put("memberId", b.memberId);
    v.put("classId", b.classId);
    v.put("status", b.status);
    v.put("bookedAt", b.bookedAt);
    members.findById(b.memberId).ifPresent(m -> v.put("memberName", m.name));
    classes.findById(b.classId).ifPresent(c -> v.put("className", c.name));
    return v;
  }

  @PostMapping("/bookings")
  @Transactional
  public Booking addBooking(@RequestBody Booking b, HttpSession session) {
    boolean isAdmin = "ADMIN".equals(session.getAttribute("fitflowRole"));
    if (!isAdmin) {
      b.memberId = (Long) session.getAttribute("fitflowMemberId");
      Member member = members.findById(b.memberId).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "账户未关联会员档案"));
      if (!"正常".equals(member.status) || member.expireDate == null || member.expireDate.isBefore(LocalDate.now())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请先联系管理员开通有效会籍");
      }
    }
    FitnessClass c = classes
      .findByIdForUpdate(b.classId)
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在")
      );
    if (c.startTime == null || !c.startTime.isAfter(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "课程已开始或时间未设置，不能预约");
    }
    if (bookings.existsByMemberIdAndClassId(b.memberId, b.classId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "你已经预约过这节课程");
    }
    if (!members.existsById(b.memberId)) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "会员不存在"
    );
    if (c.booked >= c.capacity) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "课程名额已满"
    );
    c.booked++;
    classes.save(c);
    return bookings.save(b);
  }

  @DeleteMapping("/bookings/{id}")
  @Transactional
  public void deleteBooking(@PathVariable Long id, HttpSession session) {
    Booking b = bookings
      .findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!"ADMIN".equals(session.getAttribute("fitflowRole")) && !Objects.equals(b.memberId, session.getAttribute("fitflowMemberId"))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能取消自己的预约");
    }
    classes.findByIdForUpdate(b.classId).ifPresent(c -> {
      if (!"ADMIN".equals(session.getAttribute("fitflowRole")) && c.startTime != null && !c.startTime.isAfter(LocalDateTime.now())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "课程开始后不能取消预约");
      }
      c.booked = Math.max(0, c.booked - 1);
      classes.save(c);
    });
    bookings.delete(b);
  }

  /** 用户只读取自己的会员档案。 */
  @GetMapping("/profile")
  public Member profile(HttpSession session) {
    Long memberId = (Long) session.getAttribute("fitflowMemberId");
    return members.findById(memberId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会员档案不存在"));
  }

  /** 用户可以更新自己的姓名和手机号，并同步登录账号。 */
  @PutMapping("/profile")
  @Transactional
  public Member updateProfile(@Valid @RequestBody ProfileUpdate update, HttpSession session) {
    Long memberId = (Long) session.getAttribute("fitflowMemberId");
    Member member = members.findById(memberId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会员档案不存在"));
    if (!member.phone.equals(update.phone()) && members.existsByPhone(update.phone())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该手机号已被使用");
    }
    UserAccount account = users.findById((Long) session.getAttribute("fitflowUserId")).orElseThrow();
    member.name = update.name().trim();
    member.phone = update.phone();
    account.username = update.phone();
    users.save(account);
    session.setAttribute("fitflowUsername", update.phone());
    return members.save(member);
  }

  /** 管理员查看账户、角色及会员激活状态，不返回密码摘要。 */
  @GetMapping("/users")
  public List<Map<String, Object>> userAccounts(HttpSession session) {
    AuthController.requireAdmin(session);
    return users.findAll().stream().map(account -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", account.id);
      row.put("username", account.username);
      row.put("role", account.role);
      row.put("memberId", account.memberId);
      row.put("memberName", account.memberId == null ? "管理员账户" : members.findById(account.memberId).map(m -> m.name).orElse("会员档案不存在"));
      row.put("status", account.memberId == null ? "正常" : members.findById(account.memberId).map(m -> m.status).orElse("未知"));
      return row;
    }).toList();
  }

  public record ProfileUpdate(
    @NotBlank @Size(max = 80) String name,
    @NotBlank @Pattern(regexp = "1[3-9]\\d{9}", message = "请输入有效的 11 位手机号") String phone
  ) {}
}
