package com.erp.core.customers.application;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerSortPolicyTest {
    @Test
    void acceptsEveryAllowlistedPropertyUsingExactNames() {
        List<String> properties = List.of(
                "name", "document", "personType", "active", "createdAt", "updatedAt");

        for (String property : properties) {
            Sort sort = CustomerSortPolicy.fromParameters(List.of(property + ",asc"));

            assertThat(sort.getOrderFor(property)).isNotNull();
        }
    }

    @Test
    void rejectsPropertiesOutsideTheAllowlistAndNestedPaths() {
        for (String parameter : List.of(
                "id,asc", "address.city,asc", "name.toString,asc", " name,asc", "name ,asc",
                "name desc,asc", "name,asc;delete from customers")) {
            assertInvalid(parameter);
        }
    }

    @Test
    void acceptsAscendingAndDescendingDirections() {
        Sort sort = CustomerSortPolicy.fromParameters(List.of("name,asc", "createdAt,desc"));

        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void rejectsInvalidOrMissingDirectionsAndMalformedCriteria() {
        for (String parameter : List.of(
                "name,ascending", "name,", "name", "name,asc,document", "name,ASC", "")) {
            assertInvalid(parameter);
        }
        assertThatThrownBy(() -> CustomerSortPolicy.fromParameters(java.util.Arrays.asList((String) null)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void preservesThePrecedenceOfMultipleCriteria() {
        Sort sort = CustomerSortPolicy.fromParameters(
                List.of("name,asc", "createdAt,desc", "document,asc"));

        assertThat(sort.stream().map(Sort.Order::getProperty))
                .containsExactly("name", "createdAt", "document");
    }

    @Test
    void defaultsMissingSortParametersToNameAscending() {
        assertThat(CustomerSortPolicy.fromParameters(null))
                .containsExactly(Sort.Order.asc("name"));
        assertThat(CustomerSortPolicy.fromParameters(List.of()))
                .containsExactly(Sort.Order.asc("name"));
    }

    @Test
    void invalidParameterMessagesDoNotEchoTheSuppliedValue() {
        String supplied = "secretInternalField,desc";

        assertThatThrownBy(() -> CustomerSortPolicy.fromParameters(List.of(supplied)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("Invalid sort parameter")
                .hasMessageNotContaining(supplied);
    }

    private static void assertInvalid(String parameter) {
        assertThatThrownBy(() -> CustomerSortPolicy.fromParameters(List.of(parameter)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("Invalid sort parameter");
    }
}
