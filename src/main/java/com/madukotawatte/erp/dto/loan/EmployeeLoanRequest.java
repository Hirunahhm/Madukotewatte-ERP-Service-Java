package com.madukotawatte.erp.dto.loan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmployeeLoanRequest {
    @NotBlank
    private String employeeId;

    @NotNull
    @Positive
    private BigDecimal principalAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal interest;

    @NotNull
    @Positive
    private BigDecimal installment;
}
