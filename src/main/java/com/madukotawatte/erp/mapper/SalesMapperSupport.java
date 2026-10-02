package com.madukotawatte.erp.mapper;

import java.time.LocalDate;

/** Shared helpers for the sales mappers. */
public final class SalesMapperSupport {
    private SalesMapperSupport() {}

    /** A sale is overdue when it is still unpaid and its due date has passed. */
    public static boolean isOverdue(Boolean paid, LocalDate dueDate) {
        return !Boolean.TRUE.equals(paid) && dueDate != null && dueDate.isBefore(LocalDate.now());
    }
}
