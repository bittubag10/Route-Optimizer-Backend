package com.example.DeliveryRouteOptimizer.InMemoryGraph;

import java.math.BigDecimal;
import java.util.List;

public record PathState(
        String currentHubCode,
        BigDecimal totalCost,
        int totalDurationMinutes,
        BigDecimal totalDistanceKm,
        List<String> visitedHubs,
        List<GraphEdge> traversedEdges
) {}