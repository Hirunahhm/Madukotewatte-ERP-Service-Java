package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "labour")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Labour extends BaseEntity {

    @Id
    @Column(name = "labour_id", length = 36)
    private String labourId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_record_id")
    private EmployeeTransaction employeeTransaction;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(name = "worked_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal workedHours;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "work_type", nullable = false, length = 50)
    private String workType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime timestamp;

    // 'bank_transfer' | 'cash'
    @Column(name = "payment_type", length = 20)
    private String paymentType;
}
