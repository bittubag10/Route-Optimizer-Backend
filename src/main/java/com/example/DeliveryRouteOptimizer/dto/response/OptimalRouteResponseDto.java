package com.example.DeliveryRouteOptimizer.dto.response;

import com.example.DeliveryRouteOptimizer.enums.RoutingPreference;
import com.example.DeliveryRouteOptimizer.enums.TrafficLevel;

import java.math.BigDecimal;
import java.util.List;

public record OptimalRouteResponseDto(
        String source,
        String destination,
        RoutingPreference preference,
        BigDecimal totalCost,
        Integer totalDurationMinutes,
        BigDecimal totalDistanceKm,
        int totalStops,
        List<String> hubPath,               // e.g., ["DEL", "BOM", "BLR"]
        List<RouteSegmentResponseDto> segments, // Step-by-step breakdown
        TrafficLevel appliedTrafficLevel,    // Traffic condition used to compute the weights above
        BigDecimal appliedTrafficMultiplier
) {}