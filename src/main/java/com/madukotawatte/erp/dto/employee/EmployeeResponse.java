package com.madukotawatte.erp.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private String employeeId;
    private String name;
    private LocalDate joinedDate;
    private BigDecimal salary;
    private String position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
