package com.erp.core.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 2000) String description,
        @NotNull UUID categoryId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal price) { }
