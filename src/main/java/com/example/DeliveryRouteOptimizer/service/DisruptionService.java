package com.example.DeliveryRouteOptimizer.service;

import com.example.DeliveryRouteOptimizer.Exceptions.HubNotFoundException;
import com.example.DeliveryRouteOptimizer.Exceptions.RouteNotFoundException;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.GraphLoaderService;
import com.example.DeliveryRouteOptimizer.dto.request.DisruptionCreateRequestDto;
import com.example.DeliveryRouteOptimizer.dto.response.DisruptionResponseDto;
import com.example.DeliveryRouteOptimizer.model.Hub;
import com.example.DeliveryRouteOptimizer.model.NetworkDisruption;
import com.example.DeliveryRouteOptimizer.model.Route;
import com.example.DeliveryRouteOptimizer.repository.HubRepository;
import com.example.DeliveryRouteOptimizer.repository.NetworkDisruptionRepository;
import com.example.DeliveryRouteOptimizer.repository.RouteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DisruptionService {
    private final NetworkDisruptionRepository disruptionRepository;
    private final HubRepository hubRepository;
    private final RouteRepository routeRepository;
    private final GraphLoaderService graphLoaderService;

    @Transactional
    public DisruptionResponseDto createDisruption(DisruptionCreateRequestDto request) {
        Hub affectedHub = null;
        if (request.affectedHubId() != null) {
            affectedHub = hubRepository.findById(request.affectedHubId())
                    .orElseThrow(() -> new HubNotFoundException("Hub not found with ID: " + request.affectedHubId()));
        }

        Route affectedRoute = null;
        if (request.affectedRouteId() != null) {
            affectedRoute = routeRepository.findById(request.affectedRouteId())
                    .orElseThrow(() -> new RouteNotFoundException("Route not found with ID: " + request.affectedRouteId()));
        }

        if (affectedHub == null && affectedRoute == null) {
            throw new IllegalArgumentException("Either affectedHubId or affectedRouteId must be provided.");
        }

        NetworkDisruption disruption = NetworkDisruption.builder()
                .affectedHub(affectedHub)
                .affectedRoute(affectedRoute)
                .disruptionType(request.disruptionType())
                .severity(request.severity())
                .costMultiplier(request.costMultiplier())
                .delayMinutes(request.delayMinutes())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .reason(request.reason())
                .active(true)
                .build();

        NetworkDisruption saved = disruptionRepository.save(disruption);

        // Live memory update
        graphLoaderService.reloadGraph();

        String target = affectedHub != null
                ? "Hub: " + affectedHub.getCode()
                : "Route: " + affectedRoute.getSourceHub().getCode() + "->" + affectedRoute.getDestinationHub().getCode();

        return new DisruptionResponseDto(
                saved.getId(),
                target,
                saved.getDisruptionType(),
                saved.getSeverity(),
                saved.getCostMultiplier(),
                saved.getDelayMinutes(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getReason(),
                saved.isActive()
        );
    }
}

