package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance extends BaseEntity {

    @Id
    @Column(name = "attendance_id", length = 36)
    private String attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private LocalDateTime timestamp;

    @Column(name = "no_of_trees")
    private Integer noOfTrees;

    // 'none' | 'rain' | 'ill' | 'no_loads' | 'holiday'
    @Column(name = "no_work", length = 10)
    private String noWork = "none";
}
