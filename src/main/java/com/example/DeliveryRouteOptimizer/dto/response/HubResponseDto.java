package com.example.DeliveryRouteOptimizer.dto.response;

import com.example.DeliveryRouteOptimizer.enums.HubStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record HubResponseDto(
        Long id,
        String code,
        String name,
        String city,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        HubStatus status,
        Integer handlingCapacityPerDay,
        Instant createdAt
) {}