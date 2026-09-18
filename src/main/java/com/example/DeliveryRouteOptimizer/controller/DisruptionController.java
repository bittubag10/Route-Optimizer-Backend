package com.example.DeliveryRouteOptimizer.controller;

import com.example.DeliveryRouteOptimizer.dto.request.DisruptionCreateRequestDto;
import com.example.DeliveryRouteOptimizer.dto.response.DisruptionResponseDto;
import com.example.DeliveryRouteOptimizer.service.DisruptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/disruptions")
@RequiredArgsConstructor
public class DisruptionController {
    private final DisruptionService disruptionService;

    @PostMapping
    public ResponseEntity<DisruptionResponseDto> createDisruption(@Valid @RequestBody DisruptionCreateRequestDto request) {
        DisruptionResponseDto response = disruptionService.createDisruption(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
