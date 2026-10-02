package com.madukotawatte.erp.dto.sales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalesGeneralRequest {

    /** One of SalesGeneralCatalog.PRODUCTS. */
    @NotBlank
    private String product;

    @Size(max = 255)
    private String description;

    @NotNull
    @Positive
    private BigDecimal quantity;

    /** One of SalesGeneralCatalog.UNITS. */
    @NotBlank
    private String unit;

    @NotNull
    @PositiveOrZero
    private BigDecimal unitPrice;

    @Size(max = 150)
    private String buyerName;

    @NotNull
    private LocalDate saleDate;

    private LocalDate dueDate;

    /** Create only: true records the receipt immediately. */
    private Boolean isPaid = false;

    /** Cash/bank account the money was received into (required when isPaid). */
    private String accountCode;

    /** Day the money was received; defaults to the sale date. Only used when isPaid=true. */
    private LocalDate paymentDate;
}
