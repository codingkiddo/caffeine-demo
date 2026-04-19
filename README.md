# Caffeine Demo

Production-style **Spring Boot + Caffeine Cache** example showing how to implement fast in-memory caching with:

- `@Cacheable` for read-through caching
- `@CachePut` for write-through / cache warm-up
- `@CacheEvict` for invalidation
- per-cache TTL and size policies
- Actuator + Micrometer cache metrics
- manual cache inspection and eviction endpoints
- tests that verify actual cache behavior

This project is designed as a practical reference for learning and showcasing how to use **Caffeine** in a real Spring Boot service.

---

## Why this project?

A lot of cache demos stop at a single `@Cacheable` annotation.

This project goes further and shows:

- how to define **different cache policies per cache**
- how to keep cache entries consistent after create/update/delete
- how to expose cache statistics for observability
- how to verify cache semantics with tests
- how to avoid caching unstable entities such as drafts

---

## Features

### 1) Read-through caching with `@Cacheable`
`getById(id)` caches product lookups by ID.

- first call -> hits repository / DB
- second call -> served from cache
- `DRAFT` products are excluded from caching via `unless`

### 2) Write-through / warm-up with `@CachePut`
`create(...)` immediately stores the newly created product in cache.

That means the next `getById(newId)` can be served without another DB round trip.

`update(...)` refreshes the cache entry after a successful write.

### 3) Eviction with `@CacheEvict`
`delete(id)` removes the cache entry only after successful deletion.

This avoids stale cache data while preserving correctness if deletion fails.

### 4) Per-cache policies
Instead of one global configuration, this demo uses different settings for different cache types:

- `products` -> higher traffic, moderate TTL
- `categories` -> small and stable, longer TTL
- `users` -> smaller and shorter-lived

### 5) Observability
Cache metrics are exposed through Spring Boot Actuator and Micrometer.

You can inspect:
- cache hit/miss behavior
- eviction counts
- cache metrics
- Prometheus-friendly metrics endpoint

### 6) Manual cache administration
Admin endpoints allow you to:
- inspect all cache stats
- inspect stats for a specific cache
- evict a single cache key manually

### 7) Test coverage for cache behavior
Tests verify:
- first call hits DB, second call comes from cache
- DRAFT products are not cached
- create warms cache
- update refreshes cache
- delete evicts cache entry

---

## Tech stack

- **Java 21**
- **Spring Boot 3**
- **Spring Cache**
- **Caffeine**
- **Spring Data JPA**
- **H2 in-memory database**
- **Spring Boot Actuator**
- **Micrometer + Prometheus registry**
- **JUnit 5 + Mockito**

---

## Cache strategy used

### Default cache spec
A default Caffeine policy is configured for general use.

### Per-cache overrides
Custom cache definitions override the default behavior for specific named caches.

Example design intent:

- **products**
  - larger cache
  - moderate TTL
  - suited for high-read traffic

- **categories**
  - smaller set
  - longer TTL
  - suitable for mostly static reference data

- **users**
  - shorter TTL
  - smaller size
  - better for sensitive or frequently changing data

---

## API overview

## Product API

Base path: `/api/products`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | List all products |
| GET | `/api/products/{id}` | Get product by ID (cached) |
| GET | `/api/products/category/{categoryId}` | List products by category |
| POST | `/api/products` | Create product and warm cache |
| PATCH | `/api/products/{id}` | Update product and refresh cache |
| PUT | `/api/products/{id}` | Replace product |
| DELETE | `/api/products/{id}` | Delete product and evict cache |

## Cache Admin API

Base path: `/admin/cache`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/cache/stats` | Stats for all caches |
| GET | `/admin/cache/stats/{name}` | Stats for one cache |
| DELETE | `/admin/cache/{name}/{key}` | Evict one cache key |

> In a real production service, these admin endpoints should be secured.

---

## Run locally

### Prerequisites

- Java 21
- Maven 3.9+

### Start the app

```bash
mvn spring-boot:run
