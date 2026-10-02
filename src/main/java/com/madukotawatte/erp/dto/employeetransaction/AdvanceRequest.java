package com.madukotawatte.erp.dto.employeetransaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An advance paid to an employee before pay day. Posts EMPLOYEE_ADVANCE from the chosen cash/bank account. */
@Data
public class AdvanceRequest {
    @NotBlank
    private String employeeId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String accountCode;

    @NotNull
    private LocalDate date;

    private String note;
}
