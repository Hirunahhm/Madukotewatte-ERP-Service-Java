package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.expense.ExpenseResponse;
import com.madukotawatte.erp.entity.Expense;

public class ExpenseMapper {
    private ExpenseMapper() {}

    public static ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .expenseId(expense.getExpenseId())
                .type(expense.getType())
                .paymentType(expense.getPaymentType())
                .amount(expense.getAmount())
                .timestamp(expense.getTimestamp())
                .monetaryTransactionId(expense.getTransaction() != null ? expense.getTransaction().getId() : null)
                .estateLoanTransactionId(expense.getEstateLoanTransaction() != null ? expense.getEstateLoanTransaction().getId() : null)
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
