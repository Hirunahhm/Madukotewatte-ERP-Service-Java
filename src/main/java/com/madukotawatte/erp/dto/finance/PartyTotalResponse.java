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
public class PartyTotalResponse {
    /** Buyer (receivables) or payee (payables); "Unspecified" when none was recorded. */
    private String name;
    private int count;
    private BigDecimal total;
    private BigDecimal overdueTotal;
    private long oldestDays;
}
