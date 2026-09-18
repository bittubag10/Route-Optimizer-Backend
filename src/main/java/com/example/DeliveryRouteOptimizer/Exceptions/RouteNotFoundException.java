package com.example.DeliveryRouteOptimizer.Exceptions;

public class RouteNotFoundException extends RuntimeException{
    public RouteNotFoundException(String message) {
        super(message);
    }
}
