package com.madukotawatte.erp.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgingBucketResponse {
    /** 0-30 | 31-60 | 61-90 | 90+ (days since the document date). */
    private String bucket;
    private int count;
    private BigDecimal total;
}
