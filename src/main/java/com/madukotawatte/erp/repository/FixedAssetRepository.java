package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, String>, JpaSpecificationExecutor<FixedAsset> {
}
