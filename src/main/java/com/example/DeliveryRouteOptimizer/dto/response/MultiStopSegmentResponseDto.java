package com.example.DeliveryRouteOptimizer.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * One leg of a multi-stop delivery plan, e.g. "A -> C". Each leg is itself the
 * cheapest/fastest/fewest-hops path between the two stops (which may hop
 * through hubs that aren't delivery stops themselves) - {@code hubPath} shows
 * that full underlying path for transparency.
 */
public record MultiStopSegmentResponseDto(
        String fromHubCode,
        String toHubCode,
        BigDecimal legCost,
        Integer legDurationMinutes,
        BigDecimal legDistanceKm,
        List<String> hubPath
) {}
