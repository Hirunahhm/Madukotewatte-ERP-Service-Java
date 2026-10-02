package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.estateloan.EstateLoanPaymentRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanResponse;
import com.madukotawatte.erp.entity.EstateLoan;
import com.madukotawatte.erp.entity.EstateLoanTransaction;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.exception.InsufficientFundsException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.EstateLoanMapper;
import com.madukotawatte.erp.repository.EstateLoanRepository;
import com.madukotawatte.erp.repository.EstateLoanTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Named estate loans (credit cards are ledger accounts, see {@link LedgerService}). */
@Service
@RequiredArgsConstructor
public class EstateLoanService {

    public static final String REF_ESTATE_LOAN = "ESTATE_LOAN";

    private final EstateLoanRepository estateLoanRepository;
    private final EstateLoanTransactionRepository estateLoanTransactionRepository;
    private final LedgerService ledgerService;

    public List<EstateLoanResponse> getEstateLoans() {
        return estateLoanRepository.findAll().stream()
                .map(EstateLoanMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** Taking a loan optionally books the money into a cash/bank account (ESTATE_LOAN_RECEIPT). */
    @Transactional
    public EstateLoanResponse createEstateLoan(EstateLoanRequest request) {
        EstateLoan loan = EstateLoanMapper.toEntity(request);
        String account = request.getReceivedIntoAccountCode();
        if (account != null && !account.isBlank()) {
            ledgerService.requireCashOrBank(account);
        }
        EstateLoan saved = estateLoanRepository.save(loan);
        if (account != null && !account.isBlank()) {
            ledgerService.post(account, LedgerDirection.IN, request.getPrincipalAmount(), request.getIssuedDate(),
                    LedgerCategory.ESTATE_LOAN_RECEIPT, REF_ESTATE_LOAN, saved.getLoanId(),
                    "Loan received: " + saved.getName());
        }
        return EstateLoanMapper.toResponse(saved);
    }

    /** Principal repayment: posts ESTATE_LOAN_REPAYMENT from the chosen account and reduces the loan balance. */
    @Transactional
    public EstateLoanResponse recordEstateLoanPayment(String loanId, EstateLoanPaymentRequest request) {
        EstateLoan loan = estateLoanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("EstateLoan", "id", loanId));
        ledgerService.requireCashOrBank(request.getAccountCode());
        if (request.getAmount().compareTo(loan.getCurrentBalance()) > 0) {
            throw new InsufficientFundsException("Payment exceeds outstanding balance on loan " + loan.getName());
        }

        ledgerService.post(request.getAccountCode(), LedgerDirection.OUT, request.getAmount(),
                request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now(),
                LedgerCategory.ESTATE_LOAN_REPAYMENT, REF_ESTATE_LOAN, loanId, "Loan repayment: " + loan.getName());

        EstateLoanTransaction tx = new EstateLoanTransaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setLoanId(loanId);
        tx.setTransactionType("repay");
        tx.setLastAmount(loan.getCurrentBalance());
        tx.setNewAmount(loan.getCurrentBalance().subtract(request.getAmount()));
        estateLoanTransactionRepository.save(tx);

        loan.setCurrentBalance(loan.getCurrentBalance().subtract(request.getAmount()));
        if (loan.getCurrentBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setIsActive(false);
        }
        return EstateLoanMapper.toResponse(estateLoanRepository.save(loan));
    }
}
