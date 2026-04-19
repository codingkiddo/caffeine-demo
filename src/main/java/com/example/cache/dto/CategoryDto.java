package com.example.cache.dto;

import com.example.cache.entity.Category;
import lombok.Value;

import java.io.Serializable;

@Value
public class CategoryDto implements Serializable {
    Long   id;
    String name;
    String description;

    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription());
    }
}
