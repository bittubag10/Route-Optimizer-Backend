package com.example.DeliveryRouteOptimizer.traffic;

import com.example.DeliveryRouteOptimizer.enums.TrafficLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.function.Supplier;

/**
 * Calculates the current traffic multiplier purely from the time of day.
 * <p>
 * Deliberately kept as the single place that knows about "peak hours" so that
 * no other class in the application hard-codes traffic logic. If a smarter
 * model (live GPS feeds, historical averages, etc.) is introduced later, only
 * this class needs to change - everything downstream just calls
 * {@link #getCurrentTrafficCondition()} and multiplies.
 * <p>
 * Two time windows are supported out of the box:
 * <ul>
 *     <li>Peak hours (e.g. 6 PM - 8 PM) -> {@link TrafficLevel#HIGH}</li>
 *     <li>Moderate/rush hours (e.g. 8 AM - 10 AM) -> {@link TrafficLevel#NORMAL}</li>
 *     <li>Everything else -> {@link TrafficLevel#LOW}</li>
 * </ul>
 * All windows and multipliers are configurable via application.properties.
 */
@Service
public class TrafficService {

    private final LocalTime peakStart;
    private final LocalTime peakEnd;
    private final LocalTime moderateStart;
    private final LocalTime moderateEnd;
    private final BigDecimal peakMultiplier;
    private final BigDecimal moderateMultiplier;
    private final BigDecimal normalMultiplier;

    // Supplies "now"; overridable in tests so traffic assertions never depend on wall-clock time.
    private final Supplier<LocalTime> clock;

    @Autowired
    public TrafficService(
            @Value("${traffic.peak.start:18:00}") String peakStart,
            @Value("${traffic.peak.end:20:00}") String peakEnd,
            @Value("${traffic.moderate.start:08:00}") String moderateStart,
            @Value("${traffic.moderate.end:10:00}") String moderateEnd,
            @Value("${traffic.multiplier.peak:2.0}") BigDecimal peakMultiplier,
            @Value("${traffic.multiplier.moderate:1.5}") BigDecimal moderateMultiplier,
            @Value("${traffic.multiplier.normal:1.0}") BigDecimal normalMultiplier) {
        this(LocalTime.parse(peakStart), LocalTime.parse(peakEnd),
                LocalTime.parse(moderateStart), LocalTime.parse(moderateEnd),
                peakMultiplier, moderateMultiplier, normalMultiplier, LocalTime::now);
    }

    // Package/test-friendly constructor: lets tests pin "now" to a fixed value.
    public TrafficService(LocalTime peakStart, LocalTime peakEnd,
                           LocalTime moderateStart, LocalTime moderateEnd,
                           BigDecimal peakMultiplier, BigDecimal moderateMultiplier, BigDecimal normalMultiplier,
                           Supplier<LocalTime> clock) {
        this.peakStart = peakStart;
        this.peakEnd = peakEnd;
        this.moderateStart = moderateStart;
        this.moderateEnd = moderateEnd;
        this.peakMultiplier = peakMultiplier;
        this.moderateMultiplier = moderateMultiplier;
        this.normalMultiplier = normalMultiplier;
        this.clock = clock;
    }

    /**
     * The traffic condition right now.
     */
    public TrafficCondition getCurrentTrafficCondition() {
        return resolve(clock.get());
    }

    /**
     * The traffic condition at a specific time of day. Exposed separately so
     * the time-window logic can be unit tested without waiting for the clock.
     */
    public TrafficCondition resolve(LocalTime time) {
        if (isWithin(time, peakStart, peakEnd)) {
            return new TrafficCondition(TrafficLevel.HIGH, peakMultiplier);
        }
        if (isWithin(time, moderateStart, moderateEnd)) {
            return new TrafficCondition(TrafficLevel.NORMAL, moderateMultiplier);
        }
        return new TrafficCondition(TrafficLevel.LOW, normalMultiplier);
    }

    /**
     * Inclusive-start/exclusive-end window check that also handles windows
     * which wrap past midnight (e.g. 22:00 - 02:00).
     */
    private boolean isWithin(LocalTime time, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }
}
