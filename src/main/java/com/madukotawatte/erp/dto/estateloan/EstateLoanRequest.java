package com.madukotawatte.erp.dto.estateloan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EstateLoanRequest {
    @NotBlank
    private String name;

    @NotNull
    @Positive
    private BigDecimal principalAmount;

    @NotNull
    private LocalDate issuedDate;

    @PositiveOrZero
    private BigDecimal interestRate;
}
