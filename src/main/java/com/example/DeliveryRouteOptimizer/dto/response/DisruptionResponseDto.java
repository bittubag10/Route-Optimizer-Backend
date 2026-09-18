package com.example.DeliveryRouteOptimizer.dto.response;

import com.example.DeliveryRouteOptimizer.enums.DisruptionSeverity;
import com.example.DeliveryRouteOptimizer.enums.DisruptionType;

import java.math.BigDecimal;
import java.time.Instant;

public record DisruptionResponseDto(
        Long id,
        String targetDetails,
        DisruptionType disruptionType,
        DisruptionSeverity severity,
        BigDecimal costMultiplier,
        Integer delayMinutes,
        Instant startTime,
        Instant endTime,
        String reason,
        boolean active
) {}