package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.estateloan.EstateLoanRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanResponse;
import com.madukotawatte.erp.entity.EstateLoan;

import java.util.UUID;

public class EstateLoanMapper {
    private EstateLoanMapper() {}

    public static EstateLoan toEntity(EstateLoanRequest request) {
        EstateLoan loan = new EstateLoan();
        loan.setLoanId(UUID.randomUUID().toString());
        loan.setName(request.getName());
        loan.setPrincipalAmount(request.getPrincipalAmount());
        loan.setCurrentBalance(request.getPrincipalAmount());
        loan.setIssuedDate(request.getIssuedDate());
        loan.setInterestRate(request.getInterestRate());
        return loan;
    }

    public static EstateLoanResponse toResponse(EstateLoan loan) {
        return EstateLoanResponse.builder()
                .loanId(loan.getLoanId())
                .name(loan.getName())
                .principalAmount(loan.getPrincipalAmount())
                .currentBalance(loan.getCurrentBalance())
                .issuedDate(loan.getIssuedDate())
                .interestRate(loan.getInterestRate())
                .isActive(loan.getIsActive())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }
}
