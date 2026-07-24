package com.madukotawatte.erp.dto.rubbersolid;

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
public class RubberLoadSummaryResponse {
    private String loadId;
    private String loadType;
    private String status;
    private LocalDateTime startDate;

    /** Total rubber mass collected across all records for this load (kg). */
    private BigDecimal totalMassKg;

    /** Number of individual collection entries. */
    private int recordCount;

    /** Latest collection timestamp (null if no records). */
    private LocalDateTime lastCollectionAt;
}
