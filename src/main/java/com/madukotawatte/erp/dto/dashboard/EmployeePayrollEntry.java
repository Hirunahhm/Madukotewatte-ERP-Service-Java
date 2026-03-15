package com.madukotawatte.erp.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePayrollEntry {
    private String employeeId;
    private String employeeName;
    private BigDecimal salary;
    private int workDays;
    private BigDecimal latexTap;
    private BigDecimal advances;
    private BigDecimal loanDeductions;
    private BigDecimal manualLabor;
    private BigDecimal net;
}
