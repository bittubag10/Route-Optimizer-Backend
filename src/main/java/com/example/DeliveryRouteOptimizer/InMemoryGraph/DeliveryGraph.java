package com.example.DeliveryRouteOptimizer.InMemoryGraph;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DeliveryGraph {
    // Adjacency List: HubCode -> List of Outgoing Edges
    private final Map<String, List<GraphEdge>> adjacencyList = new ConcurrentHashMap<>();

    // Registered Active Hubs
    private final Set<String> registeredHubs = ConcurrentHashMap.newKeySet();

    public void addHub(String hubCode) {
        registeredHubs.add(hubCode.toUpperCase());
        adjacencyList.putIfAbsent(hubCode.toUpperCase(), new CopyOnWriteArrayList<>());
    }

    public void addEdge(GraphEdge edge) {
        addHub(edge.getSourceHubCode());
        addHub(edge.getDestinationHubCode());

        List<GraphEdge> edges = adjacencyList.get(edge.getSourceHubCode().toUpperCase());
        // Remove duplicate route if updating
        edges.removeIf(e -> e.getRouteId().equals(edge.getRouteId()));
        edges.add(edge);
    }

    public List<GraphEdge> getOutgoingEdges(String hubCode) {
        return adjacencyList.getOrDefault(hubCode.toUpperCase(), Collections.emptyList());
    }

    public boolean containsHub(String hubCode) {
        return registeredHubs.contains(hubCode.toUpperCase());
    }

    public void clearGraph() {
        adjacencyList.clear();
        registeredHubs.clear();
    }

    public Set<String> getAllHubs() {
        return Collections.unmodifiableSet(registeredHubs);
    }
}
