package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.sales.SalesBananaRequest;
import com.madukotawatte.erp.dto.sales.SalesBananaResponse;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.SalesBanana;

import java.util.UUID;

public class SalesBananaMapper {
    private SalesBananaMapper() {}

    public static SalesBanana toEntity(SalesBananaRequest request, Load load) {
        SalesBanana sale = new SalesBanana();
        sale.setSaleId(UUID.randomUUID().toString());
        sale.setLoad(load);
        sale.setType(request.getType());
        sale.setSaleDate(request.getSaleDate());
        sale.setMass(request.getMass());
        sale.setUnitPrice(request.getUnitPrice());
        // payment is applied by SalesService, which posts the ledger receipt
        sale.setIsPaid(false);
        sale.setStatus("pending");
        sale.setBuyerName(request.getBuyerName());
        sale.setDueDate(request.getDueDate());
        return sale;
    }

    public static SalesBananaResponse toResponse(SalesBanana sale) {
        return SalesBananaResponse.builder()
                .saleId(sale.getSaleId())
                .loadId(sale.getLoad().getLoadId())
                .type(sale.getType())
                .saleDate(sale.getSaleDate())
                .mass(sale.getMass())
                .unitPrice(sale.getUnitPrice())
                .totalAmount(sale.getMass().multiply(sale.getUnitPrice()))
                .isPaid(sale.getIsPaid())
                .status(sale.getStatus())
                .accountCode(sale.getAccountCode())
                .paymentDate(sale.getPaymentDate())
                .transactionId(sale.getTransaction() != null ? sale.getTransaction().getId() : null)
                .buyerName(sale.getBuyerName())
                .dueDate(sale.getDueDate())
                .overdue(SalesMapperSupport.isOverdue(sale.getIsPaid(), sale.getDueDate()))
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .build();
    }
}
