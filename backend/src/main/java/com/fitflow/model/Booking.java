package com.fitflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="bookings")
/** 课程预约实体，通过会员 ID 与课程 ID 关联业务记录。 */
public class Booking {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Long memberId;
    @Column(nullable=false) public Long classId;
    public String status = "已预约";
    public LocalDateTime bookedAt = LocalDateTime.now();
}
