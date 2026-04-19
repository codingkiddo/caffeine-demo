package com.example.cache.dto;

import jakarta.validation.constraints.*;
import lombok.Value;

import java.math.BigDecimal;

@Value
public class UpdateProductRequest {

    @Size(max = 120)
    String name;

    String description;

    @Positive
    BigDecimal price;

    @PositiveOrZero
    Integer stock;
}
