package com.example.DeliveryRouteOptimizer.enums;

/**
 * Represents how congested the network currently is.
 * Used purely as a dynamic, time-derived signal - it is never persisted.
 */
public enum TrafficLevel {
    LOW,
    NORMAL,
    HIGH
}
