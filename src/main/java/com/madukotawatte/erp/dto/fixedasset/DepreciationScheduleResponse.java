package com.madukotawatte.erp.dto.fixedasset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepreciationScheduleResponse {
    private LocalDate from;
    private LocalDate to;
    private List<DepreciationRowResponse> assets;
    /** Sum per category (assetId and name are null). */
    private List<DepreciationRowResponse> categories;
    private DepreciationRowResponse total;
}
