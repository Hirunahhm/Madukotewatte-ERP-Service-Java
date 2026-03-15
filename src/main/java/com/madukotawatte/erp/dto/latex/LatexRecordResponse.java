package com.madukotawatte.erp.dto.latex;

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
public class LatexRecordResponse {
    private String recordId;
    private String loadId;
    private String employeeId;
    private String employeeName;
    private LocalDateTime timestamp;
    private BigDecimal latexAmount;
    private BigDecimal ammoniaAmount;
    private String metrolacReadingId;
    private LocalDateTime createdAt;
}
