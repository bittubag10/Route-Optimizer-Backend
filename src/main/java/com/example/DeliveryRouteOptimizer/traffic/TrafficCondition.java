package com.example.DeliveryRouteOptimizer.traffic;

import com.example.DeliveryRouteOptimizer.enums.TrafficLevel;

import java.math.BigDecimal;

/**
 * The traffic level currently in effect, together with the multiplier that
 * should be applied on top of a route's base cost/duration.
 */
public record TrafficCondition(TrafficLevel level, BigDecimal multiplier) {
}
