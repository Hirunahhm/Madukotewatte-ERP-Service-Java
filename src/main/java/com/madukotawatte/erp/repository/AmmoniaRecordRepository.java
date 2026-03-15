package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.AmmoniaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmmoniaRecordRepository extends JpaRepository<AmmoniaRecord, String> {}
