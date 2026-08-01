package com.madukotawatte.erp.dto.manioc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManiocVarietyBreakdownResponse {
    private String variety;
    private BigDecimal totalMassKg;
    private int recordCount;
}
