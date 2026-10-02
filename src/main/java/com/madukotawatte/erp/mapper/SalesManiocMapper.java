package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.sales.SalesManiocRequest;
import com.madukotawatte.erp.dto.sales.SalesManiocResponse;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.SalesManioc;

import java.util.UUID;

public class SalesManiocMapper {
    private SalesManiocMapper() {}

    public static SalesManioc toEntity(SalesManiocRequest request, Load load) {
        SalesManioc sale = new SalesManioc();
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

    public static SalesManiocResponse toResponse(SalesManioc sale) {
        return SalesManiocResponse.builder()
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
