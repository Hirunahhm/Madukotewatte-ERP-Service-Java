package com.madukotawatte.erp.dto.sales;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    @DecimalMax("150")
    private BigDecimal metrolacReading;

    @NotNull
    @Positive
    private BigDecimal unitPrice;

    @Size(max = 150)
    private String buyerName;

    /** Date the buyer is expected to pay (credit sales). */
    private LocalDate dueDate;
}
