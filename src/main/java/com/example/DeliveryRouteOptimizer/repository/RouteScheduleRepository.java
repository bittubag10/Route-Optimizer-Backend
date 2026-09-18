package com.example.DeliveryRouteOptimizer.repository;

import com.example.DeliveryRouteOptimizer.model.RouteSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteScheduleRepository extends JpaRepository<RouteSchedule, Long> {

    List<RouteSchedule> findByRouteIdAndOperationalTodayTrue(Long routeId);

    @Query("SELECT s FROM RouteSchedule s WHERE LOWER(s.flightOrServiceCode) = LOWER(:serviceCode)")
    List<RouteSchedule> findByFlightOrServiceCodeIgnoreCase(@Param("serviceCode") String serviceCode);
}