package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.expense.ExpenseResponse;
import com.madukotawatte.erp.entity.Expense;

public class ExpenseMapper {

    private ExpenseMapper() {}

    public static ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .expenseId(expense.getExpenseId())
                .type(expense.getType())
                .accountCode(expense.getAccountCode())
                .payee(expense.getPayee())
                .description(expense.getDescription())
                .dueDate(expense.getDueDate())
                .overdue(SalesMapperSupport.isOverdue(expense.getIsPaid(), expense.getDueDate()))
                .systemManaged(expense.getReferenceType() != null)
                .amount(expense.getAmount())
                .timestamp(expense.getTimestamp())
                .transactionId(expense.getTransaction() != null ? expense.getTransaction().getId() : null)
                .paymentDate(expense.getTransaction() != null ? expense.getTransaction().getTransactionDate() : null)
                .referenceType(expense.getReferenceType())
                .referenceId(expense.getReferenceId())
                .isPaid(expense.getIsPaid())
                .status(expense.getStatus())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
