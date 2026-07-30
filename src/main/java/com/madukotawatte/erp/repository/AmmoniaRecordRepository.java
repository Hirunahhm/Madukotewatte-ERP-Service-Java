package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.AmmoniaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmmoniaRecordRepository extends JpaRepository<AmmoniaRecord, String>, JpaSpecificationExecutor<AmmoniaRecord> {
    List<AmmoniaRecord> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    Optional<AmmoniaRecord> findTopByOrderByTimestampDesc();
    Optional<AmmoniaRecord> findTopByTimestampLessThanEqualOrderByTimestampDesc(LocalDateTime cutoff);
}
