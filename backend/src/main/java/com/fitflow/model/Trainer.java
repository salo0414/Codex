package com.fitflow.model;

import jakarta.persistence.*;

@Entity @Table(name="trainers")
/** 教练资料实体，供课程排课及团队页面使用。 */
public class Trainer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public String name;
    public String specialty;
    public String phone;
    public String avatar;
}
