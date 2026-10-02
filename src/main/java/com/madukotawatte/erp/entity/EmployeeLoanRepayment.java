package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee_loan_repayments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeLoanRepayment extends BaseEntity {

    public static final String SOURCE_PAYSLIP = "PAYSLIP";
    public static final String SOURCE_CASH = "CASH";

    @Id
    @Column(name = "repayment_id", length = 36)
    private String repaymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id")
    private EmployeeLoan loan;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "repayment_date", nullable = false)
    private LocalDate repaymentDate;

    // 'PAYSLIP' | 'CASH'
    @Column(nullable = false, length = 10)
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id")
    private Payslip payslip;

    // EMPLOYEE_LOAN_REPAYMENT ledger row (cash repayments only)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private MonetaryAssetTransaction transaction;
}
