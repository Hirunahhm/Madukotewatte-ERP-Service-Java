package com.madukotawatte.erp.dto.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLoanResponse {
    private String loanId;
    private String employeeId;
    private String employeeName;
    private BigDecimal principalAmount;
    private BigDecimal interest;
    private BigDecimal installment;
    private BigDecimal currentBalance;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
