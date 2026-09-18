package com.example.DeliveryRouteOptimizer.dto.request;

import com.example.DeliveryRouteOptimizer.enums.HubStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;


public record HubCreateRequestDto( @NotBlank(message = "Hub code is required")
@Pattern(regexp = "^[A-Z0-9]{3,10}$", message = "Hub code must be 3-10 alphanumeric uppercase characters (e.g. DEL, BOM)")
String code,

@NotBlank(message = "Hub name cannot be blank")
@Size(max = 150, message = "Hub name cannot exceed 150 characters")
String name,

@NotBlank(message = "City cannot be blank")
@Size(max = 100, message = "City name cannot exceed 100 characters")
String city,

@NotBlank(message = "Country cannot be blank")
@Size(max = 100, message = "Country name cannot exceed 100 characters")
String country,

@DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
@DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
BigDecimal latitude,

@DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
@DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
BigDecimal longitude,

@NotNull(message = "Hub status is required") HubStatus status,

@Positive(message = "Handling capacity must be greater than 0")
Integer handlingCapacityPerDay
) {}
