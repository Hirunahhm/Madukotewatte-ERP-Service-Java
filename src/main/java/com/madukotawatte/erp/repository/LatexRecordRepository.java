package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.LatexRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LatexRecordRepository extends JpaRepository<LatexRecord, String> {
    List<LatexRecord> findByLoad_LoadId(String loadId);
    List<LatexRecord> findByEmployee_EmployeeId(String employeeId);
    List<LatexRecord> findByEmployee_EmployeeIdAndTimestampBetween(String employeeId, LocalDateTime from, LocalDateTime to);
    List<LatexRecord> findByTimestampBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Returns a single-row list [SUM(latexAmount), SUM(ammoniaAmount), COUNT(*), MAX(timestamp)] for a given load.
     * Declared as List rather than Optional&lt;Object[]&gt; — Spring Data JPA double-wraps tuple rows
     * when the return type is Optional&lt;Object[]&gt;, making agg[0] itself another Object[].
     */
    @Query("SELECT COALESCE(SUM(r.latexAmount), 0), COALESCE(SUM(r.ammoniaAmount), 0), COUNT(r), MAX(r.timestamp) " +
           "FROM LatexRecord r WHERE r.load.loadId = :loadId")
    List<Object[]> aggregateByLoadId(@Param("loadId") String loadId);
}

