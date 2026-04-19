package com.example.cache.dto;

import jakarta.validation.constraints.*;
import lombok.Value;

import java.math.BigDecimal;

@Value
public class CreateProductRequest {

    @NotBlank(message = "name is required")
    @Size(max = 120)
    String name;

    String description;

    @NotNull
    @Positive(message = "price must be positive")
    BigDecimal price;

    @NotNull
    @PositiveOrZero
    Integer stock;

    @NotNull
    Long categoryId;
}
