package com.madukotawatte.erp.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSummaryRow {
    private String employeeId;
    private String employeeName;
    private int payslipCount;
    private BigDecimal gross;
    private BigDecimal fixedSalary;
    private BigDecimal tap;
    private BigDecimal hourly;
    private BigDecimal other;
    private BigDecimal advanceRecovered;
    private BigDecimal loanDeducted;
    private BigDecimal otherDeducted;
    private BigDecimal net;
}
