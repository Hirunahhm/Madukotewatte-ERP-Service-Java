package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.RubberSolidRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RubberSolidRecordRepository extends JpaRepository<RubberSolidRecord, String> {
    List<RubberSolidRecord> findByLoad_LoadId(String loadId);
}
