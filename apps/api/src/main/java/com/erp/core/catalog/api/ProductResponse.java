package com.erp.core.catalog.api;

import com.erp.core.catalog.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(UUID id, String sku, String name, String description, UUID categoryId,
                              BigDecimal price, boolean active, Instant createdAt, Instant updatedAt) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getCategory().getId(), product.getPrice(), product.isActive(),
                product.getCreatedAt(), product.getUpdatedAt());
    }
}
