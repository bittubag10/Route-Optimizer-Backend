package com.example.DeliveryRouteOptimizer.controller;

import com.example.DeliveryRouteOptimizer.dto.request.HubCreateRequestDto;
import com.example.DeliveryRouteOptimizer.dto.request.HubUpdateRequestDto;
import com.example.DeliveryRouteOptimizer.dto.response.HubResponseDto;
import com.example.DeliveryRouteOptimizer.service.HubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class HubController {
    private final HubService hubService;

    @PostMapping
    public ResponseEntity<HubResponseDto> createHub(@Valid @RequestBody HubCreateRequestDto request) {
        HubResponseDto created = hubService.createHub(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<HubResponseDto>> getAllHubs() {
        return ResponseEntity.ok(hubService.getAllHubs());
    }

    @GetMapping("/{code}")
    public ResponseEntity<HubResponseDto> getHubByCode(@PathVariable String code) {
        return ResponseEntity.ok(hubService.getHubByCode(code));
    }

    @PutMapping("/{code}")
    public ResponseEntity<HubResponseDto> updateHub(@PathVariable String code,
                                                     @Valid @RequestBody HubUpdateRequestDto request) {
        return ResponseEntity.ok(hubService.updateHub(code, request));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteHub(@PathVariable String code) {
        hubService.deleteHub(code);
        return ResponseEntity.noContent().build();
    }
}
