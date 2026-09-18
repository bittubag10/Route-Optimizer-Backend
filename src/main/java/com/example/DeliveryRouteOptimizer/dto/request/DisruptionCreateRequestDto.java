package com.example.DeliveryRouteOptimizer.dto.request;

import com.example.DeliveryRouteOptimizer.enums.DisruptionSeverity;
import com.example.DeliveryRouteOptimizer.enums.DisruptionType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;


public record DisruptionCreateRequestDto(
        Long affectedHubId,

        Long affectedRouteId,

        @NotNull(message = "Disruption type is required")
        DisruptionType disruptionType,

        @NotNull(message = "Severity is required")
        DisruptionSeverity severity,

        @NotNull(message = "Cost multiplier is required")
        @DecimalMin(value = "1.0", message = "Multiplier cannot be less than 1.0 (no discount)")
        @DecimalMax(value = "5.0", message = "Multiplier cannot exceed 5.0")
        BigDecimal costMultiplier,

        @NotNull(message = "Delay minutes is required")
        @Min(value = 0, message = "Delay minutes cannot be negative")
        Integer delayMinutes,

        @NotNull(message = "Start time is required")
        Instant startTime,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        Instant endTime,

        @Size(max = 255, message = "Reason cannot exceed 255 characters")
        String reason
) {}