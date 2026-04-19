package com.example.cache.cache;

import com.example.cache.dto.ProductDto;
import com.example.cache.repository.ProductRepository;
import com.example.cache.service.CacheOperationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs once after the application context is fully started.
 *
 * Pre-warms the "products" cache by loading all ACTIVE products from the DB.
 * This avoids a thundering-herd of cache misses on first traffic after a restart.
 *
 * For large datasets: load only the top-N by access frequency, not the entire table.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmUpRunner implements ApplicationRunner {

    private final ProductRepository    productRepository;
    private final CacheOperationsService cacheOps;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache warm-up...");
        long start = System.currentTimeMillis();

        var raw = productRepository.findAllWithCategory();
        if (raw == null || raw.isEmpty()) {
            log.info("Cache warm-up skipped — no products found (expected during tests)");
            return;
        }

        List<ProductDto> active = raw.stream()
                .filter(p -> p.getStatus() == com.example.cache.entity.Product.ProductStatus.ACTIVE)
                .map(ProductDto::from)
                .toList();

        cacheOps.warmUpProducts(active);

        log.info("Cache warm-up complete: {} products loaded in {}ms",
                active.size(), System.currentTimeMillis() - start);
    }
}
