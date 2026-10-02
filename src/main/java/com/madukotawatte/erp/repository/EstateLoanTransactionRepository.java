package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.EstateLoanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstateLoanTransactionRepository extends JpaRepository<EstateLoanTransaction, String>, JpaSpecificationExecutor<EstateLoanTransaction> {
    List<EstateLoanTransaction> findByLoanIdOrderBySeqAsc(String loanId);

    Optional<EstateLoanTransaction> findFirstByLoanIdOrderBySeqDesc(String loanId);

    List<EstateLoanTransaction> findAllByOrderBySeqAsc();
}
