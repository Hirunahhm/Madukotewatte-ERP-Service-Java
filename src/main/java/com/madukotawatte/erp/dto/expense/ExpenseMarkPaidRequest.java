package com.madukotawatte.erp.dto.expense;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseMarkPaidRequest {
    @NotBlank
    private String paymentType;

    private String monetaryTransactionId;
    private String estateLoanTransactionId;
}
