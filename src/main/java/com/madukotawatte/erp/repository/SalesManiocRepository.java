package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.SalesManioc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesManiocRepository extends JpaRepository<SalesManioc, String> {
    List<SalesManioc> findByLoad_LoadId(String loadId);
}
