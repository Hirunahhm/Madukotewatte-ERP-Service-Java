package com.madukotawatte.erp.dto.coconut;

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
public class CoconutLoadSummaryResponse {
    private String loadId;
    private String loadType;
    private String status;
    private LocalDateTime startDate;

    /** Total coconut mass harvested across all records for this load (kg). */
    private BigDecimal totalMassKg;

    /** Total nuts harvested across all records for this load. */
    private int totalNutCount;

    /** Number of individual harvest entries. */
    private int recordCount;

    /** Latest harvest timestamp (null if no records). */
    private LocalDateTime lastCollectionAt;
}
