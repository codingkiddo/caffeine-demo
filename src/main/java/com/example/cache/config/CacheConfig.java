package com.example.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Central cache configuration.
 *
 * Design choices explained inline so this file doubles as reference material:
 *
 *  - One CaffeineCacheManager manages all caches via Spring's @Cacheable / @CacheEvict etc.
 *  - A default spec covers most caches; a CacheManagerCustomizer overrides specific ones.
 *  - recordStats() is mandatory for Micrometer/Actuator integration.
 *  - asyncCacheMode=true makes Spring call the async Caffeine API under the hood,
 *    which is important when you later swap in AsyncLoadingCache loaders.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // ── Cache names ─────────────────────────────────────────────────────────
    // Define as constants so controllers / services reference them without magic strings.
    public static final String CACHE_PRODUCTS   = "products";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_USERS      = "users";

    // ── Primary CacheManager bean ────────────────────────────────────────────
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.setCacheNames(List.of(CACHE_PRODUCTS, CACHE_CATEGORIES, CACHE_USERS));
        mgr.setCaffeine(defaultCaffeineSpec());
        // NOTE: setAsyncCacheMode(true) is intentionally omitted here.
        // It requires all registered caches to be AsyncCache/AsyncLoadingCache.
        // We will enable it in the async-loading deep-dive when we switch to
        // buildAsync() loaders in CacheManagerCustomizer.
        return mgr;
    }

    /**
     * Default spec: moderate size, 10-min write TTL, 5-min idle TTL.
     * Categories are long-lived; products and users are overridden below.
     */
    private Caffeine<Object, Object> defaultCaffeineSpec() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .expireAfterAccess(Duration.ofMinutes(5))
                .recordStats();   // REQUIRED for Micrometer hit/miss metrics
    }

    // ── Per-cache overrides ──────────────────────────────────────────────────
    /**
     * CacheManagerCustomizer runs after the CacheManager bean is fully
     * constructed, so registerCustomCache() safely replaces the default spec
     * for specific caches without affecting the others.
     */
    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> cacheManagerCustomizer() {
        return mgr -> {

            // "products": high-traffic, moderate TTL
            mgr.registerCustomCache(CACHE_PRODUCTS,
                    Caffeine.newBuilder()
                            .maximumSize(5_000)
                            .expireAfterWrite(Duration.ofMinutes(10))
                            .expireAfterAccess(Duration.ofMinutes(3))
                            .recordStats()
                            .build());

            // "categories": rarely changes — long TTL, small set
            mgr.registerCustomCache(CACHE_CATEGORIES,
                    Caffeine.newBuilder()
                            .maximumSize(100)
                            .expireAfterWrite(Duration.ofHours(1))
                            .recordStats()
                            .build());

            // "users": PII-adjacent — short TTL, smaller size
            mgr.registerCustomCache(CACHE_USERS,
                    Caffeine.newBuilder()
                            .maximumSize(2_000)
                            .expireAfterWrite(Duration.ofMinutes(5))
                            .expireAfterAccess(Duration.ofMinutes(2))
                            .recordStats()
                            .build());
        };
    }
}
