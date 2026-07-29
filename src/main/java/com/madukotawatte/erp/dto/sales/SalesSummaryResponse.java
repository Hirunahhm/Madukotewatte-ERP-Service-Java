package com.madukotawatte.erp.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryResponse {
    private BigDecimal totalSales;
    private BigDecimal received;
    private BigDecimal pending;
}
