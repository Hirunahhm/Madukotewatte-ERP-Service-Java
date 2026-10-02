package com.madukotawatte.erp.dto.finance;

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
public class ReceivableRowResponse {
    /** latex | rubber-solid | manioc | coconut | banana | general */
    private String saleType;
    private String saleId;
    /** Product/category label, e.g. "latex" or "Cocoa". */
    private String product;
    private String buyerName;
    private LocalDate saleDate;
    private LocalDate dueDate;
    private BigDecimal amount;
    private long daysOutstanding;
    private String bucket;
    private boolean overdue;
}
