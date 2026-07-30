package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.sales.SalesRubberSolidRequest;
import com.madukotawatte.erp.dto.sales.SalesRubberSolidResponse;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.SalesRubberSolid;

import java.util.UUID;

public class SalesRubberSolidMapper {
    private SalesRubberSolidMapper() {}

    public static SalesRubberSolid toEntity(SalesRubberSolidRequest request, Load load) {
        SalesRubberSolid sale = new SalesRubberSolid();
        sale.setSaleId(UUID.randomUUID().toString());
        sale.setLoad(load);
        sale.setSaleDate(request.getSaleDate());
        sale.setMass(request.getMass());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setIsPaid(request.getIsPaid() != null ? request.getIsPaid() : false);
        sale.setStatus(request.getStatus() != null ? request.getStatus() : "pending");
        sale.setPaymentType(request.getPaymentType());
        return sale;
    }

    public static SalesRubberSolidResponse toResponse(SalesRubberSolid sale) {
        return SalesRubberSolidResponse.builder()
                .saleId(sale.getSaleId())
                .loadId(sale.getLoad().getLoadId())
                .saleDate(sale.getSaleDate())
                .mass(sale.getMass())
                .unitPrice(sale.getUnitPrice())
                .totalAmount(sale.getMass().multiply(sale.getUnitPrice()))
                .isPaid(sale.getIsPaid())
                .status(sale.getStatus())
                .paymentType(sale.getPaymentType())
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .build();
    }
}
