package com.madukotawatte.erp.dto.manioc;

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
public class ManiocLoadSummaryResponse {
    private String loadId;
    private String loadType;
    private String status;
    private LocalDateTime startDate;

    /** Total manioc mass harvested across all records for this load (kg). */
    private BigDecimal totalMassKg;

    /** Number of individual harvest entries. */
    private int recordCount;

    /** Latest harvest timestamp (null if no records). */
    private LocalDateTime lastCollectionAt;
}
