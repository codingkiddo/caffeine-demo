package com.example.cache.controller;

import com.example.cache.dto.CreateProductRequest;
import com.example.cache.dto.ProductDto;
import com.example.cache.exception.ResourceNotFoundException;
import com.example.cache.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerIntegrationTest {

    @Autowired MockMvc       mvc;
    @Autowired ObjectMapper  mapper;
    @MockBean  ProductService productService;

    private static final ProductDto PRODUCT_DTO = ProductDto.builder()
            .id(1L).name("Laptop").description("16-inch")
            .price(BigDecimal.valueOf(129999)).stock(50)
            .status("ACTIVE").categoryId(1L).categoryName("Electronics")
            .build();

    @Test
    @DisplayName("GET /api/products/{id} returns 200 + DTO")
    void getById_returns200() throws Exception {
        when(productService.getById(1L)).thenReturn(PRODUCT_DTO);

        mvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 404 for unknown id")
    void getById_returns404() throws Exception {
        when(productService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Product", 99L));

        mvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/products returns list")
    void getAll_returnsList() throws Exception {
        when(productService.getAll()).thenReturn(List.of(PRODUCT_DTO));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/products returns 201 + Location header")
    void create_returns201() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Laptop", "16-inch", BigDecimal.valueOf(129999), 50, 1L);

        when(productService.create(any())).thenReturn(PRODUCT_DTO);

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/products/1")))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/products with blank name returns 400 validation error")
    void create_invalidRequest_returns400() throws Exception {
        CreateProductRequest bad = new CreateProductRequest(
                "", "desc", BigDecimal.ONE, 1, 1L);

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} returns 204")
    void delete_returns204() throws Exception {
        mvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
