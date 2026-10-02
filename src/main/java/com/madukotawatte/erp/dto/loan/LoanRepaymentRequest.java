package com.madukotawatte.erp.dto.loan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The employee repays part of the loan in cash/bank (money in, EMPLOYEE_LOAN_REPAYMENT). */
@Data
public class LoanRepaymentRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String accountCode;

    @NotNull
    private LocalDate date;
}
