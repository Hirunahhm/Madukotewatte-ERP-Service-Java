package com.madukotawatte.erp.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesLedgerRowResponse {
    private String saleId;
    private String category;
    private String loadId;
    private LocalDate saleDate;
    private BigDecimal amount;
    private String status;
    private String paymentType;
}
