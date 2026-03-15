package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {
    Page<Expense> findByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<Expense> findByType(String type, Pageable pageable);
}
