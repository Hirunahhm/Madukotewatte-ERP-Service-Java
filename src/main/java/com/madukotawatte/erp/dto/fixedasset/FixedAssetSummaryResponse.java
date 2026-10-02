package com.madukotawatte.erp.dto.fixedasset;

import com.madukotawatte.erp.dto.finance.CategoryTotalResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetSummaryResponse {
    private long totalCount;
    private BigDecimal totalAcquisitionValue;
    /** Total net book value today of active assets. */
    private BigDecimal totalCurrentValue;
    private BigDecimal totalAccumulatedDepreciation;
    private List<CategoryTotalResponse> byCategory;
}
