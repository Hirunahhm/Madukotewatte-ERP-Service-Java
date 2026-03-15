package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.EstateLoanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstateLoanTransactionRepository extends JpaRepository<EstateLoanTransaction, String> {
    List<EstateLoanTransaction> findByLoanType(String loanType);
}
