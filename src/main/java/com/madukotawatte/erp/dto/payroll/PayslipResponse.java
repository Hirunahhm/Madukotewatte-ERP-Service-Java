package com.madukotawatte.erp.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipResponse {
    private String payslipId;
    private String employeeId;
    private String employeeName;
    private String position;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal fixedSalary;
    private BigDecimal tapEarnings;
    private BigDecimal hourlyEarnings;
    private BigDecimal otherEarnings;
    private String otherEarningsNote;
    private BigDecimal grossPay;
    private BigDecimal advanceDeduction;
    private BigDecimal loanDeduction;
    private BigDecimal otherDeduction;
    private String otherDeductionNote;
    private BigDecimal netPay;
    private String accountCode;
    private LocalDate paymentDate;
    /** SALARY_PAYMENT ledger row id (null when net pay is zero). */
    private String ledgerTransactionId;
    private String loanId;
    private String notes;
    /** PAID | VOID */
    private String status;
    private String voidReason;
    private LocalDateTime voidedAt;
    /** Linked per-day tapping earnings (empty once voided). */
    private List<PayslipTapLine> tapLines;
    /** Linked labour lines (empty once voided). */
    private List<PayslipLabourLine> labourLines;
    private LocalDateTime createdAt;
}
