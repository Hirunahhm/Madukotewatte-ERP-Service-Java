package com.madukotawatte.erp.dto.ammonia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmmoniaUsageResponse {
    private String name; // e.g. "Mon" or "Jul 24"
    private BigDecimal refill;
    private BigDecimal out;
}
