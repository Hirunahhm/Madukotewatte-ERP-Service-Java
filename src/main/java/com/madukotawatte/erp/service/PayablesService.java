package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.finance.PayableRowResponse;
import com.madukotawatte.erp.dto.finance.PayablesResponse;
import com.madukotawatte.erp.entity.Expense;
import com.madukotawatte.erp.repository.ExpenseRepository;
import com.madukotawatte.erp.service.support.AgingSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/** Trade payables: expenses incurred but not yet paid on a given day, aged from the expense date. */
@Service
@RequiredArgsConstructor
public class PayablesService {

    private final ExpenseRepository expenseRepository;

    /**
     * An expense counts when its date is on/before asOf and (it is unpaid, or it was paid after asOf).
     * Overdue means the due date is before asOf.
     */
    @Transactional(readOnly = true)
    public PayablesResponse getPayables(LocalDate asOf) {
        LocalDate date = asOf != null ? asOf : LocalDate.now();
        List<PayableRowResponse> rows = expenseRepository.findByTimestampBefore(date.plusDays(1).atStartOfDay()).stream()
                .filter(e -> isOutstanding(e, date))
                .sorted(Comparator.comparing(Expense::getTimestamp))
                .map(e -> {
                    LocalDate expenseDate = e.getTimestamp().toLocalDate();
                    long days = Math.max(0, ChronoUnit.DAYS.between(expenseDate, date));
                    return PayableRowResponse.builder()
                            .expenseId(e.getExpenseId())
                            .category(e.getType())
                            .payee(e.getPayee())
                            .description(e.getDescription())
                            .expenseDate(expenseDate)
                            .dueDate(e.getDueDate())
                            .amount(e.getAmount())
                            .daysOutstanding(days)
                            .bucket(AgingSupport.bucketFor(days))
                            .overdue(e.getDueDate() != null && e.getDueDate().isBefore(date))
                            .build();
                })
                .toList();
        BigDecimal total = rows.stream().map(PayableRowResponse::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overdue = rows.stream().filter(PayableRowResponse::isOverdue)
                .map(PayableRowResponse::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return PayablesResponse.builder()
                .asOf(date)
                .total(total)
                .overdueTotal(overdue)
                .rows(rows)
                .buckets(AgingSupport.buckets(rows, PayableRowResponse::getBucket, PayableRowResponse::getAmount))
                .byPayee(AgingSupport.byParty(rows, PayableRowResponse::getPayee, PayableRowResponse::getAmount,
                        PayableRowResponse::isOverdue, PayableRowResponse::getDaysOutstanding))
                .build();
    }

    private boolean isOutstanding(Expense expense, LocalDate asOf) {
        if (!Boolean.TRUE.equals(expense.getIsPaid())) {
            return true;
        }
        return expense.getTransaction() != null && expense.getTransaction().getTransactionDate().isAfter(asOf);
    }
}
