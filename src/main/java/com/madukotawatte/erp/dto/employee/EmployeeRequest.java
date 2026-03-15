package com.madukotawatte.erp.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeRequest {
    @NotBlank
    private String name;

    @NotNull
    private LocalDate joinedDate;

    @NotNull
    @Positive
    private BigDecimal salary;

    private String position;
}
