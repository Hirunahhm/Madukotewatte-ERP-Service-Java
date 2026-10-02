package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.EstateLoanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstateLoanTransactionRepository extends JpaRepository<EstateLoanTransaction, String>, JpaSpecificationExecutor<EstateLoanTransaction> {
    List<EstateLoanTransaction> findByLoanIdOrderByCreatedAtAsc(String loanId);
}
