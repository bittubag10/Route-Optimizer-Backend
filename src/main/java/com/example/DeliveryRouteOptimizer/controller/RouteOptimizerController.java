package com.example.DeliveryRouteOptimizer.controller;

import com.example.DeliveryRouteOptimizer.dto.response.MultiStopRouteResponseDto;
import com.example.DeliveryRouteOptimizer.dto.response.OptimalRouteResponseDto;
import com.example.DeliveryRouteOptimizer.enums.RoutingPreference;
import com.example.DeliveryRouteOptimizer.service.MultiStopRouteService;
import com.example.DeliveryRouteOptimizer.service.RouteOptimizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/optimizer")
@RequiredArgsConstructor
public class RouteOptimizerController {
    private final RouteOptimizerService routeOptimizerService;
    private final MultiStopRouteService multiStopRouteService;

    /**
     * Feature 3 - single source/destination shortest path.
     * Example: GET /api/v1/optimizer/route?source=DEL&destination=BLR&preference=CHEAPEST
     */
    @GetMapping("/route")
    public ResponseEntity<OptimalRouteResponseDto> findOptimalRoute(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam(defaultValue = "CHEAPEST") RoutingPreference preference) {

        OptimalRouteResponseDto response = routeOptimizerService.findOptimalRoute(source, destination, preference);
        return ResponseEntity.ok(response);
    }

    /**
     * Feature 7 - multi-stop delivery routing (small-scale TSP).
     * Example: GET /api/v1/optimizer/multi-stop?start=DEL&stops=BOM,BLR,HYD&preference=CHEAPEST
     */
    @GetMapping("/multi-stop")
    public ResponseEntity<MultiStopRouteResponseDto> planMultiStopRoute(
            @RequestParam String start,
            @RequestParam List<String> stops,
            @RequestParam(defaultValue = "CHEAPEST") RoutingPreference preference) {

        MultiStopRouteResponseDto response = multiStopRouteService.planMultiStopRoute(start, stops, preference);
        return ResponseEntity.ok(response);
    }
}
