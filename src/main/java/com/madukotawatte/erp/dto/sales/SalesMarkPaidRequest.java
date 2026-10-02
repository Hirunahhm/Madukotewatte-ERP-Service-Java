package com.madukotawatte.erp.dto.sales;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SalesMarkPaidRequest {

    /** financial_accounts.code the money was received into (cash or bank). */
    @NotBlank
    private String accountCode;

    /** Day the money was received; defaults to today. */
    private LocalDate paymentDate;
}
