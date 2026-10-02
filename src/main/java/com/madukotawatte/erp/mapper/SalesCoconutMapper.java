package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.sales.SalesCoconutRequest;
import com.madukotawatte.erp.dto.sales.SalesCoconutResponse;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.SalesCoconut;

import java.util.UUID;

public class SalesCoconutMapper {
    private SalesCoconutMapper() {}

    public static SalesCoconut toEntity(SalesCoconutRequest request, Load load) {
        SalesCoconut sale = new SalesCoconut();
        sale.setSaleId(UUID.randomUUID().toString());
        sale.setLoad(load);
        sale.setType(request.getType());
        sale.setSaleDate(request.getSaleDate());
        sale.setMass(request.getMass());
        sale.setUnitPrice(request.getUnitPrice());
        // payment is applied by SalesService, which posts the ledger receipt
        sale.setIsPaid(false);
        sale.setStatus("pending");
        return sale;
    }

    public static SalesCoconutResponse toResponse(SalesCoconut sale) {
        return SalesCoconutResponse.builder()
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
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .build();
    }
}
