package com.fitflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 登录账户；密码字段只保存 BCrypt 摘要，不保存明文。 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 80)
  public String username;

  @Column(nullable = false, length = 100)
  public String passwordHash;

  @Column(nullable = false, length = 20)
  public String role;

  @Column(unique = true)
  public Long memberId;

  @Column(nullable = false)
  public LocalDateTime createdAt = LocalDateTime.now();
}
