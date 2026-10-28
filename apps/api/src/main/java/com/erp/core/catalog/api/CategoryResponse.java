package com.erp.core.catalog.api;

import com.erp.core.catalog.domain.Category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String description, boolean active,
                               Instant createdAt, Instant updatedAt) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(),
                category.isActive(), category.getCreatedAt(), category.getUpdatedAt());
    }
}
