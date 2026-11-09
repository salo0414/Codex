package com.fitflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="fitness_classes")
/** 团体课程实体：课程容量与已预约人数用于控制可预约名额。 */
public class FitnessClass {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public String name;
    public String category;
    public String trainer;
    public LocalDateTime startTime;
    public Integer duration = 60;
    public Integer capacity = 20;
    public Integer booked = 0;
    public String room;
}
