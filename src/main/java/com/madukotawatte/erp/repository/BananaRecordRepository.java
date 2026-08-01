package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.BananaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BananaRecordRepository extends JpaRepository<BananaRecord, String>, JpaSpecificationExecutor<BananaRecord> {
    List<BananaRecord> findByLoad_LoadId(String loadId);
}
