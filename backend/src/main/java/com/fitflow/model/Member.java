package com.fitflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
/** 会员实体：保存联系信息、会籍、到期日期和到访统计。 */
public class Member {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  @NotBlank(message = "会员姓名不能为空")
  @Size(max = 80, message = "会员姓名不能超过 80 个字符")
  public String name;

  @Column(unique = true, nullable = false, length = 30)
  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = "1[3-9]\\d{9}", message = "请输入有效的 11 位手机号")
  public String phone;

  public String email;
  public String membershipType = "月卡";
  public LocalDate expireDate;
  public Integer visits = 0;
  public String status = "正常";
  public LocalDateTime createdAt = LocalDateTime.now();
}
