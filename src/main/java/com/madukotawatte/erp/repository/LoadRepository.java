package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.Load;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoadRepository extends JpaRepository<Load, String> {
    Page<Load> findByStartDateBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}
