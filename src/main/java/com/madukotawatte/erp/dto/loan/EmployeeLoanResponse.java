package com.madukotawatte.erp.dto.loan;

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
public class EmployeeLoanResponse {
    private String loanId;
    private String employeeId;
    private String employeeName;
    private BigDecimal principalAmount;
    private BigDecimal interest;
    private BigDecimal installment;
    private BigDecimal currentBalance;
    private Boolean isActive;
    private LocalDate issuedDate;
    /** EMPLOYEE_LOAN_ISSUE ledger row (null for loans that pre-date the ledger). */
    private String ledgerTransactionId;
    private BigDecimal totalRepaid;
    private List<LoanRepaymentResponse> repayments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
