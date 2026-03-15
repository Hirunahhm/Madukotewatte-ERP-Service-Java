package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.MetrolacReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetrolacReadingRepository extends JpaRepository<MetrolacReading, String> {
    List<MetrolacReading> findByLoad_LoadId(String loadId);
}
