package com.madukotawatte.erp.dto.load;

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
public class LoadSummaryResponse {
    private String loadId;
    private String loadType;
    private String status;
    private LocalDateTime startDate;

    /** Total latex collected across all records for this load (litres). */
    private BigDecimal totalLatexCollected;

    /** Total ammonia used across all records for this load. */
    private BigDecimal totalAmmoniaUsed;

    /** Number of individual latex collection entries. */
    private int recordCount;

    /** Latest collection timestamp (null if no records). */
    private LocalDateTime lastCollectionAt;
}
