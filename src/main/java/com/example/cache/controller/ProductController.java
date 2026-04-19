package com.example.cache.controller;

import com.example.cache.dto.CreateProductRequest;
import com.example.cache.dto.ProductDto;
import com.example.cache.dto.UpdateProductRequest;
import com.example.cache.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductDto> getAll() {
        return productService.getAll();
    }

    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductDto> getByCategory(@PathVariable Long categoryId) {
        return productService.getByCategory(categoryId);
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@RequestBody @Valid CreateProductRequest req) {
        ProductDto created = productService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public ProductDto update(@PathVariable Long id,
                             @RequestBody @Valid UpdateProductRequest req) {
        return productService.update(id, req);
    }

    @PutMapping("/{id}")
    public ProductDto replace(@PathVariable Long id,
                              @RequestBody @Valid CreateProductRequest req) {
        return productService.replace(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
