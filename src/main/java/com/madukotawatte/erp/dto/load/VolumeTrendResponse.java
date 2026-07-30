package com.madukotawatte.erp.dto.load;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolumeTrendResponse {
    private String name; // e.g. "Mon" or "2026-07-24"
    private BigDecimal actual;
    private BigDecimal target;
}
