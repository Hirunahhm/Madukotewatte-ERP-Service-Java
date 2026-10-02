package com.madukotawatte.erp.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** KPI cards of the Payroll screen. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {
    /** Monthly cost of the active fixed-salary employees. */
    private BigDecimal totalSalaryCost;
    /** Payslips paid (payment date) in the current month: gross earnings. */
    private BigDecimal payrollPaidGross;
    /** Payslips paid in the current month: money that left the accounts. */
    private BigDecimal payrollPaidNet;
    /** Earned this month to date by active employees and not yet on a payslip. */
    private BigDecimal unpaidEarnings;
    /** Advances given and not yet recovered, all employees. */
    private BigDecimal outstandingAdvances;
    /** Balance of active employee loans. */
    private BigDecimal activeEmployeeLoansTotal;
}
