package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.EstateLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstateLoanRepository extends JpaRepository<EstateLoan, String> {
}
