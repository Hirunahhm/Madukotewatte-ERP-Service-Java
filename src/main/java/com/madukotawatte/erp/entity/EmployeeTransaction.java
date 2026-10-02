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

    // 'Manual_Labor' | 'Advance' | 'Advance_Recovery' | 'Loan_Payment' | 'Latex_Tap' | 'Salary_Payment' (legacy)
    @Column(length = 20)
    private String type;

    // financial_accounts.code the money moved through (when applicable)
    @Column(name = "account_code", length = 40)
    private String accountCode;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    private LocalDateTime timestamp;

    // ledger row this transaction moved money through (advances); null otherwise
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private MonetaryAssetTransaction transaction;

    @Column(length = 255)
    private String note;

    // set once the transaction has been settled on a payslip (Latex_Tap) or was produced by one (recoveries)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id")
    private Payslip payslip;
}
