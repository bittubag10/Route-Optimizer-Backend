package com.example.DeliveryRouteOptimizer.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(
        name = "route_schedules",
        indexes = {
                @Index(name = "idx_schedule_route", columnList = "route_id"),
                @Index(name = "idx_schedule_dept_time", columnList = "departure_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false, foreignKey = @ForeignKey(name = "fk_schedule_route"))
    private Route route;

    @Column(name = "flight_or_service_code", nullable = false, length = 30)
    private String flightOrServiceCode; // e.g., "6E-2034", "EK-501"

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "max_weight_capacity_kg", precision = 10, scale = 2)
    private BigDecimal maxWeightCapacityKg;

    @Column(name = "is_operational_today", nullable = false)
    private boolean operationalToday;
}