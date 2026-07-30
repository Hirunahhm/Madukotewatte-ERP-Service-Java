package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.SalesBanana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesBananaRepository extends JpaRepository<SalesBanana, String> {
    List<SalesBanana> findByLoad_LoadId(String loadId);
}
