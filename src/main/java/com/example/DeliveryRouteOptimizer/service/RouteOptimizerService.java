package com.example.DeliveryRouteOptimizer.service;

import com.example.DeliveryRouteOptimizer.Exceptions.HubNotFoundException;
import com.example.DeliveryRouteOptimizer.Exceptions.RouteNotFoundException;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.DeliveryGraph;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.GraphEdge;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.PathState;
import com.example.DeliveryRouteOptimizer.config.CacheConfig;
import com.example.DeliveryRouteOptimizer.config.RouteMapper;
import com.example.DeliveryRouteOptimizer.dto.response.OptimalRouteResponseDto;
import com.example.DeliveryRouteOptimizer.enums.RoutingPreference;
import com.example.DeliveryRouteOptimizer.traffic.TrafficCondition;
import com.example.DeliveryRouteOptimizer.traffic.TrafficService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class RouteOptimizerService {

    private final DeliveryGraph deliveryGraph;
    private final RouteMapper routeMapper;
    private final TrafficService trafficService;

    /**
     * Main Entry Point: User ki preference ke hisab se algorithm chalta hai.
     * <p>
     * Cached (Feature 4): repeated calls for the same source/destination/preference
     * skip the Dijkstra/BFS traversal entirely and return the previous result.
     * The traffic level is folded into the cache key (via the {@code @trafficService}
     * bean reference below) so that a cached "normal traffic" answer is never served
     * once the clock rolls into a peak window - a fresh entry is computed and cached
     * for the new traffic bucket instead. Cache entries are fully evicted whenever
     * the underlying graph changes (see GraphLoaderService#reloadGraph), since a new
     * hub, route, or disruption can change what "shortest" means.
     */
    @Cacheable(
            cacheNames = CacheConfig.SHORTEST_ROUTE_CACHE,
            key = "#source.trim().toUpperCase() + '_' + #destination.trim().toUpperCase() + '_' + #preference + '_' + @trafficService.getCurrentTrafficCondition().level()"
    )
    public OptimalRouteResponseDto findOptimalRoute(String source, String destination, RoutingPreference preference) {
        String src = source.trim().toUpperCase();
        String dest = destination.trim().toUpperCase();

        // Step 1: Validation
        validateHubs(src, dest);

        // Step 2: Traffic condition ek hi baar nikal lo taaki poore traversal me consistent rahe
        TrafficCondition trafficCondition = trafficService.getCurrentTrafficCondition();

        // Step 3: Decision Engine
        return switch (preference) {
            case CHEAPEST -> calculateCheapestRoute(src, dest, trafficCondition);
            case FASTEST -> calculateFastestRoute(src, dest, trafficCondition);
            case MIN_HOPS -> calculateMinimumHopsRoute(src, dest, trafficCondition);
        };
    }

    /**
     * 1. DIJKSTRA ALGORITHM (CHEAPEST PATH)
     * Min-Heap use karta hai effective (disruption + traffic adjusted) cost ke basis par
     */
    private OptimalRouteResponseDto calculateCheapestRoute(String source, String destination, TrafficCondition traffic) {
        BigDecimal trafficMultiplier = traffic.multiplier();
        PriorityQueue<PathState> pq = new PriorityQueue<>(Comparator.comparing(PathState::totalCost));
        Map<String, BigDecimal> minCostToReach = new HashMap<>();

        // Start point ko queue me daalo
        pq.offer(new PathState(source, BigDecimal.ZERO, 0, BigDecimal.ZERO, List.of(source), Collections.emptyList()));
        minCostToReach.put(source, BigDecimal.ZERO);

        while (!pq.isEmpty()) {
            PathState current = pq.poll();
            String currentHub = current.currentHubCode();

            // Destination mil gaya! (Cheapest guarantee)
            if (currentHub.equals(destination)) {
                return routeMapper.toOptimalRouteResponse(source, destination, RoutingPreference.CHEAPEST, current, traffic);
            }

            // Purani kharab entry ko skip karo
            if (minCostToReach.containsKey(currentHub) && current.totalCost().compareTo(minCostToReach.get(currentHub)) > 0) {
                continue;
            }

            // Outgoing edges explore karo
            for (GraphEdge edge : deliveryGraph.getOutgoingEdges(currentHub)) {
                if (!edge.isActive()) continue;

                // Cycle se bachne ke liye check
                if (current.visitedHubs().contains(edge.getDestinationHubCode())) continue;

                BigDecimal newTotalCost = current.totalCost().add(edge.getTrafficAdjustedCost(trafficMultiplier));
                String nextHub = edge.getDestinationHubCode();

                // Agar pehle se sasta rasta mil gaya, toh update karo aur queue me daalo
                if (!minCostToReach.containsKey(nextHub) || newTotalCost.compareTo(minCostToReach.get(nextHub)) < 0) {
                    minCostToReach.put(nextHub, newTotalCost);

                    List<String> newPath = new ArrayList<>(current.visitedHubs());
                    newPath.add(nextHub);

                    List<GraphEdge> newEdges = new ArrayList<>(current.traversedEdges());
                    newEdges.add(edge);

                    pq.offer(new PathState(
                            nextHub,
                            newTotalCost,
                            current.totalDurationMinutes() + edge.getTrafficAdjustedDurationMinutes(trafficMultiplier),
                            current.totalDistanceKm().add(edge.getDistanceKm()),
                            newPath,
                            newEdges
                    ));
                }
            }
        }

        throw new RouteNotFoundException("No active route found connecting " + source + " to " + destination);
    }

    /**
     * 2. DIJKSTRA ALGORITHM (FASTEST PATH)
     * Min-Heap use karta hai effective (disruption + traffic adjusted) time/duration ke basis par
     */
    private OptimalRouteResponseDto calculateFastestRoute(String source, String destination, TrafficCondition traffic) {
        BigDecimal trafficMultiplier = traffic.multiplier();
        PriorityQueue<PathState> pq = new PriorityQueue<>(Comparator.comparingInt(PathState::totalDurationMinutes));
        Map<String, Integer> minTimeToReach = new HashMap<>();

        pq.offer(new PathState(source, BigDecimal.ZERO, 0, BigDecimal.ZERO, List.of(source), Collections.emptyList()));
        minTimeToReach.put(source, 0);

        while (!pq.isEmpty()) {
            PathState current = pq.poll();
            String currentHub = current.currentHubCode();

            if (currentHub.equals(destination)) {
                return routeMapper.toOptimalRouteResponse(source, destination, RoutingPreference.FASTEST, current, traffic);
            }

            if (minTimeToReach.containsKey(currentHub) && current.totalDurationMinutes() > minTimeToReach.get(currentHub)) {
                continue;
            }

            for (GraphEdge edge : deliveryGraph.getOutgoingEdges(currentHub)) {
                if (!edge.isActive()) continue;
                if (current.visitedHubs().contains(edge.getDestinationHubCode())) continue;

                int newTotalTime = current.totalDurationMinutes() + edge.getTrafficAdjustedDurationMinutes(trafficMultiplier);
                String nextHub = edge.getDestinationHubCode();

                if (!minTimeToReach.containsKey(nextHub) || newTotalTime < minTimeToReach.get(nextHub)) {
                    minTimeToReach.put(nextHub, newTotalTime);

                    List<String> newPath = new ArrayList<>(current.visitedHubs());
                    newPath.add(nextHub);

                    List<GraphEdge> newEdges = new ArrayList<>(current.traversedEdges());
                    newEdges.add(edge);

                    pq.offer(new PathState(
                            nextHub,
                            current.totalCost().add(edge.getTrafficAdjustedCost(trafficMultiplier)),
                            newTotalTime,
                            current.totalDistanceKm().add(edge.getDistanceKm()),
                            newPath,
                            newEdges
                    ));
                }
            }
        }

        throw new RouteNotFoundException("No active route found connecting " + source + " to " + destination);
    }

    /**
     * 3. BFS (MINIMUM HOPS / FEWEST STOPS)
     * Queue use karta hai level-by-level shortest stop path ke liye.
     * Traffic/disruptions don't change which path has fewest hops, but the
     * accumulated cost/duration are still reported using the same effective
     * weights as the other two strategies, so the response stays consistent.
     */
    private OptimalRouteResponseDto calculateMinimumHopsRoute(String source, String destination, TrafficCondition traffic) {
        BigDecimal trafficMultiplier = traffic.multiplier();
        Queue<PathState> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.offer(new PathState(source, BigDecimal.ZERO, 0, BigDecimal.ZERO, List.of(source), Collections.emptyList()));
        visited.add(source);

        while (!queue.isEmpty()) {
            PathState current = queue.poll();
            String currentHub = current.currentHubCode();

            if (currentHub.equals(destination)) {
                return routeMapper.toOptimalRouteResponse(source, destination, RoutingPreference.MIN_HOPS, current, traffic);
            }

            for (GraphEdge edge : deliveryGraph.getOutgoingEdges(currentHub)) {
                if (!edge.isActive()) continue;

                String nextHub = edge.getDestinationHubCode();

                if (!visited.contains(nextHub)) {
                    visited.add(nextHub);

                    List<String> newPath = new ArrayList<>(current.visitedHubs());
                    newPath.add(nextHub);

                    List<GraphEdge> newEdges = new ArrayList<>(current.traversedEdges());
                    newEdges.add(edge);

                    queue.offer(new PathState(
                            nextHub,
                            current.totalCost().add(edge.getTrafficAdjustedCost(trafficMultiplier)),
                            current.totalDurationMinutes() + edge.getTrafficAdjustedDurationMinutes(trafficMultiplier),
                            current.totalDistanceKm().add(edge.getDistanceKm()),
                            newPath,
                            newEdges
                    ));
                }
            }
        }

        throw new RouteNotFoundException("No path found connecting " + source + " to " + destination);
    }

    private void validateHubs(String src, String dest) {
        if (!deliveryGraph.containsHub(src)) {
            throw new HubNotFoundException("Source hub does not exist or is inactive: " + src);
        }
        if (!deliveryGraph.containsHub(dest)) {
            throw new HubNotFoundException("Destination hub does not exist or is inactive: " + dest);
        }
        if (src.equals(dest)) {
            throw new IllegalArgumentException("Source and destination cannot be the same hub.");
        }
    }
}
