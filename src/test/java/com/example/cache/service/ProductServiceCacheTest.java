package com.example.cache.service;

import com.example.cache.config.CacheConfig;
import com.example.cache.dto.CreateProductRequest;
import com.example.cache.dto.ProductDto;
import com.example.cache.dto.UpdateProductRequest;
import com.example.cache.entity.Category;
import com.example.cache.entity.Product;
import com.example.cache.exception.ResourceNotFoundException;
import com.example.cache.repository.CategoryRepository;
import com.example.cache.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests that run the full Spring context so @Cacheable AOP proxies
 * are active. The repository is mocked so tests are fast and DB-free.
 *
 * Each test clears the cache in @BeforeEach to guarantee isolation.
 */
@SpringBootTest
class ProductServiceCacheTest {

    @Autowired ProductService    productService;
    @Autowired CacheManager      cacheManager;
    @MockBean  ProductRepository productRepository;
    @MockBean  CategoryRepository categoryRepository;

    private static final Long      ID       = 1L;
    private static final Category  CATEGORY = new Category(10L, "Electronics", "desc");
    private static final Product   PRODUCT  = Product.builder()
            .id(ID).name("Laptop").description("16-inch").price(BigDecimal.valueOf(129999))
            .stock(50).status(Product.ProductStatus.ACTIVE).category(CATEGORY).build();

    @BeforeEach
    void clearCache() {
        cacheManager.getCache(CacheConfig.CACHE_PRODUCTS).clear();
    }

    // ── @Cacheable ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("First call hits DB; second call is served from cache")
    void cacheable_secondCallFromCache() {
        when(productRepository.findByIdWithCategory(ID)).thenReturn(Optional.of(PRODUCT));

        ProductDto first  = productService.getById(ID);
        ProductDto second = productService.getById(ID);

        assertThat(first).isEqualTo(second);
        // Repository called exactly ONCE despite two getById() calls
        verify(productRepository, times(1)).findByIdWithCategory(ID);
    }

    @Test
    @DisplayName("DRAFT products are NOT cached (unless guard)")
    void cacheable_draftNotCached() {
        Product draft = Product.builder()
                .id(2L).name("Draft").description("").price(BigDecimal.ONE)
                .stock(0).status(Product.ProductStatus.DRAFT).category(CATEGORY).build();

        when(productRepository.findByIdWithCategory(2L)).thenReturn(Optional.of(draft));

        productService.getById(2L);
        productService.getById(2L);

        // Both calls go to the repository because DRAFT is excluded from cache
        verify(productRepository, times(2)).findByIdWithCategory(2L);
    }

    @Test
    @DisplayName("Unknown product throws ResourceNotFoundException")
    void cacheable_notFoundThrows() {
        when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── @CachePut ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() warms the cache — subsequent getById() skips DB")
    void cachePut_createWarmsCache() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(CATEGORY));
        when(productRepository.save(any())).thenReturn(PRODUCT);
        // No stub for findByIdWithCategory — it must NOT be called

        CreateProductRequest req = new CreateProductRequest(
                "Laptop", "16-inch", BigDecimal.valueOf(129999), 50, 10L);

        ProductDto created = productService.create(req);

        // Now call getById — should be a cache hit (zero additional DB calls)
        ProductDto fetched = productService.getById(created.getId());

        assertThat(fetched).isEqualTo(created);
        verify(productRepository, never()).findByIdWithCategory(any());
    }

    @Test
    @DisplayName("update() refreshes the cache entry")
    void cachePut_updateRefreshesCache() {
        // Prime the cache with original product
        when(productRepository.findByIdWithCategory(ID)).thenReturn(Optional.of(PRODUCT));
        productService.getById(ID);

        // Now update
        Product updated = Product.builder()
                .id(ID).name("Laptop UPDATED").description("16-inch")
                .price(BigDecimal.valueOf(119999)).stock(40)
                .status(Product.ProductStatus.ACTIVE).category(CATEGORY).build();

        when(productRepository.findByIdWithCategory(ID)).thenReturn(Optional.of(updated));
        when(productRepository.save(any())).thenReturn(updated);

        UpdateProductRequest req = new UpdateProductRequest("Laptop UPDATED", null,
                BigDecimal.valueOf(119999), 40);
        ProductDto result = productService.update(ID, req);

        assertThat(result.getName()).isEqualTo("Laptop UPDATED");

        // Next getById must return updated value from cache, not the old one
        ProductDto fromCache = productService.getById(ID);
        assertThat(fromCache.getName()).isEqualTo("Laptop UPDATED");
        // findByIdWithCategory called twice: once for getById (miss), once inside update()
        verify(productRepository, times(2)).findByIdWithCategory(ID);
    }

    // ── @CacheEvict ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() evicts the cache entry — next getById() hits DB again")
    void cacheEvict_deleteEvictsEntry() {
        when(productRepository.findByIdWithCategory(ID)).thenReturn(Optional.of(PRODUCT));
        when(productRepository.existsById(ID)).thenReturn(true);

        productService.getById(ID);   // populates cache
        productService.delete(ID);    // @CacheEvict fires

        // Cache is now empty — next call must go to DB
        productService.getById(ID);

        // findByIdWithCategory called twice: before delete, then after eviction
        verify(productRepository, times(2)).findByIdWithCategory(ID);
    }

    @Test
    @DisplayName("evictAll() clears the entire cache")
    void cacheEvict_allEntries() {
        when(productRepository.findByIdWithCategory(any())).thenReturn(Optional.of(PRODUCT));

        // Prime with two different IDs
        productService.getById(1L);
        productService.getById(2L);

        productService.evictAll();

        // Both must miss now
        productService.getById(1L);
        productService.getById(2L);

        verify(productRepository, times(2)).findByIdWithCategory(1L);
        verify(productRepository, times(2)).findByIdWithCategory(2L);
    }
}
