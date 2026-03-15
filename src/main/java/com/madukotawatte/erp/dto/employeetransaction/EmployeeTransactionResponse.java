package com.madukotawatte.erp.dto.employeetransaction;

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
public class EmployeeTransactionResponse {
    private String transactionRecordId;
    private String employeeId;
    private String employeeName;
    private String type;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
}
