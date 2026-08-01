package com.madukotawatte.erp.dto.banana;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BananaVarietyBreakdownResponse {
    private String variety;
    private BigDecimal totalMassKg;
    private int totalBunchCount;
    private int recordCount;
}
