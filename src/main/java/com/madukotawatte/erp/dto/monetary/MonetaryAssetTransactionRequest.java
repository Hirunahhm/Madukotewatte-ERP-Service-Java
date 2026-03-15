package com.madukotawatte.erp.dto.monetary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonetaryAssetTransactionRequest {
    @NotBlank
    private String transactionType;

    @NotBlank
    private String assetType;

    @NotNull
    @Positive
    private BigDecimal amount;
}
