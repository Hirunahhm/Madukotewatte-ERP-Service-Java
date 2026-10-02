package com.madukotawatte.erp.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipSummaryResponse {
    private String payslipId;
    private String employeeId;
    private String employeeName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate paymentDate;
    private BigDecimal fixedSalary;
    private BigDecimal grossPay;
    private BigDecimal netPay;
    private String accountCode;
    private String status;
}
