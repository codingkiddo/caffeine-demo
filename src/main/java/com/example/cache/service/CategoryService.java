package com.example.cache.service;

import com.example.cache.dto.CategoryDto;
import com.example.cache.exception.ResourceNotFoundException;
import com.example.cache.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.cache.config.CacheConfig.CACHE_CATEGORIES;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = CACHE_CATEGORIES)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(key = "#id")
    public CategoryDto getById(Long id) {
        log.debug("CACHE MISS — loading category id={}", id);
        return categoryRepository.findById(id)
                .map(CategoryDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    // Cache the full list under a fixed key — valid because categories rarely change.
    @Cacheable(key = "'all'")
    public List<CategoryDto> getAll() {
        log.debug("CACHE MISS — loading all categories");
        return categoryRepository.findAll()
                .stream()
                .map(CategoryDto::from)
                .toList();
    }

    @CacheEvict(allEntries = true)
    public void evictAll() {
        log.info("Category cache evicted");
    }
}
