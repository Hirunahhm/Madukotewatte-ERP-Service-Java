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
public class SalesRubberSolidResponse {
    private String saleId;
    private String loadId;
    private LocalDate saleDate;
    private BigDecimal mass;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Boolean isPaid;
    private String status;
    private String paymentType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
