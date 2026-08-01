package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.ManiocRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManiocRecordRepository extends JpaRepository<ManiocRecord, String>, JpaSpecificationExecutor<ManiocRecord> {
    List<ManiocRecord> findByLoad_LoadId(String loadId);
}
