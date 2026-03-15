package com.madukotawatte.erp.dto.monetary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetBalanceResponse {
    private String assetType;
    private BigDecimal balance;
}
