package com.madukotawatte.erp.dto.labour;

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
public class LabourResponse {
    private String labourId;
    private String employeeId;
    private String employeeName;
    private String transactionRecordId;
    private Boolean isPaid;
    private BigDecimal workedHours;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
    private String workType;
    private String description;
    private LocalDateTime timestamp;
    private String paymentType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
