package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanBalanceResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionResponse;
import com.madukotawatte.erp.dto.expense.ExpenseRequest;
import com.madukotawatte.erp.dto.expense.ExpenseResponse;
import com.madukotawatte.erp.dto.monetary.AssetBalanceResponse;
import com.madukotawatte.erp.dto.monetary.MonetaryAssetTransactionRequest;
import com.madukotawatte.erp.dto.monetary.MonetaryAssetTransactionResponse;
import com.madukotawatte.erp.dto.sales.SalesLatexRequest;
import com.madukotawatte.erp.dto.sales.SalesLatexResponse;
import com.madukotawatte.erp.dto.sales.SalesLatexUpdateRequest;
import com.madukotawatte.erp.entity.*;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.InsufficientFundsException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.*;
import com.madukotawatte.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final MonetaryAssetTransactionRepository monetaryAssetTransactionRepository;
    private final EstateLoanTransactionRepository estateLoanTransactionRepository;
    private final SalesLatexRepository salesLatexRepository;
    private final LoadRepository loadRepository;
    private final ExpenseRepository expenseRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;
    private final EmployeeRepository employeeRepository;

    // ── Monetary Assets ─────────────────────────────────────────
    public List<AssetBalanceResponse> getAssetBalances() {
        List<Object[]> results = monetaryAssetTransactionRepository.findBalanceByAssetType();
        return results.stream()
                .map(row -> AssetBalanceResponse.builder()
                        .assetType((String) row[0])
                        .balance((BigDecimal) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public MonetaryAssetTransactionResponse createMonetaryTransaction(MonetaryAssetTransactionRequest request) {
        List<MonetaryAssetTransaction> existing = monetaryAssetTransactionRepository.findByAssetType(request.getAssetType());
        BigDecimal lastAmount = BigDecimal.ZERO;
        if (!existing.isEmpty()) {
            lastAmount = existing.get(existing.size() - 1).getNewAmount();
        }

        BigDecimal newAmount;
        if ("money in".equals(request.getTransactionType())) {
            newAmount = lastAmount.add(request.getAmount());
        } else if ("money out".equals(request.getTransactionType())) {
            newAmount = lastAmount.subtract(request.getAmount());
        } else {
            throw new BadRequestException("Invalid transaction type: " + request.getTransactionType());
        }

        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient funds in " + request.getAssetType());
        }

        MonetaryAssetTransaction transaction = new MonetaryAssetTransaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setAssetType(request.getAssetType());
        transaction.setLastAmount(lastAmount);
        transaction.setNewAmount(newAmount);

        return MonetaryAssetMapper.toResponse(monetaryAssetTransactionRepository.save(transaction));
    }

    public PageResponse<MonetaryAssetTransactionResponse> getAllMonetaryTransactions(Pageable pageable) {
        return PageResponse.from(monetaryAssetTransactionRepository.findAll(pageable)
                .map(MonetaryAssetMapper::toResponse));
    }

    public MonetaryAssetTransactionResponse getMonetaryTransaction(String id) {
        MonetaryAssetTransaction transaction = monetaryAssetTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonetaryAssetTransaction", "id", id));
        return MonetaryAssetMapper.toResponse(transaction);
    }

    // ── Estate Loans ────────────────────────────────────────────
    public List<EstateLoanBalanceResponse> getEstateLoanBalances() {
        List<String> loanTypes = List.of("credit-card - Peoples", "credit-card - Sampath", "Loan-mom", "Loan-other");
        return loanTypes.stream()
                .map(loanType -> {
                    List<EstateLoanTransaction> transactions = estateLoanTransactionRepository.findByLoanType(loanType);
                    BigDecimal balance = transactions.isEmpty() ? BigDecimal.ZERO
                            : transactions.get(transactions.size() - 1).getNewAmount();
                    return EstateLoanBalanceResponse.builder()
                            .loanType(loanType)
                            .balance(balance)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public EstateLoanTransactionResponse createEstateLoanTransaction(EstateLoanTransactionRequest request) {
        List<EstateLoanTransaction> existing = estateLoanTransactionRepository.findByLoanType(request.getLoanType());
        BigDecimal lastAmount = BigDecimal.ZERO;
        if (!existing.isEmpty()) {
            lastAmount = existing.get(existing.size() - 1).getNewAmount();
        }
        BigDecimal newAmount = lastAmount.add(request.getAmount());

        EstateLoanTransaction transaction = new EstateLoanTransaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setLoanType(request.getLoanType());
        transaction.setLastAmount(lastAmount);
        transaction.setNewAmount(newAmount);

        return EstateLoanMapper.toResponse(estateLoanTransactionRepository.save(transaction));
    }

    public PageResponse<EstateLoanTransactionResponse> getAllEstateLoanTransactions(Pageable pageable) {
        return PageResponse.from(estateLoanTransactionRepository.findAll(pageable)
                .map(EstateLoanMapper::toResponse));
    }

    public EstateLoanTransactionResponse getEstateLoanTransaction(String id) {
        EstateLoanTransaction transaction = estateLoanTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EstateLoanTransaction", "id", id));
        return EstateLoanMapper.toResponse(transaction);
    }

    // ── Sales Latex ─────────────────────────────────────────────
    @Transactional
    public SalesLatexResponse createSalesLatex(SalesLatexRequest request) {
        Load load = loadRepository.findById(request.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load", "id", request.getLoadId()));
        SalesLatex sale = SalesLatexMapper.toEntity(request, load);
        return SalesLatexMapper.toResponse(salesLatexRepository.save(sale));
    }

    public SalesLatexResponse getSalesLatex(String id) {
        SalesLatex sale = salesLatexRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesLatex", "id", id));
        return SalesLatexMapper.toResponse(sale);
    }

    public PageResponse<SalesLatexResponse> getAllSalesLatex(Pageable pageable) {
        return PageResponse.from(salesLatexRepository.findAll(pageable).map(SalesLatexMapper::toResponse));
    }

    public List<SalesLatexResponse> getUnpaidSales() {
        return salesLatexRepository.findByIsPaymentReceivedFalse().stream()
                .map(SalesLatexMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalesLatexResponse markPaymentReceived(String id, SalesLatexUpdateRequest request) {
        SalesLatex sale = salesLatexRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesLatex", "id", id));
        sale.setIsPaymentReceived(true);
        if (request.getMonetaryTransactionId() != null) {
            MonetaryAssetTransaction transaction = monetaryAssetTransactionRepository
                    .findById(request.getMonetaryTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException("MonetaryAssetTransaction", "id", request.getMonetaryTransactionId()));
            sale.setTransaction(transaction);
        }
        return SalesLatexMapper.toResponse(salesLatexRepository.save(sale));
    }

    // ── Expenses ────────────────────────────────────────────────
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        boolean hasMonetary = request.getMonetaryTransactionId() != null;
        boolean hasLoan = request.getEstateLoanTransactionId() != null;

        if (hasMonetary == hasLoan) {
            throw new BadRequestException("Exactly one of monetaryTransactionId or estateLoanTransactionId must be provided");
        }

        Expense expense = new Expense();
        expense.setExpenseId(UUID.randomUUID().toString());
        expense.setType(request.getType());
        expense.setPaymentType(request.getPaymentType());
        expense.setAmount(request.getAmount());
        expense.setTimestamp(request.getTimestamp());

        if (hasMonetary) {
            MonetaryAssetTransaction monetaryTx = monetaryAssetTransactionRepository
                    .findById(request.getMonetaryTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException("MonetaryAssetTransaction", "id", request.getMonetaryTransactionId()));
            expense.setTransaction(monetaryTx);
        } else {
            EstateLoanTransaction loanTx = estateLoanTransactionRepository
                    .findById(request.getEstateLoanTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException("EstateLoanTransaction", "id", request.getEstateLoanTransactionId()));
            expense.setEstateLoanTransaction(loanTx);
        }

        return ExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    public ExpenseResponse getExpense(String id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        return ExpenseMapper.toResponse(expense);
    }

    public PageResponse<ExpenseResponse> getAllExpenses(Pageable pageable) {
        return PageResponse.from(expenseRepository.findAll(pageable).map(ExpenseMapper::toResponse));
    }

    public PageResponse<ExpenseResponse> getExpensesByDateRange(java.time.LocalDateTime from, java.time.LocalDateTime to, Pageable pageable) {
        return PageResponse.from(expenseRepository.findByTimestampBetween(from, to, pageable).map(ExpenseMapper::toResponse));
    }

    // ── Employee Transactions ───────────────────────────────────
    @Transactional
    public EmployeeTransactionResponse createEmployeeTransaction(EmployeeTransactionRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
        com.madukotawatte.erp.entity.EmployeeTransaction transaction = EmployeeTransactionMapper.toEntity(request, employee);
        return EmployeeTransactionMapper.toResponse(employeeTransactionRepository.save(transaction));
    }

    public EmployeeTransactionResponse getEmployeeTransaction(String id) {
        com.madukotawatte.erp.entity.EmployeeTransaction transaction = employeeTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeTransaction", "id", id));
        return EmployeeTransactionMapper.toResponse(transaction);
    }

    public PageResponse<EmployeeTransactionResponse> getAllEmployeeTransactions(Pageable pageable) {
        return PageResponse.from(employeeTransactionRepository.findAll(pageable)
                .map(EmployeeTransactionMapper::toResponse));
    }
}
