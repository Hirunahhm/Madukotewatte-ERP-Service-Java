package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeLoanDeductionRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeSalaryPaymentRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeLoan;
import com.madukotawatte.erp.entity.EmployeeTransaction;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.InsufficientFundsException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.EmployeeTransactionMapper;
import com.madukotawatte.erp.repository.EmployeeLoanRepository;
import com.madukotawatte.erp.repository.EmployeeRepository;
import com.madukotawatte.erp.repository.EmployeeTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Employee money events: generic employee transactions, salary payments and loan deductions. */
@Service
@RequiredArgsConstructor
public class EmployeeFinanceService {

    public static final String REF_EMPLOYEE_TRANSACTION = "EMPLOYEE_TRANSACTION";

    private final EmployeeTransactionRepository employeeTransactionRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final LedgerService ledgerService;

    @Transactional
    public EmployeeTransactionResponse createEmployeeTransaction(EmployeeTransactionRequest request) {
        Employee employee = findEmployee(request.getEmployeeId());
        if (request.getAccountCode() != null && !request.getAccountCode().isBlank()) {
            ledgerService.requireActiveAccount(request.getAccountCode());
        }
        EmployeeTransaction transaction = EmployeeTransactionMapper.toEntity(request, employee);
        return EmployeeTransactionMapper.toResponse(employeeTransactionRepository.save(transaction));
    }

    /**
     * A salary payment settles through the account ledger (SALARY_PAYMENT) so a paid salary always reduces a
     * real cash/bank balance.
     */
    @Transactional
    public EmployeeTransactionResponse recordSalaryPayment(EmployeeSalaryPaymentRequest request) {
        Employee employee = findEmployee(request.getEmployeeId());
        ledgerService.requireCashOrBank(request.getAccountCode());

        String id = UUID.randomUUID().toString();
        ledgerService.post(request.getAccountCode(), LedgerDirection.OUT, request.getAmount(),
                request.getTimestamp().toLocalDate(), LedgerCategory.SALARY_PAYMENT,
                REF_EMPLOYEE_TRANSACTION, id, "Salary: " + employee.getName());

        EmployeeTransaction transaction = EmployeeTransaction.builder()
                .transactionRecordId(id)
                .employee(employee)
                .type("Salary_Payment")
                .accountCode(request.getAccountCode())
                .amount(request.getAmount())
                .timestamp(request.getTimestamp())
                .build();
        return EmployeeTransactionMapper.toResponse(employeeTransactionRepository.save(transaction));
    }

    /**
     * Deducts an amount from the employee's single active loan (business rule: at most one active loan per
     * employee, enforced in WorkforceService.createLoan) and logs it as a Loan_Payment transaction. This is
     * bookkeeping only; no ledger entry, since the deduction reduces what would otherwise be paid out.
     */
    @Transactional
    public EmployeeTransactionResponse applyLoanDeduction(EmployeeLoanDeductionRequest request) {
        Employee employee = findEmployee(request.getEmployeeId());
        List<EmployeeLoan> active = employeeLoanRepository.findByEmployee_EmployeeIdAndIsActiveTrue(request.getEmployeeId());
        if (active.isEmpty()) {
            throw new BadRequestException("Employee has no active loan");
        }
        EmployeeLoan loan = active.get(0);
        if (request.getAmount().compareTo(loan.getCurrentBalance()) > 0) {
            throw new InsufficientFundsException("Deduction exceeds the loan outstanding balance");
        }
        loan.setCurrentBalance(loan.getCurrentBalance().subtract(request.getAmount()));
        if (loan.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setIsActive(false);
        }
        employeeLoanRepository.save(loan);

        EmployeeTransaction transaction = EmployeeTransaction.builder()
                .transactionRecordId(UUID.randomUUID().toString())
                .employee(employee)
                .type("Loan_Payment")
                .accountCode(null)
                .amount(request.getAmount())
                .timestamp(request.getTimestamp())
                .build();
        return EmployeeTransactionMapper.toResponse(employeeTransactionRepository.save(transaction));
    }

    public EmployeeTransactionResponse getEmployeeTransaction(String id) {
        return EmployeeTransactionMapper.toResponse(employeeTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeTransaction", "id", id)));
    }

    public PageResponse<EmployeeTransactionResponse> getAllEmployeeTransactions(Pageable pageable) {
        return PageResponse.from(employeeTransactionRepository.findAll(pageable)
                .map(EmployeeTransactionMapper::toResponse));
    }

    private Employee findEmployee(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }
}
