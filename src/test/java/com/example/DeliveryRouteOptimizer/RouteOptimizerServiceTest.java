package com.example.DeliveryRouteOptimizer;


import com.example.DeliveryRouteOptimizer.Exceptions.HubNotFoundException;
import com.example.DeliveryRouteOptimizer.Exceptions.RouteNotFoundException;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.DeliveryGraph;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.GraphEdge;
import com.example.DeliveryRouteOptimizer.config.RouteMapper;
import com.example.DeliveryRouteOptimizer.dto.response.OptimalRouteResponseDto;
import com.example.DeliveryRouteOptimizer.enums.RoutingPreference;
import com.example.DeliveryRouteOptimizer.enums.TrafficLevel;
import com.example.DeliveryRouteOptimizer.enums.TransitType;
import com.example.DeliveryRouteOptimizer.service.RouteOptimizerService;
import com.example.DeliveryRouteOptimizer.traffic.TrafficService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteOptimizerServiceTest {

    private DeliveryGraph deliveryGraph;
    private RouteOptimizerService routeOptimizerService;

    @BeforeEach
    void setUp() {
        deliveryGraph = new DeliveryGraph();
        RouteMapper routeMapper = new RouteMapper();
        // Fixed clock at 3 AM -> outside both the peak (18:00-20:00) and moderate (08:00-10:00)
        // windows, so traffic multiplier is 1.0 (LOW) and existing cost/time assertions below
        // stay exact regardless of what time this test actually runs at.
        TrafficService offPeakTrafficService = trafficServiceAt(LocalTime.of(3, 0));
        routeOptimizerService = new RouteOptimizerService(deliveryGraph, routeMapper, offPeakTrafficService);

        // Setup Test Graph Topology:
        // A -> B (Cost: 50, Time: 100)
        // A -> C (Cost: 20, Time: 60)
        // C -> B (Cost: 10, Time: 60) -> Total Path A-C-B (Cost: 30, Time: 120)

        deliveryGraph.addEdge(createEdge(1L, "A", "B", new BigDecimal("50.00"), 100, new BigDecimal("500")));
        deliveryGraph.addEdge(createEdge(2L, "A", "C", new BigDecimal("20.00"), 60, new BigDecimal("200")));
        deliveryGraph.addEdge(createEdge(3L, "C", "B", new BigDecimal("10.00"), 60, new BigDecimal("200")));
        deliveryGraph.addHub("DISCONNECTED_NODE");
    }

    /** Builds a TrafficService whose "now" is pinned to a fixed time, for deterministic tests. */
    private TrafficService trafficServiceAt(LocalTime fixedTime) {
        return new TrafficService(
                LocalTime.of(18, 0), LocalTime.of(20, 0),   // peak window
                LocalTime.of(8, 0), LocalTime.of(10, 0),    // moderate window
                new BigDecimal("2.0"), new BigDecimal("1.5"), BigDecimal.ONE,
                () -> fixedTime
        );
    }

    @Test
    @DisplayName("Should find cheapest path (A -> C -> B) using Dijkstra")
    void testCheapestPath() {
        OptimalRouteResponseDto response = routeOptimizerService.findOptimalRoute("A", "B", RoutingPreference.CHEAPEST);

        assertEquals(new BigDecimal("30.00"), response.totalCost());
        assertEquals(List.of("A", "C", "B"), response.hubPath());
        assertEquals(1, response.totalStops());
    }

    @Test
    @DisplayName("Should find fastest path (A -> B) directly despite higher cost")
    void testFastestPath() {
        OptimalRouteResponseDto response = routeOptimizerService.findOptimalRoute("A", "B", RoutingPreference.FASTEST);

        assertEquals(100, response.totalDurationMinutes());
        assertEquals(List.of("A", "B"), response.hubPath());
        assertEquals(0, response.totalStops());
    }

    @Test
    @DisplayName("Should find minimum hops path (A -> B) using BFS")
    void testMinimumHopsPath() {
        OptimalRouteResponseDto response = routeOptimizerService.findOptimalRoute("A", "B", RoutingPreference.MIN_HOPS);

        assertEquals(List.of("A", "B"), response.hubPath());
        assertEquals(0, response.totalStops());
    }

    @Test
    @DisplayName("Should throw HubNotFoundException for non-existent hub")
    void testInvalidHub() {
        assertThrows(HubNotFoundException.class, () ->
                routeOptimizerService.findOptimalRoute("XYZ", "B", RoutingPreference.CHEAPEST)
        );
    }

    @Test
    @DisplayName("Should throw RouteNotFoundException when no path exists to destination")
    void testDisconnectedGraph() {
        assertThrows(RouteNotFoundException.class, () ->
                routeOptimizerService.findOptimalRoute("A", "DISCONNECTED_NODE", RoutingPreference.CHEAPEST)
        );
    }

    @Test
    @DisplayName("Should scale cost/time by the peak-hour traffic multiplier (Feature 5 & 6)")
    void testPeakTrafficIncreasesEffectiveWeight() {
        RouteOptimizerService peakHourService = new RouteOptimizerService(
                deliveryGraph, new RouteMapper(), trafficServiceAt(LocalTime.of(19, 0)) // inside 18:00-20:00 peak window
        );

        OptimalRouteResponseDto response = peakHourService.findOptimalRoute("A", "B", RoutingPreference.CHEAPEST);

        // Off-peak cheapest path A-C-B costs 30.00 (see testCheapestPath); at 2.0x peak multiplier it should double.
        assertEquals(new BigDecimal("60.00"), response.totalCost());
        assertEquals(TrafficLevel.HIGH, response.appliedTrafficLevel());
        assertEquals(new BigDecimal("2.0"), response.appliedTrafficMultiplier());
    }

    private GraphEdge createEdge(Long id, String src, String dest, BigDecimal cost, int duration, BigDecimal distance) {
        return GraphEdge.builder()
                .routeId(id)
                .sourceHubCode(src)
                .destinationHubCode(dest)
                .baseCost(cost)
                .durationMinutes(duration)
                .distanceKm(distance)
                .transitType(TransitType.AIR_FREIGHT)
                .active(true)
                .costMultiplier(BigDecimal.ONE)
                .additionalDelayMinutes(0)
                .build();
    }
}