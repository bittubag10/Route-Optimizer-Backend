package com.example.DeliveryRouteOptimizer.dto.request;

import com.example.DeliveryRouteOptimizer.enums.TransitType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Fields that can be changed on an existing route. Source/destination hubs are
 * intentionally excluded - re-pointing a route to different hubs is really a
 * different edge, so create a new route and delete the old one for that case.
 */
public record RouteUpdateRequestDto(
        @NotNull(message = "Base cost is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Base cost must be positive")
        BigDecimal baseCost,

        @NotNull(message = "Duration in minutes is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        @NotNull(message = "Distance is required")
        @DecimalMin(value = "0.1", inclusive = true, message = "Distance must be positive")
        BigDecimal distanceKm,

        @NotNull(message = "Transit type is required")
        TransitType transitType,

        @NotNull(message = "Active flag is required")
        Boolean active
) {}
