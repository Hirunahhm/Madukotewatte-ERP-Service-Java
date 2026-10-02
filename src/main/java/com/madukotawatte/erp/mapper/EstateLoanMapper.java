package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.estateloan.EstateLoanMovementResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanResponse;
import com.madukotawatte.erp.entity.EstateLoan;
import com.madukotawatte.erp.entity.EstateLoanTransaction;

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
        loan.setLender(request.getLender());
        loan.setTermMonths(request.getTermMonths());
        loan.setInstallmentAmount(request.getInstallmentAmount());
        loan.setNotes(request.getNotes());
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
                .lender(loan.getLender())
                .termMonths(loan.getTermMonths())
                .installmentAmount(loan.getInstallmentAmount())
                .notes(loan.getNotes())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }

    public static EstateLoanMovementResponse toMovementResponse(EstateLoanTransaction tx, boolean undoable) {
        return EstateLoanMovementResponse.builder()
                .id(tx.getId())
                .loanId(tx.getLoanId())
                .type(tx.getTransactionType())
                .transactionDate(tx.getTransactionDate())
                .amount(tx.getAmount())
                .interestAmount(tx.getInterestAmount())
                .balanceBefore(tx.getLastAmount())
                .balanceAfter(tx.getNewAmount())
                .ledgerTransactionId(tx.getLedgerTransactionId())
                .undoable(undoable)
                .build();
    }
}
