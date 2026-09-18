package com.example.DeliveryRouteOptimizer.config;

import com.example.DeliveryRouteOptimizer.InMemoryGraph.GraphLoaderService;
import com.example.DeliveryRouteOptimizer.enums.HubStatus;
import com.example.DeliveryRouteOptimizer.enums.TransitType;
import com.example.DeliveryRouteOptimizer.model.Hub;
import com.example.DeliveryRouteOptimizer.model.Route;
import com.example.DeliveryRouteOptimizer.repository.HubRepository;
import com.example.DeliveryRouteOptimizer.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final HubRepository hubRepository;
    private final RouteRepository routeRepository;
    private final GraphLoaderService graphLoaderService;

    @Override
    public void run(String... args) {
        if (hubRepository.count() > 0) {
            log.info("Data already seeded. Skipping initial seeding.");
            return;
        }

        log.info("Seeding initial Hubs and Routes for testing...");

        // 1. Create Hubs
        Hub del = createHub("DEL", "Indira Gandhi Int Airport", "New Delhi", "India", new BigDecimal("28.5562"), new BigDecimal("77.1000"));
        Hub bom = createHub("BOM", "Chhatrapati Shivaji Int Airport", "Mumbai", "India", new BigDecimal("19.0896"), new BigDecimal("72.8656"));
        Hub blr = createHub("BLR", "Kempegowda Int Airport", "Bengaluru", "India", new BigDecimal("13.1986"), new BigDecimal("77.7066"));
        Hub ccu = createHub("CCU", "Netaji Subhash Chandra Bose Int Airport", "Kolkata", "India", new BigDecimal("22.6547"), new BigDecimal("88.4467"));
        Hub hyd = createHub("HYD", "Rajiv Gandhi Int Airport", "Hyderabad", "India", new BigDecimal("17.2403"), new BigDecimal("78.4294"));

        hubRepository.saveAll(List.of(del, bom, blr, ccu, hyd));

        // 2. Create Interconnected Routes
        // DEL to BOM: Fast direct, higher cost
        createRoute(del, bom, new BigDecimal("4500.00"), 130, new BigDecimal("1150.00"), TransitType.AIR_FREIGHT);

        // DEL to CCU: Direct connection
        createRoute(del, ccu, new BigDecimal("4000.00"), 140, new BigDecimal("1300.00"), TransitType.AIR_FREIGHT);

        // DEL to HYD: Cheaper transit
        createRoute(del, hyd, new BigDecimal("3200.00"), 135, new BigDecimal("1250.00"), TransitType.AIR_FREIGHT);

        // BOM to BLR: Direct flight
        createRoute(bom, blr, new BigDecimal("2800.00"), 100, new BigDecimal("840.00"), TransitType.AIR_FREIGHT);

        // HYD to BLR: Very cheap short route
        createRoute(hyd, blr, new BigDecimal("1800.00"), 75, new BigDecimal("500.00"), TransitType.AIR_FREIGHT);

        // CCU to HYD: Connecting route
        createRoute(ccu, hyd, new BigDecimal("3000.00"), 125, new BigDecimal("1180.00"), TransitType.AIR_FREIGHT);

        // Direct DEL to BLR: High cost express (fastest single-hop)
        createRoute(del, blr, new BigDecimal("7500.00"), 165, new BigDecimal("1740.00"), TransitType.AIR_FREIGHT);

        // Reload memory graph
        graphLoaderService.reloadGraph();
        log.info("Initial network seeded and loaded into memory successfully.");
    }

    private Hub createHub(String code, String name, String city, String country, BigDecimal lat, BigDecimal lon) {
        return Hub.builder()
                .code(code)
                .name(name)
                .city(city)
                .country(country)
                .latitude(lat)
                .longitude(lon)
                .status(HubStatus.ACTIVE)
                .handlingCapacityPerDay(10000)
                .build();
    }

    private void createRoute(Hub source, Hub dest, BigDecimal cost, int duration, BigDecimal distance, TransitType type) {
        Route route = Route.builder()
                .sourceHub(source)
                .destinationHub(dest)
                .baseCost(cost)
                .durationMinutes(duration)
                .distanceKm(distance)
                .transitType(type)
                .active(true)
                .build();
        routeRepository.save(route);
    }
}
