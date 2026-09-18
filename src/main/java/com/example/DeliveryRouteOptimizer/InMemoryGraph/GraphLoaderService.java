package com.example.DeliveryRouteOptimizer.InMemoryGraph;

import com.example.DeliveryRouteOptimizer.enums.HubStatus;
import com.example.DeliveryRouteOptimizer.model.Hub;
import com.example.DeliveryRouteOptimizer.model.NetworkDisruption;
import com.example.DeliveryRouteOptimizer.model.Route;
import com.example.DeliveryRouteOptimizer.config.CacheConfig;
import com.example.DeliveryRouteOptimizer.repository.HubRepository;
import com.example.DeliveryRouteOptimizer.repository.NetworkDisruptionRepository;
import com.example.DeliveryRouteOptimizer.repository.RouteRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphLoaderService {
    private final HubRepository hubRepository;
    private final RouteRepository routeRepository;
    private final NetworkDisruptionRepository disruptionRepository;
    private final DeliveryGraph deliveryGraph;

    @PostConstruct
    public void initializeGraphOnStartup() {
        log.info("Initializing in-memory routing graph from database...");
        reloadGraph();
        log.info("Routing graph successfully loaded into memory.");
    }

    /**
     * Rebuilds the in-memory graph from the database and, in the same breath,
     * evicts every cached shortest-path/multi-stop result (Feature 4's
     * invalidation strategy). This is the single choke point every hub/route/
     * disruption mutation goes through, so it's also the single choke point
     * for cache invalidation - no other service needs to know the cache exists.
     */
    @Transactional(readOnly = true)
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.SHORTEST_ROUTE_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.MULTI_STOP_ROUTE_CACHE, allEntries = true)
    })
    public synchronized void reloadGraph() {
        deliveryGraph.clearGraph();

        // 1. Load active hubs
        List<Hub> activeHubs = hubRepository.findAllByStatus(HubStatus.ACTIVE);
        for (Hub hub : activeHubs) {
            deliveryGraph.addHub(hub.getCode());
        }

        // 2. Load active disruptions map for quick matching
        List<NetworkDisruption> disruptions = disruptionRepository.findCurrentlyActiveDisruptions(Instant.now());
        Map<Long, NetworkDisruption> routeDisruptionMap = new HashMap<>();
        Map<Long, NetworkDisruption> hubDisruptionMap = new HashMap<>();

        for (NetworkDisruption d : disruptions) {
            if (d.getAffectedRoute() != null) {
                routeDisruptionMap.put(d.getAffectedRoute().getId(), d);
            }
            if (d.getAffectedHub() != null) {
                hubDisruptionMap.put(d.getAffectedHub().getId(), d);
            }
        }

        // 3. Load active routes and convert to GraphEdge
        List<Route> routes = routeRepository.findAllActiveRoutesWithHubs();
        for (Route route : routes) {
            // Check if source or destination hub is blocked
            if (route.getSourceHub().getStatus() != HubStatus.ACTIVE ||
                    route.getDestinationHub().getStatus() != HubStatus.ACTIVE) {
                continue;
            }

            BigDecimal costMultiplier = BigDecimal.ONE;
            int delayMinutes = 0;

            // Route-level disruption
            if (routeDisruptionMap.containsKey(route.getId())) {
                NetworkDisruption rd = routeDisruptionMap.get(route.getId());
                costMultiplier = costMultiplier.multiply(rd.getCostMultiplier());
                delayMinutes += rd.getDelayMinutes();
            }

            // Hub-level disruption (source hub congestion)
            if (hubDisruptionMap.containsKey(route.getSourceHub().getId())) {
                NetworkDisruption hd = hubDisruptionMap.get(route.getSourceHub().getId());
                costMultiplier = costMultiplier.multiply(hd.getCostMultiplier());
                delayMinutes += hd.getDelayMinutes();
            }

            GraphEdge edge = GraphEdge.builder()
                    .routeId(route.getId())
                    .sourceHubCode(route.getSourceHub().getCode())
                    .destinationHubCode(route.getDestinationHub().getCode())
                    .baseCost(route.getBaseCost())
                    .durationMinutes(route.getDurationMinutes())
                    .distanceKm(route.getDistanceKm())
                    .transitType(route.getTransitType())
                    .active(route.isActive())
                    .costMultiplier(costMultiplier)
                    .additionalDelayMinutes(delayMinutes)
                    .build();

            deliveryGraph.addEdge(edge);
        }
    }
}
