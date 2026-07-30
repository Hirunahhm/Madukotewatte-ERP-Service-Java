package com.madukotawatte.erp.dto.sales;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SalesMarkPaidRequest {
    @NotBlank
    private String paymentType;
}
