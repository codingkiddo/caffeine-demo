package com.example.cache.service;

import com.example.cache.dto.CreateProductRequest;
import com.example.cache.dto.ProductDto;
import com.example.cache.dto.UpdateProductRequest;
import com.example.cache.entity.Category;
import com.example.cache.entity.Product;
import com.example.cache.exception.ResourceNotFoundException;
import com.example.cache.repository.CategoryRepository;
import com.example.cache.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.cache.config.CacheConfig.*;

/**
 * ProductService demonstrates all four Spring cache annotations:
 *
 *   @Cacheable   — read-through: returns cached value or populates on miss
 *   @CachePut    — write-through: always executes + updates cache
 *   @CacheEvict  — invalidation: removes entry (or all entries)
 *   @Caching     — compose multiple annotations on a single method
 *
 * Key design rules followed here:
 *   1. Cache keys are always explicit (#id, #result.id) — never rely on defaults.
 *   2. @CachePut is used on create/update so the cache is warm after writes
 *      without requiring a follow-up GET.
 *   3. The `unless` guard on getById skips caching DRAFT products —
 *      they change often and shouldn't pollute the cache.
 *   4. allEntries=true eviction is kept in a dedicated admin method, never in
 *      a hot path, because it locks the entire cache segment briefly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = CACHE_PRODUCTS)  // avoids repeating cacheNames on every annotation
public class ProductService {

    private final ProductRepository    productRepository;
    private final CategoryRepository   categoryRepository;

    // ────────────────────────────────────────────────────────────────────────
    // READ
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Cache hit  → returns ProductDto directly from Caffeine, no DB call.
     * Cache miss → executes method body, stores result, logs the miss.
     *
     * `unless` prevents caching DRAFT products (they're mutable and low-traffic).
     */
    @Cacheable(key = "#id", unless = "#result.status == 'DRAFT'")
    public ProductDto getById(Long id) {
        log.debug("CACHE MISS — loading product id={} from DB", id);
        return productRepository.findByIdWithCategory(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    /**
     * Lists are NOT individually cached here because a single update invalidates
     * the whole list. Cache the list only if it's immutable or very stable.
     * For mutable lists, prefer per-ID caching + client-side aggregation.
     */
    public List<ProductDto> getAll() {
        return productRepository.findAllWithCategory()
                .stream()
                .map(ProductDto::from)
                .toList();
    }

    public List<ProductDto> getByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(ProductDto::from)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────────────
    // CREATE — @CachePut warms the cache immediately after insert
    // ────────────────────────────────────────────────────────────────────────

    /**
     * @CachePut: always executes, then stores the returned DTO under key=result.id.
     * Subsequent getById(newId) will be a cache hit with zero DB round-trips.
     */
    @CachePut(key = "#result.id")
    @Transactional
    public ProductDto create(CreateProductRequest req) {
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));

        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stock(req.getStock())
                .status(Product.ProductStatus.ACTIVE)
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        log.debug("Created product id={}, cache warmed", saved.getId());
        return ProductDto.from(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // UPDATE — @CachePut keeps cache consistent after writes
    // ────────────────────────────────────────────────────────────────────────

    @CachePut(key = "#id")
    @Transactional
    public ProductDto update(Long id, UpdateProductRequest req) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (req.getName()        != null) product.setName(req.getName());
        if (req.getDescription() != null) product.setDescription(req.getDescription());
        if (req.getPrice()       != null) product.setPrice(req.getPrice());
        if (req.getStock()       != null) product.setStock(req.getStock());

        Product saved = productRepository.save(product);
        log.debug("Updated product id={}, cache refreshed", id);
        return ProductDto.from(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    // DELETE — @CacheEvict removes the entry after successful deletion
    // ────────────────────────────────────────────────────────────────────────

    /**
     * beforeInvocation=false (default) means the eviction happens AFTER the
     * method returns. If deleteById throws, the cache entry is preserved —
     * which is correct behaviour.
     */
    @CacheEvict(key = "#id")
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
        log.debug("Deleted product id={}, cache evicted", id);
    }

    // ────────────────────────────────────────────────────────────────────────
    // ADMIN — bulk eviction, typically called after imports or migrations
    // ────────────────────────────────────────────────────────────────────────

    /**
     * @Caching composes evict + put on the same method.
     * Used when you need to atomically replace a product (delete + re-insert).
     */
    @Caching(
        evict = @CacheEvict(key = "#id"),
        put   = @CachePut(key   = "#id")
    )
    @Transactional
    public ProductDto replace(Long id, CreateProductRequest req) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));

        existing.setName(req.getName());
        existing.setDescription(req.getDescription());
        existing.setPrice(req.getPrice());
        existing.setStock(req.getStock());
        existing.setCategory(category);

        return ProductDto.from(productRepository.save(existing));
    }

    /** Full cache flush — use sparingly; prefer per-key eviction in hot paths. */
    @CacheEvict(allEntries = true)
    public void evictAll() {
        log.warn("Full product cache eviction triggered");
    }
}
