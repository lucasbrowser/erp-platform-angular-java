package com.erp.core.catalog.api.v2;

import com.erp.core.catalog.domain.Product;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record ProductV2Response(UUID id, String sku, String name, String description, UUID categoryId,
                                String price, boolean active, Instant createdAt, Instant updatedAt) {
    public static ProductV2Response from(Product product) {
        String price = product.getPrice().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        return new ProductV2Response(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getCategory().getId(), price, product.isActive(), product.getCreatedAt(), product.getUpdatedAt());
    }
}
