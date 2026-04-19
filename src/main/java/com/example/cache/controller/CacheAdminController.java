package com.example.cache.controller;

import com.example.cache.service.CacheOperationsService;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoints for cache inspection and manual control.
 * In production, secure these behind an admin role.
 *
 * GET  /admin/cache/stats            → hit rates, miss counts, evictions for all caches
 * GET  /admin/cache/stats/{name}     → stats for one named cache
 * DELETE /admin/cache/{name}/{key}   → evict a single key
 * DELETE /admin/cache/{name}         → evict all entries in a cache
 * POST /admin/cache/warmup           → trigger product pre-warm (demo only)
 */
@RestController
@RequestMapping("/admin/cache")
@RequiredArgsConstructor
public class CacheAdminController {

    private final CacheOperationsService cacheOps;

    @GetMapping("/stats")
    public Map<String, CacheStats> allStats() {
        return cacheOps.allStats();
    }

    @GetMapping("/stats/{name}")
    public CacheStats stats(@PathVariable String name) {
        return cacheOps.stats(name);
    }

    @DeleteMapping("/{name}/{key}")
    public ResponseEntity<Void> evictKey(@PathVariable String name,
                                         @PathVariable String key) {
        // Keys are stored as Longs for product/category; try numeric parse first
        Object resolvedKey;
        try {
            resolvedKey = Long.parseLong(key);
        } catch (NumberFormatException e) {
            resolvedKey = key;
        }
        cacheOps.evict(name, resolvedKey);
        return ResponseEntity.noContent().build();
    }
}
