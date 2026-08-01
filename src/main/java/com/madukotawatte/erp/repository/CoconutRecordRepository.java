package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.CoconutRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoconutRecordRepository extends JpaRepository<CoconutRecord, String>, JpaSpecificationExecutor<CoconutRecord> {
    List<CoconutRecord> findByLoad_LoadId(String loadId);
}
