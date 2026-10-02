package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonetaryAssetTransactionRepository extends JpaRepository<MonetaryAssetTransaction, String>, JpaSpecificationExecutor<MonetaryAssetTransaction> {

    /** Latest row of an account (highest insertion sequence) — carries the current running balance. */
    Optional<MonetaryAssetTransaction> findFirstByAccountCodeOrderBySeqDesc(String accountCode);

    Optional<MonetaryAssetTransaction> findByReversalOfId(String reversalOfId);

    List<MonetaryAssetTransaction> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    List<MonetaryAssetTransaction> findByAccountCodeAndCategoryOrderBySeqAsc(String accountCode, com.madukotawatte.erp.entity.LedgerCategory category);

    @Query("SELECT m.reversalOfId FROM MonetaryAssetTransaction m WHERE m.reversalOfId IN :ids")
    List<String> findReversedIds(Collection<String> ids);

    /** [accountCode, currentBalance] for every account that has ledger rows. */
    @Query(value = "SELECT DISTINCT ON (account_code) account_code, new_amount FROM monetary_asset_transactions ORDER BY account_code, seq DESC",
            nativeQuery = true)
    List<Object[]> findCurrentBalances();

    /** [accountCode, signedSum] up to and including the date. */
    @Query(value = "SELECT account_code, COALESCE(SUM(CASE WHEN transaction_type = 'money in' THEN amount ELSE -amount END), 0) " +
            "FROM monetary_asset_transactions WHERE transaction_date <= :asOf GROUP BY account_code", nativeQuery = true)
    List<Object[]> findBalancesAsOf(LocalDate asOf);

    /** Signed sum of an account strictly before a date (statement opening balance). */
    @Query(value = "SELECT COALESCE(SUM(CASE WHEN transaction_type = 'money in' THEN amount ELSE -amount END), 0) " +
            "FROM monetary_asset_transactions WHERE account_code = :accountCode AND transaction_date < :before", nativeQuery = true)
    java.math.BigDecimal sumBefore(String accountCode, LocalDate before);

    /** [transactionDate, accountType, signedSum] up to a date, ascending — used by the balance trend. */
    @Query(value = "SELECT m.transaction_date, a.type, COALESCE(SUM(CASE WHEN m.transaction_type = 'money in' THEN m.amount ELSE -m.amount END), 0) " +
            "FROM monetary_asset_transactions m JOIN financial_accounts a ON a.code = m.account_code " +
            "WHERE m.transaction_date <= :until GROUP BY m.transaction_date, a.type ORDER BY m.transaction_date", nativeQuery = true)
    List<Object[]> findDailyTotalsByType(LocalDate until);
}
