package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One payment of wages to one employee for a period. gross = fixed + tap + hourly + other earnings. */
@Entity
@Table(name = "payslips")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payslip extends BaseEntity {

    public static final String PAID = "PAID";
    public static final String VOID = "VOID";

    @Id
    @Column(name = "payslip_id", length = 36)
    private String payslipId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "fixed_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal fixedSalary;

    @Column(name = "tap_earnings", nullable = false, precision = 15, scale = 2)
    private BigDecimal tapEarnings;

    @Column(name = "hourly_earnings", nullable = false, precision = 15, scale = 2)
    private BigDecimal hourlyEarnings;

    @Column(name = "other_earnings", nullable = false, precision = 15, scale = 2)
    private BigDecimal otherEarnings;

    @Column(name = "other_earnings_note")
    private String otherEarningsNote;

    @Column(name = "gross_pay", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossPay;

    @Column(name = "advance_deduction", nullable = false, precision = 15, scale = 2)
    private BigDecimal advanceDeduction;

    @Column(name = "loan_deduction", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanDeduction;

    @Column(name = "other_deduction", nullable = false, precision = 15, scale = 2)
    private BigDecimal otherDeduction;

    @Column(name = "other_deduction_note")
    private String otherDeductionNote;

    @Column(name = "net_pay", nullable = false, precision = 15, scale = 2)
    private BigDecimal netPay;

    @Column(name = "account_code", nullable = false, length = 40)
    private String accountCode;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    // SALARY_PAYMENT ledger row (null when net pay is 0)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private MonetaryAssetTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private EmployeeLoan loan;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, length = 10)
    private String status = PAID;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;
}
