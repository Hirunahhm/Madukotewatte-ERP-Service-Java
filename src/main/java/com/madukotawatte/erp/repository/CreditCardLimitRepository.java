package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.CreditCardLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCardLimitRepository extends JpaRepository<CreditCardLimit, String> {
}
