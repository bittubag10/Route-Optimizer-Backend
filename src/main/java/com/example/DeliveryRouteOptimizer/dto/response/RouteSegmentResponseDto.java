package com.example.DeliveryRouteOptimizer.dto.response;

import com.example.DeliveryRouteOptimizer.enums.TransitType;

import java.math.BigDecimal;

public record RouteSegmentResponseDto(
        String fromHubCode,
        String toHubCode,
        BigDecimal segmentCost,
        Integer segmentDurationMinutes,
        BigDecimal segmentDistanceKm,
        TransitType transitType
) {}