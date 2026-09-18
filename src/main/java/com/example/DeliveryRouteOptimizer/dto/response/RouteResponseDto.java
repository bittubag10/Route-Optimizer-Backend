package com.example.DeliveryRouteOptimizer.dto.response;

import com.example.DeliveryRouteOptimizer.enums.TransitType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Full representation of a route/edge, used by the Route management CRUD
 * endpoints. Distinct from {@link RouteSegmentResponseDto}, which is a
 * lightweight, id-less view used purely inside a computed path's segment list.
 */
public record RouteResponseDto(
        Long id,
        String sourceHubCode,
        String destinationHubCode,
        BigDecimal baseCost,
        Integer durationMinutes,
        BigDecimal distanceKm,
        TransitType transitType,
        boolean active,
        Instant createdAt
) {}
