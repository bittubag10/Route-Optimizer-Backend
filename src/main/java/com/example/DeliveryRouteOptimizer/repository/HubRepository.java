package com.example.DeliveryRouteOptimizer.repository;

import com.example.DeliveryRouteOptimizer.enums.HubStatus;
import com.example.DeliveryRouteOptimizer.model.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HubRepository extends JpaRepository<Hub,Long> {
    Optional<Hub> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Hub> findAllByStatus(HubStatus status);
}
