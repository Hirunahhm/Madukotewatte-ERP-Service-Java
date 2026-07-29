package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonetaryAssetTransactionRepository extends JpaRepository<MonetaryAssetTransaction, String>, JpaSpecificationExecutor<MonetaryAssetTransaction> {
    List<MonetaryAssetTransaction> findByAssetTypeOrderByCreatedAtAsc(String assetType);

    @Query("SELECT m.assetType, m.newAmount FROM MonetaryAssetTransaction m WHERE m.createdAt = " +
            "(SELECT MAX(m2.createdAt) FROM MonetaryAssetTransaction m2 WHERE m2.assetType = m.assetType)")
    List<Object[]> findBalanceByAssetType();

    @Query("SELECT m.assetType, m.createdAt, m.newAmount FROM MonetaryAssetTransaction m ORDER BY m.createdAt ASC")
    List<Object[]> findAllOrderedByCreatedAt();
}
