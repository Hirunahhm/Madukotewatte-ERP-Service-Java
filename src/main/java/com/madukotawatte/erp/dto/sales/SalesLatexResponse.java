package com.madukotawatte.erp.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesLatexResponse {
    private String saleId;
    private String loadId;
    private BigDecimal mass;
    private BigDecimal litres;
    private BigDecimal metrolacReading;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Boolean isPaymentReceived;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
