package com.madukotawatte.erp.dto.loan;

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
public class LoanRepaymentResponse {
    private String repaymentId;
    private String loanId;
    private BigDecimal amount;
    private LocalDate repaymentDate;
    /** PAYSLIP | CASH */
    private String source;
    private String payslipId;
    private String accountCode;
    private String ledgerTransactionId;
}
