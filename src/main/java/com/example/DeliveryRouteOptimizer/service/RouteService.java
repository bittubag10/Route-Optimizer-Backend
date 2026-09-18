package com.example.DeliveryRouteOptimizer.service;

import com.example.DeliveryRouteOptimizer.Exceptions.HubNotFoundException;
import com.example.DeliveryRouteOptimizer.Exceptions.RouteNotFoundException;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.GraphLoaderService;
import com.example.DeliveryRouteOptimizer.dto.request.RouteCreateRequestDto;
import com.example.DeliveryRouteOptimizer.dto.request.RouteUpdateRequestDto;
import com.example.DeliveryRouteOptimizer.dto.response.RouteResponseDto;
import com.example.DeliveryRouteOptimizer.model.Hub;
import com.example.DeliveryRouteOptimizer.model.Route;
import com.example.DeliveryRouteOptimizer.repository.HubRepository;
import com.example.DeliveryRouteOptimizer.repository.RouteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final HubRepository hubRepository;
    private final GraphLoaderService graphLoaderService;

    @Transactional
    public RouteResponseDto createRoute(RouteCreateRequestDto request) {
        Hub sourceHub = hubRepository.findByCodeIgnoreCase(request.sourceHubCode())
                .orElseThrow(() -> new HubNotFoundException("Source hub not found: " + request.sourceHubCode()));

        Hub destHub = hubRepository.findByCodeIgnoreCase(request.destinationHubCode())
                .orElseThrow(() -> new HubNotFoundException("Destination hub not found: " + request.destinationHubCode()));

        if (sourceHub.getId().equals(destHub.getId())) {
            throw new IllegalArgumentException("Source and destination hub cannot be the same.");
        }

        if (routeRepository.existsBySourceHubIdAndDestinationHubId(sourceHub.getId(), destHub.getId())) {
            throw new IllegalArgumentException("Direct route already exists between " + request.sourceHubCode() + " and " + request.destinationHubCode());
        }

        Route route = Route.builder()
                .sourceHub(sourceHub)
                .destinationHub(destHub)
                .baseCost(request.baseCost())
                .durationMinutes(request.durationMinutes())
                .distanceKm(request.distanceKm())
                .transitType(request.transitType())
                .active(true)
                .build();

        Route saved = routeRepository.save(route);

        // Memory me graph update karo
        graphLoaderService.reloadGraph();

        return mapToResponse(saved);
    }

    public List<RouteResponseDto> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public RouteResponseDto getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with ID: " + id));
        return mapToResponse(route);
    }

    @Transactional
    public RouteResponseDto updateRoute(Long id, RouteUpdateRequestDto request) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with ID: " + id));

        route.setBaseCost(request.baseCost());
        route.setDurationMinutes(request.durationMinutes());
        route.setDistanceKm(request.distanceKm());
        route.setTransitType(request.transitType());
        route.setActive(request.active());

        Route saved = routeRepository.save(route);

        // Weight/active flag change karo toh in-memory graph turant refresh hona chahiye
        graphLoaderService.reloadGraph();

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with ID: " + id));

        routeRepository.delete(route);
        graphLoaderService.reloadGraph();
    }

    private RouteResponseDto mapToResponse(Route route) {
        return new RouteResponseDto(
                route.getId(),
                route.getSourceHub().getCode(),
                route.getDestinationHub().getCode(),
                route.getBaseCost(),
                route.getDurationMinutes(),
                route.getDistanceKm(),
                route.getTransitType(),
                route.isActive(),
                route.getCreatedAt()
        );
    }
}
