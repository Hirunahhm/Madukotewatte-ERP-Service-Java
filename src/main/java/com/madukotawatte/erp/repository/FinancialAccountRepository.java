package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.FinancialAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, String> {

    /** SELECT ... FOR UPDATE: serialises concurrent postings against one account. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM FinancialAccount a WHERE a.code = :code")
    Optional<FinancialAccount> findByCodeForUpdate(String code);
}
