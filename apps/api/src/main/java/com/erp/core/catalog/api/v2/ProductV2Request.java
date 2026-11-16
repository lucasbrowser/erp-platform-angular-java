package com.erp.core.catalog.api.v2;

import com.erp.core.catalog.api.ProductRequest;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** Version 2 wire DTO: monetary values are canonical decimal strings. */
public record ProductV2Request(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 2000) String description,
        @NotNull UUID categoryId,
        @NotNull @JsonDeserialize(using = StrictDecimalStringDeserializer.class)
        @Pattern(regexp = "(?:0|[1-9][0-9]{0,11})\\.[0-9]{2}") String price) {

    public ProductRequest toV1DomainRequest() {
        return new ProductRequest(sku, name, description, categoryId, new BigDecimal(price));
    }
}
