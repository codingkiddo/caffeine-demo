package com.example.cache.service;

import com.example.cache.dto.ProductDto;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Direct (programmatic) Caffeine cache operations.
 *
 * Use this when AOP annotations are not sufficient:
 *   - Bulk pre-warming on startup
 *   - Partial eviction by predicate
 *   - Reading raw stats for custom dashboards
 *   - Inspecting current cache contents
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheOperationsService {

    private final CacheManager cacheManager;

    // ── Pre-warming ──────────────────────────────────────────────────────────

    /**
     * Bulk-load a list of DTOs into the cache bypassing the AOP proxy.
     * Typically called from an ApplicationRunner on startup.
     */
    public void warmUpProducts(List<ProductDto> products) {
        Cache cache = requireCache("products");
        products.forEach(p -> cache.put(p.getId(), p));
        log.info("Cache warm-up: loaded {} products into 'products' cache", products.size());
    }

    // ── Manual eviction ──────────────────────────────────────────────────────

    public void evict(String cacheName, Object key) {
        Optional.ofNullable(cacheManager.getCache(cacheName))
                .ifPresentOrElse(
                    c -> { c.evict(key); log.debug("Evicted key={} from cache '{}'", key, cacheName); },
                    () -> log.warn("Cache '{}' not found — nothing evicted", cacheName)
                );
    }

    /** Evict all entries whose key (Long) is in the provided set. */
    public void evictByIds(String cacheName, List<Long> ids) {
        Cache cache = requireCache(cacheName);
        // CaffeineCache exposes the native cache for key-set iteration
        if (cache instanceof CaffeineCache cc) {
            cc.getNativeCache().invalidateAll(ids);
            log.debug("Bulk-evicted {} keys from '{}'", ids.size(), cacheName);
        }
    }

    // ── Stats ────────────────────────────────────────────────────────────────

    public CacheStats stats(String cacheName) {
        Cache springCache = requireCache(cacheName);
        if (springCache instanceof CaffeineCache cc) {
            return cc.getNativeCache().stats();
        }
        throw new IllegalStateException("Not a Caffeine cache: " + cacheName);
    }

    public Map<String, CacheStats> allStats() {
        return cacheManager.getCacheNames().stream()
                .collect(Collectors.toMap(
                    name -> name,
                    this::stats
                ));
    }

    // ── Scheduled stats logging (runs every 60 s) ────────────────────────────

    @Scheduled(fixedDelayString = "PT60S", initialDelayString = "PT30S")
    public void logAllStats() {
        allStats().forEach((name, s) ->
            log.info("[Cache Stats] name={} | requests={} | hitRate={:.2f}% | " +
                     "hits={} | misses={} | evictions={} | loadAvgNs={:.0f}",
                name,
                s.requestCount(),
                s.hitRate() * 100,
                s.hitCount(),
                s.missCount(),
                s.evictionCount(),
                s.averageLoadPenalty()
            )
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Cache requireCache(String name) {
        Cache c = cacheManager.getCache(name);
        if (c == null) throw new IllegalArgumentException("Unknown cache: " + name);
        return c;
    }
}
