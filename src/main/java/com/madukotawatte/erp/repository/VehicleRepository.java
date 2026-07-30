package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    Optional<Vehicle> findByRegistrationNo(String registrationNo);
    boolean existsByRegistrationNo(String registrationNo);
}
