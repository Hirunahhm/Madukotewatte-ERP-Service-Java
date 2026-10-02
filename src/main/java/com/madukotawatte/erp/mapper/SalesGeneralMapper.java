package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.sales.SalesGeneralRequest;
import com.madukotawatte.erp.dto.sales.SalesGeneralResponse;
import com.madukotawatte.erp.entity.SalesGeneral;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class SalesGeneralMapper {
    private SalesGeneralMapper() {}

    public static SalesGeneral toEntity(SalesGeneralRequest request) {
        SalesGeneral sale = new SalesGeneral();
        sale.setSaleId(UUID.randomUUID().toString());
        // payment is applied by SalesService, which posts the ledger receipt
        sale.setIsPaid(false);
        sale.setStatus("pending");
        applyFields(sale, request);
        return sale;
    }

    /** Copies the editable (non-payment) fields and recomputes the total. */
    public static void applyFields(SalesGeneral sale, SalesGeneralRequest request) {
        sale.setProduct(request.getProduct());
        sale.setDescription(request.getDescription());
        sale.setQuantity(request.getQuantity());
        sale.setUnit(request.getUnit());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setTotalAmount(total(request.getQuantity(), request.getUnitPrice()));
        sale.setBuyerName(request.getBuyerName() == null || request.getBuyerName().isBlank() ? null : request.getBuyerName().trim());
        sale.setSaleDate(request.getSaleDate());
        sale.setDueDate(request.getDueDate());
    }

    public static BigDecimal total(BigDecimal quantity, BigDecimal unitPrice) {
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public static SalesGeneralResponse toResponse(SalesGeneral sale) {
        return SalesGeneralResponse.builder()
                .saleId(sale.getSaleId())
                .product(sale.getProduct())
                .description(sale.getDescription())
                .quantity(sale.getQuantity())
                .unit(sale.getUnit())
                .unitPrice(sale.getUnitPrice())
                .totalAmount(sale.getTotalAmount())
                .buyerName(sale.getBuyerName())
                .saleDate(sale.getSaleDate())
                .dueDate(sale.getDueDate())
                .isPaid(sale.getIsPaid())
                .status(sale.getStatus())
                .accountCode(sale.getAccountCode())
                .paymentDate(sale.getPaymentDate())
                .transactionId(sale.getTransaction() != null ? sale.getTransaction().getId() : null)
                .overdue(SalesMapperSupport.isOverdue(sale.getIsPaid(), sale.getDueDate()))
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .build();
    }
}
