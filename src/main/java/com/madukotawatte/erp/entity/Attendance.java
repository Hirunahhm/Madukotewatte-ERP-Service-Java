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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id")
    private Calendar calendar;

    private LocalDateTime timestamp;

    @Column(name = "no_of_trees")
    private Integer noOfTrees;

    // 'none' | 'rain' | 'sick' | 'other' | 'public_holiday' | 'funeral' | 'family_matter' | 'kids'
    @Column(name = "no_work", length = 20)
    private String noWork = "none";
}
