package com.fitflow.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="members")
/** 会员实体：保存联系信息、会籍、到期日期和到访统计。 */
public class Member {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public String name;
    @Column(unique=true, nullable=false, length=30) public String phone;
    public String email;
    public String membershipType = "月卡";
    public LocalDate expireDate;
    public Integer visits = 0;
    public String status = "正常";
    public LocalDateTime createdAt = LocalDateTime.now();
}
