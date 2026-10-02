package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.sales.SalesLatexRequest;
import com.madukotawatte.erp.dto.sales.SalesLatexResponse;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.SalesLatex;

import java.util.UUID;

public class SalesLatexMapper {
    private SalesLatexMapper() {}

    public static SalesLatex toEntity(SalesLatexRequest request, Load load) {
        SalesLatex sale = new SalesLatex();
        sale.setSaleId(UUID.randomUUID().toString());
        sale.setLoad(load);
        sale.setMass(request.getMass());
        sale.setLitres(request.getLitres());
        sale.setMetrolacReading(request.getMetrolacReading());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setTotalAmount(request.getMass().multiply(request.getUnitPrice()));
        sale.setIsPaymentReceived(false);
        sale.setBuyerName(request.getBuyerName());
        sale.setDueDate(request.getDueDate());
        return sale;
    }

    public static SalesLatexResponse toResponse(SalesLatex sale) {
        return SalesLatexResponse.builder()
                .saleId(sale.getSaleId())
                .loadId(sale.getLoad().getLoadId())
                .mass(sale.getMass())
                .litres(sale.getLitres())
                .metrolacReading(sale.getMetrolacReading())
                .unitPrice(sale.getUnitPrice())
                .totalAmount(sale.getTotalAmount())
                .isPaymentReceived(sale.getIsPaymentReceived())
                .transactionId(sale.getTransaction() != null ? sale.getTransaction().getId() : null)
                .accountCode(sale.getAccountCode())
                .paymentDate(sale.getPaymentDate())
                .buyerName(sale.getBuyerName())
                .dueDate(sale.getDueDate())
                .overdue(SalesMapperSupport.isOverdue(sale.getIsPaymentReceived(), sale.getDueDate()))
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .build();
    }
}
