package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.SalesLatex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesLatexRepository extends JpaRepository<SalesLatex, String> {
    List<SalesLatex> findByIsPaymentReceivedFalse();
    List<SalesLatex> findByLoad_LoadId(String loadId);
}
