package com.example.DeliveryRouteOptimizer.Exceptions;

public class HubNotFoundException  extends RuntimeException{
    public HubNotFoundException(String message) {
        super(message);
    }
}
