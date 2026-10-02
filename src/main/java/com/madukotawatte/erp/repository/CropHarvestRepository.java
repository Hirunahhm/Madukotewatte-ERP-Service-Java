package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.CropHarvest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CropHarvestRepository extends JpaRepository<CropHarvest, String>, JpaSpecificationExecutor<CropHarvest> {

    /** Rows: [crop, month (yyyy-MM), unit, total quantity, harvest count]. */
    @Query(value = """
            SELECT crop, to_char(harvest_date, 'YYYY-MM') AS month, unit, SUM(quantity), COUNT(*)
            FROM crop_harvests
            WHERE (CAST(:from AS date) IS NULL OR harvest_date >= CAST(:from AS date))
              AND (CAST(:to AS date) IS NULL OR harvest_date <= CAST(:to AS date))
            GROUP BY crop, to_char(harvest_date, 'YYYY-MM'), unit
            ORDER BY month, crop, unit
            """, nativeQuery = true)
    List<Object[]> summarize(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
