package com.madukotawatte.erp.dto.payroll;

import com.madukotawatte.erp.entity.PayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** What one employee is owed for a period and what would normally be deducted. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPreviewRow {
    private String employeeId;
    private String employeeName;
    private String position;
    private PayType payType;
    private BigDecimal salary;
    private BigDecimal ratePerTree;
    private BigDecimal hourlyRate;
    private BigDecimal fixedSalary;
    private BigDecimal tapEarnings;
    private int tapRecordCount;
    private BigDecimal hourlyEarnings;
    private BigDecimal labourHours;
    private BigDecimal grossPay;
    private BigDecimal outstandingAdvances;
    private BigDecimal suggestedAdvanceDeduction;
    private ActiveLoanInfo activeLoan;
    private BigDecimal suggestedLoanDeduction;
    private BigDecimal netPay;
    /** PAID payslips that overlap the requested period (to prevent paying twice). */
    private List<PayslipSummaryResponse> alreadyPaid;
}
