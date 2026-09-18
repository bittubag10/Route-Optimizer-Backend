package com.example.DeliveryRouteOptimizer.repository;

import com.example.DeliveryRouteOptimizer.model.NetworkDisruption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NetworkDisruptionRepository extends JpaRepository<NetworkDisruption,Long> {
    @Query("""
        SELECT d FROM NetworkDisruption d
        LEFT JOIN FETCH d.affectedHub
        LEFT JOIN FETCH d.affectedRoute
        WHERE d.active = true
          AND :now BETWEEN d.startTime AND d.endTime
    """)
    List<NetworkDisruption> findCurrentlyActiveDisruptions(@Param("now") Instant now);
}
