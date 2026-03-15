package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeTransaction extends BaseEntity {

    @Id
    @Column(name = "transaction_record_id", length = 36)
    private String transactionRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    // 'Manual_Labor' | 'Advance' | 'Loan_Payment' | 'Latex_Tap'
    @Column(length = 20)
    private String type;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    private LocalDateTime timestamp;
}
