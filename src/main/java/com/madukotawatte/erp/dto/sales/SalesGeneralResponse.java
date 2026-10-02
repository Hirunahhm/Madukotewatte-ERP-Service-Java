package com.madukotawatte.erp.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesGeneralResponse {
    private String saleId;
    private String product;
    private String description;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String buyerName;
    private LocalDate saleDate;
    private LocalDate dueDate;
    private Boolean isPaid;
    private String status;
    private String accountCode;
    private LocalDate paymentDate;
    private String transactionId;
    private Boolean overdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
