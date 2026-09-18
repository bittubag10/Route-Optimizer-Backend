package com.example.DeliveryRouteOptimizer.service;

import com.example.DeliveryRouteOptimizer.config.CacheConfig;
import com.example.DeliveryRouteOptimizer.dto.response.MultiStopRouteResponseDto;
import com.example.DeliveryRouteOptimizer.dto.response.MultiStopSegmentResponseDto;
import com.example.DeliveryRouteOptimizer.dto.response.OptimalRouteResponseDto;
import com.example.DeliveryRouteOptimizer.enums.RoutingPreference;
import com.example.DeliveryRouteOptimizer.traffic.TrafficCondition;
import com.example.DeliveryRouteOptimizer.traffic.TrafficService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Feature 7 - Multi-Stop Delivery Routing.
 * <p>
 * Given a starting hub and a handful of delivery stops, works out a practical
 * visiting order using a classic TSP-style brute-force: every pairwise
 * shortest path is pre-computed (reusing {@link RouteOptimizerService}, so
 * disruptions, traffic, and Feature 4's caching all apply automatically), and
 * every permutation of the stops is then tried, with pruning, to find the
 * cheapest/fastest/fewest-hop total. This is intentionally exact-but-exponential
 * (O(n!)), which is fine for a small number of stops - it is capped by
 * {@code optimizer.multi-stop.max-stops} for exactly that reason.
 * <p>
 * This is NOT meant to scale to large routing problems. If the number of daily
 * delivery stops grows, {@link #searchRecursive} is the seam to swap for a
 * proper approximation (nearest-neighbour + 2-opt, Held-Karp DP, or a
 * metaheuristic) without touching the pairwise-matrix step or the response
 * mapping at all.
 */
@Slf4j
@Service
public class MultiStopRouteService {

    private final RouteOptimizerService routeOptimizerService;
    private final TrafficService trafficService;
    private final int maxDeliveryStops;

    public MultiStopRouteService(
            RouteOptimizerService routeOptimizerService,
            TrafficService trafficService,
            @Value("${optimizer.multi-stop.max-stops:8}") int maxDeliveryStops) {
        this.routeOptimizerService = routeOptimizerService;
        this.trafficService = trafficService;
        this.maxDeliveryStops = maxDeliveryStops;
    }

    /**
     * Cached (Feature 4, extended to this feature too): identical multi-stop
     * requests skip both the O(n!) permutation search and the pairwise Dijkstra
     * calls. Note the key is built from the delivery-location list as given -
     * requesting the same stops in a different order is treated as a distinct
     * cache entry. That's a deliberate simplicity trade-off, not a correctness
     * issue: it just means the cache dedupes somewhat less aggressively than it could.
     */
    @Cacheable(
            cacheNames = CacheConfig.MULTI_STOP_ROUTE_CACHE,
            key = "#start.trim().toUpperCase() + '_' + #deliveryLocations + '_' + #preference + '_' + @trafficService.getCurrentTrafficCondition().level()"
    )
    public MultiStopRouteResponseDto planMultiStopRoute(String start, List<String> deliveryLocations, RoutingPreference preference) {
        String normalizedStart = start.trim().toUpperCase();
        List<String> stops = normalize(deliveryLocations);

        validate(normalizedStart, stops);

        // Step 1: Pre-compute the shortest path between every pair of nodes we care about.
        List<String> allNodes = new ArrayList<>();
        allNodes.add(normalizedStart);
        allNodes.addAll(stops);
        Map<String, OptimalRouteResponseDto> pairwiseRoutes = computePairwiseRoutes(allNodes, preference);

        // Step 2: Try visiting orders of the delivery stops (with pruning) and keep the best one.
        List<String> bestOrder = new ArrayList<>();
        BigDecimal[] bestCost = {null};
        searchRecursive(normalizedStart, stops, new ArrayList<>(), new boolean[stops.size()], BigDecimal.ZERO,
                pairwiseRoutes, preference, bestOrder, bestCost);

        // Step 3: Build the final response by walking the chosen order and reusing the pre-computed legs.
        return buildResponse(normalizedStart, stops, bestOrder, pairwiseRoutes, preference);
    }

    private List<String> normalize(List<String> deliveryLocations) {
        if (deliveryLocations == null) {
            return List.of();
        }
        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        for (String location : deliveryLocations) {
            if (location != null && !location.isBlank()) {
                deduped.add(location.trim().toUpperCase());
            }
        }
        return new ArrayList<>(deduped);
    }

    private void validate(String start, List<String> stops) {
        if (stops.isEmpty()) {
            throw new IllegalArgumentException("At least one delivery location is required.");
        }
        if (stops.size() > maxDeliveryStops) {
            throw new IllegalArgumentException(
                    "Too many delivery locations (" + stops.size() + "). This endpoint uses a brute-force " +
                            "permutation search intended for small stop counts and supports at most " + maxDeliveryStops +
                            ". Use a scalable heuristic (e.g. nearest-neighbour + 2-opt) for larger deliveries.");
        }
        if (stops.contains(start)) {
            throw new IllegalArgumentException("Start location (" + start + ") cannot also be listed as a delivery stop.");
        }
    }

    /**
     * Computes the shortest path between every ordered pair of nodes involved,
     * memoized in a local map for the duration of this request. Hub existence/
     * inactivity is validated here for free, since
     * {@link RouteOptimizerService#findOptimalRoute} already throws
     * {@code HubNotFoundException}/{@code RouteNotFoundException} for invalid
     * or unreachable hubs.
     */
    private Map<String, OptimalRouteResponseDto> computePairwiseRoutes(List<String> nodes, RoutingPreference preference) {
        Map<String, OptimalRouteResponseDto> routes = new HashMap<>();
        for (String from : nodes) {
            for (String to : nodes) {
                if (from.equals(to)) continue;
                routes.put(legKey(from, to), routeOptimizerService.findOptimalRoute(from, to, preference));
            }
        }
        return routes;
    }

    /**
     * Backtracking permutation search over the delivery stops with branch-and-bound
     * pruning: a partial order already worse than the best complete order found so
     * far is abandoned early. For n stops this explores at most n! orderings -
     * acceptable for the small n this endpoint is designed for (see maxDeliveryStops).
     */
    private void searchRecursive(String currentHub, List<String> stops, List<String> currentOrder, boolean[] used,
                                  BigDecimal runningCost, Map<String, OptimalRouteResponseDto> pairwiseRoutes,
                                  RoutingPreference preference, List<String> bestOrder, BigDecimal[] bestCost) {

        if (currentOrder.size() == stops.size()) {
            if (bestCost[0] == null || runningCost.compareTo(bestCost[0]) < 0) {
                bestCost[0] = runningCost;
                bestOrder.clear();
                bestOrder.addAll(currentOrder);
            }
            return;
        }

        for (int i = 0; i < stops.size(); i++) {
            if (used[i]) continue;

            String nextStop = stops.get(i);
            OptimalRouteResponseDto leg = pairwiseRoutes.get(legKey(currentHub, nextStop));
            BigDecimal newRunningCost = runningCost.add(weightOf(leg, preference));

            // Prune: no point continuing down a branch already worse than our best complete solution so far.
            if (bestCost[0] != null && newRunningCost.compareTo(bestCost[0]) >= 0) {
                continue;
            }

            used[i] = true;
            currentOrder.add(nextStop);

            searchRecursive(nextStop, stops, currentOrder, used, newRunningCost, pairwiseRoutes, preference, bestOrder, bestCost);

            currentOrder.remove(currentOrder.size() - 1);
            used[i] = false;
        }
    }

    private BigDecimal weightOf(OptimalRouteResponseDto leg, RoutingPreference preference) {
        return switch (preference) {
            case CHEAPEST -> leg.totalCost();
            case FASTEST -> BigDecimal.valueOf(leg.totalDurationMinutes());
            case MIN_HOPS -> BigDecimal.valueOf(leg.totalStops());
        };
    }

    private MultiStopRouteResponseDto buildResponse(String start, List<String> requestedStops, List<String> bestOrder,
                                                     Map<String, OptimalRouteResponseDto> pairwiseRoutes,
                                                     RoutingPreference preference) {
        List<MultiStopSegmentResponseDto> segments = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalDuration = 0;
        BigDecimal totalDistance = BigDecimal.ZERO;

        String previousHub = start;
        TrafficCondition traffic = trafficService.getCurrentTrafficCondition();

        for (String stop : bestOrder) {
            OptimalRouteResponseDto leg = pairwiseRoutes.get(legKey(previousHub, stop));

            segments.add(new MultiStopSegmentResponseDto(
                    previousHub,
                    stop,
                    leg.totalCost(),
                    leg.totalDurationMinutes(),
                    leg.totalDistanceKm(),
                    leg.hubPath()
            ));

            totalCost = totalCost.add(leg.totalCost());
            totalDuration += leg.totalDurationMinutes();
            totalDistance = totalDistance.add(leg.totalDistanceKm());

            previousHub = stop;
        }

        return new MultiStopRouteResponseDto(
                start,
                requestedStops,
                bestOrder,
                preference,
                totalCost,
                totalDuration,
                totalDistance,
                segments,
                traffic.level(),
                traffic.multiplier()
        );
    }

    private String legKey(String from, String to) {
        return from + "->" + to;
    }
}
