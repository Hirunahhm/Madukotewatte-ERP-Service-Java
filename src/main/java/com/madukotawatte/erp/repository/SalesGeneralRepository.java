package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.SalesGeneral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesGeneralRepository extends JpaRepository<SalesGeneral, String>, JpaSpecificationExecutor<SalesGeneral> {
}
