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
    private String accountCode;
    /** latex | rubber-solid | manioc | coconut | banana | general - selects the payment/edit endpoints. */
    private String saleType;
    private String buyerName;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private Boolean overdue;
}
