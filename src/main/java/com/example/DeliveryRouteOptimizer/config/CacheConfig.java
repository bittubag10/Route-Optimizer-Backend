package com.example.DeliveryRouteOptimizer.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's caching abstraction for the shortest-path calculation
 * (Feature 4).
 * <p>
 * Started with Spring's built-in {@link ConcurrentMapCacheManager} rather
 * than Redis since Redis isn't part of this project yet. If Redis is added
 * later, only this bean needs to change to a RedisCacheManager - the
 * {@code @Cacheable}/{@code @CacheEvict} annotations elsewhere never need to
 * know which cache provider is behind them.
 * <p>
 * Invalidation strategy:
 * <ul>
 *     <li>Graph changes (hub/route created, updated, deleted, or a disruption
 *     raised) call {@code GraphLoaderService#reloadGraph()}, which is annotated
 *     to evict every entry in both caches below - a stale path is never served
 *     after the network itself changes.</li>
 *     <li>Traffic changes (peak hours starting/ending) don't need manual
 *     eviction at all: the current {@code TrafficLevel} is baked into the cache
 *     key itself, so a lookup made during peak hours can never collide with one
 *     made during normal hours - they simply live under different keys.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Cache name used for shortest-path results. Key = source_destination_preference_trafficLevel. */
    public static final String SHORTEST_ROUTE_CACHE = "shortestRoutes";

    /** Cache name used for multi-stop delivery route plans. Key = start_sortedStops_preference_trafficLevel. */
    public static final String MULTI_STOP_ROUTE_CACHE = "multiStopRoutes";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(SHORTEST_ROUTE_CACHE, MULTI_STOP_ROUTE_CACHE);
    }
}
