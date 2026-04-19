package com.example.cache.dto;

import com.example.cache.entity.Product;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ProductDto implements Serializable so it's ready for Redis L2 serialization
 * in the upcoming deep-dive without changing this class.
 */
@Value
@Builder
public class ProductDto implements Serializable {

    Long   id;
    String name;
    String description;
    BigDecimal price;
    Integer    stock;
    String     status;
    Long       categoryId;
    String     categoryName;

    /** Factory method — keeps entity-to-DTO mapping in one place. */
    public static ProductDto from(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stock(p.getStock())
                .status(p.getStatus().name())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .build();
    }
}
