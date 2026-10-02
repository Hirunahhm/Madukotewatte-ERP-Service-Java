package com.madukotawatte.erp.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayablesResponse {
    private LocalDate asOf;
    private BigDecimal total;
    private BigDecimal overdueTotal;
    private List<PayableRowResponse> rows;
    private List<AgingBucketResponse> buckets;
    private List<PartyTotalResponse> byPayee;
}
