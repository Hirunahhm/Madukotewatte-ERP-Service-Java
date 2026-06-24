package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.SalesCoconut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesCoconutRepository extends JpaRepository<SalesCoconut, String> {
    List<SalesCoconut> findByLoad_LoadId(String loadId);
}
