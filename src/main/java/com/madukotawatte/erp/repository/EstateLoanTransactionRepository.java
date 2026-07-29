package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.EstateLoanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstateLoanTransactionRepository extends JpaRepository<EstateLoanTransaction, String>, JpaSpecificationExecutor<EstateLoanTransaction> {
    List<EstateLoanTransaction> findByLoanTypeOrderByCreatedAtAsc(String loanType);

    @Query("SELECT e.loanType, e.createdAt, e.newAmount FROM EstateLoanTransaction e ORDER BY e.createdAt ASC")
    List<Object[]> findAllOrderedByCreatedAt();

    @Query("SELECT e.loanType, e.createdAt, e.newAmount FROM EstateLoanTransaction e WHERE e.loanType = :loanType ORDER BY e.createdAt ASC")
    List<Object[]> findOrderedByCreatedAtForLoanType(String loanType);
}
