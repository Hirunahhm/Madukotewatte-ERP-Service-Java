package com.madukotawatte.erp.dto.ammonia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmmoniaBalanceResponse {
    /** Current ammonia tank stock (litres) — the newAmount of the most recent record. */
    private BigDecimal currentStock;
    private LocalDateTime lastUpdated;
    /** Percentage change vs. the stock level ~7 days before the latest record. */
    private double changeVsLastWeekPercent;
}
