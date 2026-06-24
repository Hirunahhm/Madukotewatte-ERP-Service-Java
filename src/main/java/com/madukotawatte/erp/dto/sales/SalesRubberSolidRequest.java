package com.madukotawatte.erp.dto.sales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalesRubberSolidRequest {
    @NotBlank
    private String loadId;

    @NotNull
    private LocalDate saleDate;

    @NotNull
    @Positive
    private BigDecimal mass;

    @NotNull
    @Positive
    private BigDecimal unitPrice;

    private Boolean isPaid = false;

    private String status = "pending";

    private String paymentType;
}
