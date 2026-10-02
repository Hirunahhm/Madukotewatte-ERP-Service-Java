package com.madukotawatte.erp.dto.cropharvest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropHarvestSummaryRow {
    private String crop;
    /** yyyy-MM */
    private String month;
    private String unit;
    private BigDecimal quantity;
    private long harvests;
}
