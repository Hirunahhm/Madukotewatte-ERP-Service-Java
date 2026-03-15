package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonetaryAssetTransactionRepository extends JpaRepository<MonetaryAssetTransaction, String> {
    List<MonetaryAssetTransaction> findByAssetType(String assetType);

    @Query("SELECT m.assetType, SUM(CASE WHEN m.transactionType = 'money in' THEN m.newAmount ELSE 0 END) - SUM(CASE WHEN m.transactionType = 'money out' THEN m.newAmount ELSE 0 END) FROM MonetaryAssetTransaction m GROUP BY m.assetType")
    List<Object[]> findBalanceByAssetType();
}
