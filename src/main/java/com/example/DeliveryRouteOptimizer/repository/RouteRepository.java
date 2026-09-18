package com.example.DeliveryRouteOptimizer.repository;

import com.example.DeliveryRouteOptimizer.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route,Long> {
    // Graph initialize karne ke liye saare active routes fetch karna
    @Query("SELECT r FROM Route r JOIN FETCH r.sourceHub JOIN FETCH r.destinationHub WHERE r.active = true")
    List<Route> findAllActiveRoutesWithHubs();

    List<Route> findBySourceHubCodeIgnoreCase(String sourceCode);

    boolean existsBySourceHubIdAndDestinationHubId(Long sourceHubId, Long destinationHubId);

    // Used before deleting a hub, so we never leave a route pointing at a hub that no longer exists.
    boolean existsBySourceHubIdOrDestinationHubId(Long sourceHubId, Long destinationHubId);
}
