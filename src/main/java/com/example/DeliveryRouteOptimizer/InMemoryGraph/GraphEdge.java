package com.example.DeliveryRouteOptimizer.InMemoryGraph;

import com.example.DeliveryRouteOptimizer.enums.TransitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class GraphEdge {
    private Long routeId;
    private String sourceHubCode;
    private String destinationHubCode;
    private BigDecimal baseCost;
    private Integer durationMinutes;
    private BigDecimal distanceKm;
    private TransitType transitType;
    private boolean active;

    // Dynamic Multipliers (Disruptions ke through lagte hain)
    private BigDecimal costMultiplier;
    private Integer additionalDelayMinutes;

    // Effective values after baking in disruption multipliers/delays only.
    public BigDecimal getEffectiveCost() {
        if (costMultiplier == null) {
            return baseCost;
        }
        return baseCost.multiply(costMultiplier);
    }

    public int getEffectiveDurationMinutes() {
        return durationMinutes + (additionalDelayMinutes != null ? additionalDelayMinutes : 0);
    }

    /**
     * Final weight used by the routing algorithms: the disruption-adjusted
     * cost further scaled by the traffic multiplier that is in effect right
     * now (see {@code com.example.DeliveryRouteOptimizer.traffic.TrafficService}).
     * This is the "effectiveWeight = distance/cost x trafficMultiplier"
     * formula from the spec, applied on top of any existing disruption surge.
     */
    public BigDecimal getTrafficAdjustedCost(BigDecimal trafficMultiplier) {
        return getEffectiveCost().multiply(trafficMultiplier);
    }

    /**
     * Duration scaled by the same traffic multiplier, rounded to the nearest
     * whole minute so downstream reporting stays in plain int minutes.
     */
    public int getTrafficAdjustedDurationMinutes(BigDecimal trafficMultiplier) {
        return BigDecimal.valueOf(getEffectiveDurationMinutes())
                .multiply(trafficMultiplier)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}
