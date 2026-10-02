package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.labour.LabourRequest;
import com.madukotawatte.erp.dto.labour.LabourResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeTransaction;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.Labour;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.LabourMapper;
import com.madukotawatte.erp.repository.EmployeeRepository;
import com.madukotawatte.erp.repository.EmployeeTransactionRepository;
import com.madukotawatte.erp.repository.LabourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Day labour. Paying on the spot posts a LABOUR_PAYMENT ledger row plus a 'Labour' expense (reference LABOUR);
 * editing the money or deleting the record reverses the ledger row and removes the expense.
 */
@Service
@RequiredArgsConstructor
public class LabourService {

    private final LabourRepository labourRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;
    private final ExpenseService expenseService;
    private final LedgerService ledgerService;

    @Transactional
    public LabourResponse createLabour(LabourRequest request) {
        Employee employee = findEmployeeById(request.getEmployeeId());
        resolveAmount(request, employee);
        boolean paid = Boolean.TRUE.equals(request.getIsPaid());
        if (paid && isBlank(request.getAccountCode())) {
            throw new BadRequestException("accountCode is required when isPaid=true");
        }

        Labour labour = LabourMapper.toEntity(request, employee, null);
        labour.setAccountCode(paid ? request.getAccountCode() : null);
        if (paid) {
            pay(labour, employee);
        }
        return LabourMapper.toResponse(labourRepository.save(labour));
    }

    public LabourResponse getLabour(String id) {
        return LabourMapper.toResponse(findLabourById(id));
    }

    public PageResponse<LabourResponse> getAllLabour(Pageable pageable) {
        return PageResponse.from(labourRepository.findAll(pageable).map(LabourMapper::toResponse));
    }

    public List<LabourResponse> getLabourByDateRange(LocalDateTime from, LocalDateTime to) {
        return labourRepository.findByTimestampBetween(from, to).stream()
                .map(LabourMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LabourResponse updateLabour(String id, LabourRequest request) {
        Labour labour = findLabourById(id);
        Employee employee = findEmployeeById(request.getEmployeeId());
        assertNotOnPayslip(labour, "edited");
        resolveAmount(request, employee);

        boolean wasPaid = labour.getEmployeeTransaction() != null;
        boolean wantPaid = Boolean.TRUE.equals(request.getIsPaid());
        String newAccount = isBlank(request.getAccountCode()) ? labour.getAccountCode() : request.getAccountCode();
        LocalDateTime newTimestamp = request.getTimestamp() != null ? request.getTimestamp() : labour.getTimestamp();

        if (wantPaid && isBlank(newAccount)) {
            throw new BadRequestException("accountCode is required when isPaid=true");
        }
        boolean moneyUnchanged = wasPaid && wantPaid
                && request.getAmount().compareTo(labour.getAmount()) == 0
                && newAccount.equals(labour.getAccountCode())
                && newTimestamp.toLocalDate().equals(labour.getTimestamp().toLocalDate())
                && employee.getEmployeeId().equals(labour.getEmployee().getEmployeeId());

        if (wasPaid && !moneyUnchanged) {
            unpay(labour, "Labour edited");
        }

        labour.setEmployee(employee);
        labour.setIsPaid(wantPaid);
        labour.setWorkedHours(request.getWorkedHours());
        labour.setHourlyRate(request.getHourlyRate());
        labour.setAmount(request.getAmount());
        labour.setWorkType(request.getWorkType());
        labour.setDescription(request.getDescription());
        labour.setTimestamp(newTimestamp);
        if (wantPaid && !moneyUnchanged) {
            labour.setAccountCode(newAccount);
            pay(labour, employee);
        } else if (!wantPaid) {
            labour.setAccountCode(null);
        }
        return LabourMapper.toResponse(labourRepository.save(labour));
    }

    @Transactional
    public void deleteLabour(String id) {
        Labour labour = findLabourById(id);
        assertNotOnPayslip(labour, "deleted");
        if (labour.getEmployeeTransaction() != null) {
            unpay(labour, "Labour deleted");
        }
        labourRepository.delete(labour);
    }

    /**
     * Fills in whatever the caller left out: amount = hours x rate, where the rate is the request's hourlyRate or
     * else the employee's configured hourly rate; with only an amount given the effective rate is derived.
     */
    private void resolveAmount(LabourRequest request, Employee employee) {
        BigDecimal hours = request.getWorkedHours();
        if (request.getAmount() == null) {
            BigDecimal rate = request.getHourlyRate() != null ? request.getHourlyRate() : employee.getHourlyRate();
            if (rate == null || rate.signum() <= 0) {
                throw new BadRequestException("No hourly rate: send hourlyRate/amount or set the employee's hourly rate");
            }
            request.setHourlyRate(rate);
            request.setAmount(hours.multiply(rate).setScale(2, RoundingMode.HALF_UP));
        } else if (request.getHourlyRate() == null) {
            request.setHourlyRate(request.getAmount().divide(hours, 2, RoundingMode.HALF_UP));
        }
    }

    private void assertNotOnPayslip(Labour labour, String verb) {
        if (labour.getPayslip() != null) {
            throw new BadRequestException("This labour was paid on a payslip and cannot be " + verb
                    + " - void the payslip first");
        }
    }

    /** Creates the Manual_Labor employee transaction, the ledger payment and the 'Labour' expense. */
    private void pay(Labour labour, Employee employee) {
        ledgerService.requireCashOrBank(labour.getAccountCode());
        EmployeeTransaction transaction = employeeTransactionRepository.save(EmployeeTransaction.builder()
                .transactionRecordId(UUID.randomUUID().toString())
                .employee(employee)
                .type("Manual_Labor")
                .accountCode(labour.getAccountCode())
                .amount(labour.getAmount())
                .timestamp(labour.getTimestamp())
                .build());
        expenseService.recordPaidSystemExpense("Labour", labour.getAmount(), labour.getTimestamp(),
                labour.getAccountCode(), LedgerCategory.LABOUR_PAYMENT, ExpenseService.REF_LABOUR,
                labour.getLabourId(), "Day labour: " + employee.getName() + " (" + labour.getWorkType() + ")");
        labour.setEmployeeTransaction(transaction);
    }

    /** Reverses the ledger row, removes the system expense and the Manual_Labor employee transaction. */
    private void unpay(Labour labour, String reason) {
        expenseService.removeSystemExpenses(ExpenseService.REF_LABOUR, labour.getLabourId(), reason);
        EmployeeTransaction transaction = labour.getEmployeeTransaction();
        labour.setEmployeeTransaction(null);
        labour.setAccountCode(null);
        labourRepository.saveAndFlush(labour);
        if (transaction != null) {
            employeeTransactionRepository.delete(transaction);
        }
    }

    private Labour findLabourById(String id) {
        return labourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Labour", "id", id));
    }

    private Employee findEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
