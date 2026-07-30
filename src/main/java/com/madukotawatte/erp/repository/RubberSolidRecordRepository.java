package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.RubberSolidRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RubberSolidRecordRepository extends JpaRepository<RubberSolidRecord, String>, JpaSpecificationExecutor<RubberSolidRecord> {
    List<RubberSolidRecord> findByLoad_LoadId(String loadId);

    /**
     * Returns a single-row list [SUM(massKg), COUNT(*), MAX(timestamp)] for a given load.
     * Declared as List rather than Optional&lt;Object[]&gt; — Spring Data JPA double-wraps tuple rows
     * when the return type is Optional&lt;Object[]&gt;, making agg[0] itself another Object[].
     */
    @Query("SELECT COALESCE(SUM(r.massKg), 0), COUNT(r), MAX(r.timestamp) " +
           "FROM RubberSolidRecord r WHERE r.load.loadId = :loadId")
    List<Object[]> aggregateByLoadId(@Param("loadId") String loadId);
}
