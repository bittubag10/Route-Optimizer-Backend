package com.example.DeliveryRouteOptimizer.model;


import com.example.DeliveryRouteOptimizer.enums.DisruptionSeverity;
import com.example.DeliveryRouteOptimizer.enums.DisruptionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "network_disruptions",
        indexes = {
                @Index(name = "idx_disruption_time_window", columnList = "start_time, end_time"),
                @Index(name = "idx_disruption_active", columnList = "is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkDisruption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affected_hub_id", foreignKey = @ForeignKey(name = "fk_disruption_hub"))
    private Hub affectedHub; // Nullable agar disruption kisi specific route par ho

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affected_route_id", foreignKey = @ForeignKey(name = "fk_disruption_route"))
    private Route affectedRoute; // Nullable agar disruption poore hub par ho

    @Enumerated(EnumType.STRING)
    @Column(name = "disruption_type", nullable = false, length = 50)
    private DisruptionType disruptionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private DisruptionSeverity severity;

    @Column(name = "cost_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal costMultiplier; // e.g., 1.25 (+25% surge)

    @Column(name = "delay_minutes", nullable = false)
    private Integer delayMinutes; // e.g., 60 mins delay added dynamically

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}