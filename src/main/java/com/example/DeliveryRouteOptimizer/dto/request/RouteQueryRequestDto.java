package com.example.DeliveryRouteOptimizer.dto.request;

import com.example.DeliveryRouteOptimizer.enums.RoutingPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RouteQueryRequestDto(
        @NotBlank(message = "Source code is required")
        String source,

        @NotBlank(message = "Destination code is required")
        String destination,

        @NotNull(message = "Routing preference is required (CHEAPEST, FASTEST, MIN_HOPS)")
        RoutingPreference preference
) {}