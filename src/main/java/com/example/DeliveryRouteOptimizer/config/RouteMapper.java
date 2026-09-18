package com.example.DeliveryRouteOptimizer.config;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.GraphEdge;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.PathState;
import com.example.DeliveryRouteOptimizer.dto.response.OptimalRouteResponseDto;
import com.example.DeliveryRouteOptimizer.dto.response.RouteSegmentResponseDto;
import com.example.DeliveryRouteOptimizer.enums.RoutingPreference;
import com.example.DeliveryRouteOptimizer.traffic.TrafficCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RouteMapper {

    /**
     * @param trafficCondition the traffic condition that was in effect while the path
     *                         was computed - reported back so callers know which weights
     *                         (peak/normal/low) produced this result.
     */
    public OptimalRouteResponseDto toOptimalRouteResponse(
            String source,
            String destination,
            RoutingPreference preference,
            PathState finalState,
            TrafficCondition trafficCondition) {

        // 1. Har ek road/flight connection ko response format me badlo
        List<RouteSegmentResponseDto> segments = finalState.traversedEdges().stream()
                .map(edge -> toRouteSegmentResponse(edge, trafficCondition.multiplier()))
                .toList();

        // 2. Total stops calculate karo: Agar path [DEL, BOM, BLR] hai toh 3 nodes - 2 = 1 stop
        int totalStops = Math.max(0, finalState.visitedHubs().size() - 2);

        // 3. Final response DTO bana kar return karo
        return new OptimalRouteResponseDto(
                source,
                destination,
                preference,
                finalState.totalCost(),
                finalState.totalDurationMinutes(),
                finalState.totalDistanceKm(),
                totalStops,
                finalState.visitedHubs(),
                segments,
                trafficCondition.level(),
                trafficCondition.multiplier()
        );
    }

    private RouteSegmentResponseDto toRouteSegmentResponse(GraphEdge edge, BigDecimal trafficMultiplier) {
        return new RouteSegmentResponseDto(
                edge.getSourceHubCode(),
                edge.getDestinationHubCode(),
                edge.getTrafficAdjustedCost(trafficMultiplier),
                edge.getTrafficAdjustedDurationMinutes(trafficMultiplier),
                edge.getDistanceKm(),
                edge.getTransitType()
        );
    }
}
