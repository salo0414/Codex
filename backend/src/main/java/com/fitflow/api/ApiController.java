package com.fitflow.api;

import com.fitflow.model.*;
import com.fitflow.repo.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
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

  public ApiController(
    MemberRepository m,
    FitnessClassRepository c,
    TrainerRepository t,
    BookingRepository b
  ) {
    members = m;
    classes = c;
    trainers = t;
    bookings = b;
  }

  /** 汇总会员、课程、教练数量，并返回近期排课。 */
  @GetMapping("/dashboard")
  public Map<String, Object> dashboard() {
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
  public List<Member> members() {
    return members.findAll();
  }

  @PostMapping("/members")
  public Member addMember(@Valid @RequestBody Member m) {
    return members.save(m);
  }

  @PutMapping("/members/{id}")
  public Member updateMember(@PathVariable Long id, @RequestBody Member m) {
    if (!members.existsById(id)) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND
    );
    m.id = id;
    return members.save(m);
  }

  @DeleteMapping("/members/{id}")
  public void deleteMember(@PathVariable Long id) {
    members.deleteById(id);
  }

  /** 团体课程的查询与维护。 */
  @GetMapping("/classes")
  public List<FitnessClass> classes() {
    return classes.findAll();
  }

  @PostMapping("/classes")
  public FitnessClass addClass(@RequestBody FitnessClass c) {
    return classes.save(c);
  }

  @PutMapping("/classes/{id}")
  public FitnessClass updateClass(
    @PathVariable Long id,
    @RequestBody FitnessClass c
  ) {
    if (!classes.existsById(id)) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND
    );
    c.id = id;
    return classes.save(c);
  }

  @DeleteMapping("/classes/{id}")
  public void deleteClass(@PathVariable Long id) {
    classes.deleteById(id);
  }

  /** 教练资料的查询、新增与删除。 */
  @GetMapping("/trainers")
  public List<Trainer> trainers() {
    return trainers.findAll();
  }

  @PostMapping("/trainers")
  public Trainer addTrainer(@RequestBody Trainer t) {
    return trainers.save(t);
  }

  @DeleteMapping("/trainers/{id}")
  public void deleteTrainer(@PathVariable Long id) {
    trainers.deleteById(id);
  }

  /** 预约管理：列表补充会员和课程名称，新增时校验课程容量，取消时回收名额。 */
  @GetMapping("/bookings")
  public List<Map<String, Object>> bookingList() {
    return bookings.findAll().stream().map(this::bookingView).toList();
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
  public Booking addBooking(@RequestBody Booking b) {
    FitnessClass c = classes
      .findById(b.classId)
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在")
      );
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
  public void deleteBooking(@PathVariable Long id) {
    Booking b = bookings
      .findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    classes.findById(b.classId).ifPresent(c -> {
      c.booked = Math.max(0, c.booked - 1);
      classes.save(c);
    });
    bookings.delete(b);
  }
}
