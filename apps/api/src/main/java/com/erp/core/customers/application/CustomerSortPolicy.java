package com.erp.core.customers.application;

import jakarta.validation.ConstraintViolationException;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Converts raw sort parameters into a Sort restricted to the public customer fields. */
final class CustomerSortPolicy {
    private static final Set<String> ALLOWED_PROPERTIES = Set.of(
            "name", "document", "personType", "active", "createdAt", "updatedAt");

    private CustomerSortPolicy() { }

    static Sort fromParameters(List<String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Sort.by(Sort.Order.asc("name"));
        }

        List<Sort.Order> orders = new ArrayList<>(parameters.size());
        for (String parameter : parameters) {
            orders.add(parseOrder(parameter));
        }
        return Sort.by(orders);
    }

    private static Sort.Order parseOrder(String parameter) {
        if (parameter == null) {
            throw invalidSort();
        }

        String[] parts = parameter.split(",", -1);
        if (parts.length != 2 || !ALLOWED_PROPERTIES.contains(parts[0])) {
            throw invalidSort();
        }

        Sort.Direction direction = switch (parts[1]) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw invalidSort();
        };
        return new Sort.Order(direction, parts[0]);
    }

    private static ConstraintViolationException invalidSort() {
        // ApiExceptionHandler already maps constraint violations to a generic HTTP 400 response.
        return new ConstraintViolationException("Invalid sort parameter", Set.of());
    }
}
