package com.example.DeliveryRouteOptimizer.enums;

public enum DisruptionSeverity {
    LOW,      // Minor delay
    MODERATE, // Noticeable delay + minor surge
    HIGH,     // Severe delay + high cost multiplier
    CRITICAL  // Route/Hub completely blocked
}
