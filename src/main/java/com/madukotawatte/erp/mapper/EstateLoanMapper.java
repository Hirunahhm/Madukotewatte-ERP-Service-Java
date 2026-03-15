package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionResponse;
import com.madukotawatte.erp.entity.EstateLoanTransaction;

public class EstateLoanMapper {
    private EstateLoanMapper() {}

    public static EstateLoanTransactionResponse toResponse(EstateLoanTransaction transaction) {
        return EstateLoanTransactionResponse.builder()
                .id(transaction.getId())
                .loanType(transaction.getLoanType())
                .lastAmount(transaction.getLastAmount())
                .newAmount(transaction.getNewAmount())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
