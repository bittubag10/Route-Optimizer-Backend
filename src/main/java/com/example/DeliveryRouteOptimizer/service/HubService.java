package com.example.DeliveryRouteOptimizer.service;

import com.example.DeliveryRouteOptimizer.Exceptions.HubNotFoundException;
import com.example.DeliveryRouteOptimizer.InMemoryGraph.GraphLoaderService;
import com.example.DeliveryRouteOptimizer.dto.request.HubCreateRequestDto;
import com.example.DeliveryRouteOptimizer.dto.request.HubUpdateRequestDto;
import com.example.DeliveryRouteOptimizer.dto.response.HubResponseDto;
import com.example.DeliveryRouteOptimizer.model.Hub;
import com.example.DeliveryRouteOptimizer.repository.HubRepository;
import com.example.DeliveryRouteOptimizer.repository.RouteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HubService {
    private final HubRepository hubRepository;
    private final RouteRepository routeRepository;
    private final GraphLoaderService graphLoaderService;

    @Transactional
    public HubResponseDto createHub(HubCreateRequestDto request) {
        if (hubRepository.existsByCodeIgnoreCase(request.code())) {
            throw new IllegalArgumentException("Hub with code " + request.code() + " already exists.");
        }

        Hub hub = Hub.builder()
                .code(request.code().toUpperCase())
                .name(request.name())
                .city(request.city())
                .country(request.country())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .status(request.status())
                .handlingCapacityPerDay(request.handlingCapacityPerDay())
                .build();

        Hub saved = hubRepository.save(hub);

        // Database me add hone ke baad in-memory graph refresh karo
        graphLoaderService.reloadGraph();

        return mapToResponse(saved);
    }

    public List<HubResponseDto> getAllHubs() {
        return hubRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public HubResponseDto getHubByCode(String code) {
        Hub hub = hubRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new HubNotFoundException("Hub not found with code: " + code));
        return mapToResponse(hub);
    }

    @Transactional
    public HubResponseDto updateHub(String code, HubUpdateRequestDto request) {
        Hub hub = hubRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new HubNotFoundException("Hub not found with code: " + code));

        hub.setName(request.name());
        hub.setCity(request.city());
        hub.setCountry(request.country());
        hub.setLatitude(request.latitude());
        hub.setLongitude(request.longitude());
        hub.setStatus(request.status());
        hub.setHandlingCapacityPerDay(request.handlingCapacityPerDay());

        Hub saved = hubRepository.save(hub);

        // Status (e.g. ACTIVE -> TEMPORARILY_CLOSED) or capacity changes affect
        // which routes are usable, so the in-memory graph must be rebuilt.
        graphLoaderService.reloadGraph();

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteHub(String code) {
        Hub hub = hubRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new HubNotFoundException("Hub not found with code: " + code));

        if (routeRepository.existsBySourceHubIdOrDestinationHubId(hub.getId(), hub.getId())) {
            throw new IllegalArgumentException(
                    "Cannot delete hub " + hub.getCode() + ": it still has routes referencing it. Delete those routes first.");
        }

        hubRepository.delete(hub);
        graphLoaderService.reloadGraph();
    }

    private HubResponseDto mapToResponse(Hub hub) {
        return new HubResponseDto(
                hub.getId(),
                hub.getCode(),
                hub.getName(),
                hub.getCity(),
                hub.getCountry(),
                hub.getLatitude(),
                hub.getLongitude(),
                hub.getStatus(),
                hub.getHandlingCapacityPerDay(),
                hub.getCreatedAt()
        );
    }
}
