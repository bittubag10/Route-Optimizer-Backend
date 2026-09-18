package com.example.DeliveryRouteOptimizer.model;


import com.example.DeliveryRouteOptimizer.enums.TransitType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "routes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_source_dest_transit",
                        columnNames = {"source_hub_id", "destination_hub_id", "transit_type"}
                )
        },
        indexes = {
                @Index(name = "idx_route_source", columnList = "source_hub_id"),
                @Index(name = "idx_route_dest", columnList = "destination_hub_id"),
                @Index(name = "idx_route_active", columnList = "is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_hub_id", nullable = false, foreignKey = @ForeignKey(name = "fk_route_source_hub"))
    private Hub sourceHub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_hub_id", nullable = false, foreignKey = @ForeignKey(name = "fk_route_dest_hub"))
    private Hub destinationHub;

    @Column(name = "base_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseCost;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "transit_type", nullable = false, length = 30)
    private TransitType transitType;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}