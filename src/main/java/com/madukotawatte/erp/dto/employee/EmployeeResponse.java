package com.madukotawatte.erp.dto.employee;

import com.madukotawatte.erp.entity.PayType;
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
    private BigDecimal ratePerTree;
    private PayType payType;
    private BigDecimal hourlyRate;
    private String position;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
