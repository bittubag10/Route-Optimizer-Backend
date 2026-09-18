package com.example.DeliveryRouteOptimizer.dto.response;

import com.example.DeliveryRouteOptimizer.enums.RoutingPreference;
import com.example.DeliveryRouteOptimizer.enums.TrafficLevel;

import java.math.BigDecimal;
import java.util.List;

public record MultiStopRouteResponseDto(
        String start,
        List<String> requestedDeliveryLocations,
        List<String> deliveryOrder,          // recommended visiting order, e.g. ["C", "B", "D"]
        RoutingPreference preference,
        BigDecimal totalCost,
        Integer totalDurationMinutes,
        BigDecimal totalDistanceKm,
        List<MultiStopSegmentResponseDto> segments,
        TrafficLevel appliedTrafficLevel,
        BigDecimal appliedTrafficMultiplier
) {}
