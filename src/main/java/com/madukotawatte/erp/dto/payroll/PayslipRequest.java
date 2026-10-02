package com.madukotawatte.erp.dto.payroll;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Earnings are recomputed by the server; only extras and deductions come from the client. */
@Data
public class PayslipRequest {
    @NotBlank
    private String employeeId;

    @NotNull
    private LocalDate periodStart;

    @NotNull
    private LocalDate periodEnd;

    private BigDecimal otherEarnings;
    private String otherEarningsNote;

    private BigDecimal advanceDeduction;
    private BigDecimal loanDeduction;
    private BigDecimal otherDeduction;
    private String otherDeductionNote;

    // cash or bank account the net pay is paid from
    @NotBlank
    private String accountCode;

    @NotNull
    private LocalDate paymentDate;

    private String notes;
}
