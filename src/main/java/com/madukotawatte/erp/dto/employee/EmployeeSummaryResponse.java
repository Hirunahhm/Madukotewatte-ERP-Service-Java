package com.madukotawatte.erp.dto.employee;

import com.madukotawatte.erp.entity.PayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSummaryResponse {
    private String employeeId;
    private String name;
    private String position;
    private BigDecimal salary;
    private PayType payType;
    private BigDecimal ratePerTree;
    private BigDecimal hourlyRate;
    private Boolean isActive;
}
