package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.SalesRubberSolid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesRubberSolidRepository extends JpaRepository<SalesRubberSolid, String> {
    List<SalesRubberSolid> findByLoad_LoadId(String loadId);
}
