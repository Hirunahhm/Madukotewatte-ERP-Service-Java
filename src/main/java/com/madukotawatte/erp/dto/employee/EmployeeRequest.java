package com.madukotawatte.erp.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.madukotawatte.erp.entity.PayType;
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

    @Positive
    private BigDecimal salary;

    private BigDecimal ratePerTree;

    // FIXED_MONTHLY needs salary, PER_TREE needs ratePerTree, HOURLY needs hourlyRate (validated in the service).
    // When omitted it is derived from the position ("Tapper" -> PER_TREE, else FIXED_MONTHLY).
    private PayType payType;

    @Positive
    private BigDecimal hourlyRate;

    private String position;

    private Boolean isActive;
}
