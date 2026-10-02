package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee extends BaseEntity {

    @Id
    @Column(name = "employee_id", length = 36)
    private String employeeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    private BigDecimal salary;

    @Column(name = "rate_per_tree")
    private BigDecimal ratePerTree = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_type", nullable = false, length = 20)
    private PayType payType = PayType.FIXED_MONTHLY;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    private String position;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
