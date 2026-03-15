package com.madukotawatte.erp.dto.sales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesLatexRequest {
    @NotBlank
    private String loadId;

    @NotNull
    @Positive
    private BigDecimal mass;

    @NotNull
    @Positive
    private BigDecimal litres;

    @NotNull
    @Positive
    private BigDecimal metrolacReading;

    @NotNull
    @Positive
    private BigDecimal unitPrice;
}
