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
public class CoconutRecordResponse {
    private String recordId;
    private String loadId;
    private String employeeId;
    private String employeeName;
    private String variety;
    private String varietyNote;
    private Integer nutCount;
    private BigDecimal massKg;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
}
