# 📦 Delivery Route Optimizer

A Spring Boot backend that models a logistics network as a **graph** (hubs =
nodes, routes = weighted edges) and calculates the cheapest / fastest /
fewest-hop delivery route between hubs using **Dijkstra's Algorithm** — with
real-time traffic, disruption surcharges, response caching, and small-scale
multi-stop (TSP-style) routing on top.

---

## 🧱 Tech Stack

| Layer          | Technology                                  |
|----------------|----------------------------------------------|
| Language       | Java 21                                       |
| Framework      | Spring Boot 4.1.1 (Web MVC, Validation, Cache)|
| Persistence    | Spring Data JPA + MySQL                       |
| Build tool     | Maven                                         |
| Algorithms     | Dijkstra (cheapest/fastest), BFS (min-hops), Backtracking Permutation Search (multi-stop) |
| Caching        | Spring Cache (`ConcurrentMapCacheManager`)    |
| Boilerplate    | Lombok                                        |

---

## 🏗️ Architecture


**Why an in-memory graph?** Hitting MySQL on every Dijkstra edge-relaxation
would be slow. Instead, `GraphLoaderService` loads all active hubs/routes into
an in-memory adjacency list (`DeliveryGraph`) once, and reloads it whenever a
hub, route, or disruption changes — the algorithm itself never touches the
database.

---

## ✨ Features

### 1. Graph-based shortest route (Dijkstra)
Three routing strategies, selectable via `preference`:
- **CHEAPEST** — Dijkstra minimizing total cost
- **FASTEST** — Dijkstra minimizing total duration
- **MIN_HOPS** — BFS minimizing number of stops

### 2. Location (Hub) & Route management APIs
Full CRUD for both hubs and routes (create/read/update/delete), with
validation and meaningful error messages. Every mutation triggers an in-memory
graph reload so the optimizer always sees fresh data.

### 3. Shortest Path API

`TrafficService` derives a `TrafficLevel` (LOW / MODERATE / HIGH) from the
current `LocalTime`, using configurable peak/moderate windows
(`application.properties`). The optimizer fetches this once per request and
applies it uniformly while relaxing edges — nothing about traffic is
hardcoded into the algorithm itself.

### 7. Multi-stop delivery routing (small-scale TSP)

1. Computes the shortest path between every pair of {start} ∪ {stops} (reuses
   Dijkstra + caching from Feature 4).
2. Tries every visiting order of the stops via backtracking, with
   branch-and-bound pruning, and keeps the cheapest/fastest/fewest-hop total.
3. This is an **exact but exponential (O(n!)) brute force** — intentionally,
   since it's only meant for a small number of stops. It's capped by
   `optimizer.multi-stop.max-stops` (default `8`). For a larger fleet, swap the
   `searchRecursive` method for nearest-neighbour + 2-opt or Held-Karp DP —
   everything else (pairwise matrix, response shape) stays the same.

---

## 🔌 API Reference

### Hubs — `/api/v1/hubs`
| Method | Path            | Description        |
|--------|-----------------|---------------------|
| POST   | `/`             | Create a hub        |
| GET    | `/`             | List all hubs        |
| GET    | `/{code}`       | Get hub by code       |
| PUT    | `/{code}`       | Update a hub           |
| DELETE | `/{code}`       | Delete a hub (fails if routes still reference it) |

### Routes — `/api/v1/routes`
| Method | Path        | Description                      |
|--------|-------------|-----------------------------------|
| POST   | `/`         | Create a route between two hubs   |
| GET    | `/`         | List all routes                    |
| GET    | `/{id}`     | Get route by id                     |
| PUT    | `/{id}`     | Update cost/duration/distance/status |
| DELETE | `/{id}`     | Delete a route                       |

### Route Optimizer — `/api/v1/optimizer`
| Method | Path            | Description                              |
|--------|-----------------|--------------------------------------------|
| GET    | `/route`        | Shortest path between two hubs (`source`, `destination`, `preference`) |
| GET    | `/multi-stop`   | Best visiting order across several stops (`start`, `stops`, `preference`) |

### Disruptions — `/api/v1/disruptions`
Raise/list network disruptions (accidents, weather, closures) that apply a
temporary cost multiplier / delay to specific routes.

---

## ⚙️ Configuration (`application.properties`)

```properties
# Traffic (Feature 5 & 6)
traffic.peak.start=18:00
traffic.peak.end=20:00
traffic.moderate.start=08:00
traffic.moderate.end=10:00
traffic.multiplier.peak=2.0
traffic.multiplier.moderate=1.5
traffic.multiplier.normal=1.0

# Multi-stop routing (Feature 7)
optimizer.multi-stop.max-stops=8
```

---

## 🚀 Running Locally

1. Create a MySQL database and update the datasource credentials in
   `src/main/resources/application.properties`.
2. Build & run:
```bash
   mvn clean install
   mvn spring-boot:run
```
3. `DataSeeder` seeds a few sample hubs/routes on first startup so you can hit
   the optimizer endpoints immediately.
4. Run tests:
```bash
   mvn test
```

---

## 🧪 Example

Request: GET /api/v1/optimizer/route?source=DEL&destination=BLR&preference=CHEAPEST



Response:
```json
{
  "source": "DEL",
  "destination": "BLR",
  "preference": "CHEAPEST",
  "totalCost": 450.00,
  "totalDurationMinutes": 620,
  "totalDistanceKm": 2150.0,
  "totalStops": 1,
  "hubPath": ["DEL", "BOM", "BLR"],
  "segments": [ ... ],
  "appliedTrafficLevel": "NORMAL",
  "appliedTrafficMultiplier": 1.0
}
```

---

## 📌 Design Notes (useful for interview walkthroughs)

- **Separation of concerns:** controllers never touch the repository or the
  graph directly — everything flows through a service.
- **Why cache key includes traffic level:** avoids a manual eviction job for
  traffic changes; the key itself makes stale entries unreachable.
- **Why hub deletion is blocked, not cascaded:** silently cascading deletes on
  a hub with live routes is a common production bug source — this forces an
  explicit, intentional cleanup order instead.
- **Why multi-stop is brute-force:** for ≤8 stops, exact O(n!) is fast and
  simple to explain; for more stops, the seam to swap in a heuristic is a
  single method (`searchRecursive`).