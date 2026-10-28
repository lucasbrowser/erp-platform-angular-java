package com.erp.core.catalog;

import com.erp.core.catalog.domain.CategoryRepository;
import com.erp.core.catalog.domain.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired JdbcTemplate jdbc;

    @Test
    @WithMockUser(roles = "MANAGER")
    void categoriesCanBeCreatedReadPagedUpdatedAndDeactivated() throws Exception {
        String body = "{\"name\":\"  Hardware  \",\"description\":\"Parts\"}";
        String response = mockMvc.perform(post("/api/v1/categories").with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hardware"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("id").asText());

        mockMvc.perform(get("/api/v1/categories/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/categories").param("page", "0").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[*].id").value(hasItem(id.toString())));
        mockMvc.perform(put("/api/v1/categories/{id}", id).with(csrf())
                        .contentType("application/json").content("{\"name\":\"Tools\",\"description\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Tools"));
        mockMvc.perform(delete("/api/v1/categories/{id}", id).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/categories/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(put("/api/v1/categories/{id}", id).with(csrf()).contentType("application/json")
                        .content("{\"name\":\"Tools updated\",\"description\":null,\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void categoryDuplicatesValidationAndMissingResourcesHaveStandardErrors() throws Exception {
        mockMvc.perform(post("/api/v1/categories").with(csrf()).contentType("application/json")
                        .content("{\"name\":\"DupCat\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/categories").with(csrf()).contentType("application/json")
                        .content("{\"name\":\" dupcat \"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.title").value("Conflict"));
        mockMvc.perform(post("/api/v1/categories").with(csrf()).contentType("application/json")
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
        mockMvc.perform(get("/api/v1/categories/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void productsCanBeCreatedFilteredUpdatedAndSoftDeleted() throws Exception {
        UUID categoryId = createCategory("ProductCategory");
        String productBody = productBody(" ab-12 ", "Widget", categoryId, "12.50");
        String response = mockMvc.perform(post("/api/v1/products").with(csrf()).contentType("application/json").content(productBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sku").value("AB-12"))
                .andExpect(jsonPath("$.price").value(12.50)).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("id").asText());

        mockMvc.perform(get("/api/v1/products/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products").param("name", "widg").param("sku", "ab-")
                        .param("categoryId", categoryId.toString()).param("active", "true").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(put("/api/v1/products/{id}", id).with(csrf()).contentType("application/json")
                        .content(productBody("AB-12", "Widget updated", categoryId, "14.00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Widget updated"));
        mockMvc.perform(delete("/api/v1/products/{id}", id).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/products/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        String updateAfterDeactivation = productBody("AB-12", "Widget changed after deactivation", categoryId, "14.00")
                .replace("}", ",\"active\":true}");
        mockMvc.perform(put("/api/v1/products/{id}", id).with(csrf()).contentType("application/json")
                        .content(updateAfterDeactivation))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void productRulesRejectDuplicateSkuNegativePriceAndInactiveOrMissingCategory() throws Exception {
        UUID categoryId = createCategory("ProductRulesCategory");
        mockMvc.perform(post("/api/v1/products").with(csrf()).contentType("application/json")
                        .content(productBody("SKU-1", "One", categoryId, "1.00")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/products").with(csrf()).contentType("application/json")
                        .content(productBody(" sku-1 ", "Two", categoryId, "2.00")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/products").with(csrf()).contentType("application/json")
                        .content(productBody("SKU-2", "Two", categoryId, "-0.01")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/products").with(csrf()).contentType("application/json")
                        .content(productBody("  ", " ", categoryId, "0.00")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/products").with(csrf()).contentType("application/json")
                        .content(productBody("SKU-3", "Three", UUID.randomUUID(), "2.00")))
                .andExpect(status().isNotFound());

        String existing = mockMvc.perform(post("/api/v1/products").with(csrf()).contentType("application/json")
                        .content(productBody("SKU-5", "Existing", categoryId, "2.00")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID existingId = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(existing).get("id").asText());
        var inactive = categories.findById(categoryId).orElseThrow();
        inactive.deactivate();
        categories.saveAndFlush(inactive);
        mockMvc.perform(put("/api/v1/products/{id}", existingId).with(csrf()).contentType("application/json")
                        .content(productBody("SKU-5", "Existing updated", categoryId, "3.00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Existing updated"));
        mockMvc.perform(post("/api/v1/products").with(csrf()).contentType("application/json")
                        .content(productBody("SKU-4", "Four", categoryId, "2.00")))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void catalogReadsDenyFinanceRole() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isForbidden());
    }

    @Test
    void catalogRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SALES")
    void salesCanReadButCannotChangeCatalog() throws Exception {
        mockMvc.perform(post("/api/v1/categories").with(csrf()).contentType("application/json")
                        .content("{\"name\":\"Forbidden\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void databaseConstraintsProtectSkuNormalizationPriceAndCategoryReference() throws Exception {
        String categoryName = "SqlConstraintCategory-" + UUID.randomUUID();
        UUID categoryId = categories.saveAndFlush(new com.erp.core.catalog.domain.Category(categoryName, null)).getId();
        UUID productId = UUID.randomUUID();
        String uniqueSku = "DB-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        Timestamp now = Timestamp.from(Instant.now());
        try {
            assertDatabaseConstraint(() -> jdbc.update(
                    "INSERT INTO erp.categories (id, name, active, created_at, updated_at) VALUES (?, ?, true, ?, ?)",
                    UUID.randomUUID(), categoryName.toUpperCase(), now, now));
            assertDatabaseConstraint(() -> jdbc.update(
                    "INSERT INTO erp.categories (id, name, active, created_at, updated_at) VALUES (?, ?, true, ?, ?)",
                    UUID.randomUUID(), "   ", now, now));
            jdbc.update("INSERT INTO erp.products (id, sku, name, category_id, price, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, ?, ?)",
                    productId, uniqueSku, "Database", categoryId, new BigDecimal("0.00"), now, now);

            assertDatabaseConstraint(() -> jdbc.update(
                    "INSERT INTO erp.products (id, sku, name, category_id, price, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, ?, ?)",
                    UUID.randomUUID(), uniqueSku, "Duplicate", categoryId, new BigDecimal("1.00"), now, now));
            assertDatabaseConstraint(() -> jdbc.update(
                    "INSERT INTO erp.products (id, sku, name, category_id, price, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, ?, ?)",
                    UUID.randomUUID(), "db-sku-" + UUID.randomUUID(), "Lowercase SKU", categoryId, new BigDecimal("1.00"), now, now));
            assertDatabaseConstraint(() -> jdbc.update(
                    "INSERT INTO erp.products (id, sku, name, category_id, price, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, ?, ?)",
                    UUID.randomUUID(), "BAD SKU ", "Bad normalization", categoryId, new BigDecimal("1.00"), now, now));
            assertDatabaseConstraint(() -> jdbc.update(
                    "INSERT INTO erp.products (id, sku, name, category_id, price, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, ?, ?)",
                    UUID.randomUUID(), "BLANK-NAME-" + UUID.randomUUID(), "   ", categoryId, new BigDecimal("1.00"), now, now));
            assertDatabaseConstraint(() -> jdbc.update(
                    "INSERT INTO erp.products (id, sku, name, category_id, price, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, ?, ?)",
                    UUID.randomUUID(), "NEGATIVE-" + UUID.randomUUID(), "Negative", categoryId, new BigDecimal("-1.00"), now, now));
            assertDatabaseConstraint(() -> jdbc.update(
                    "INSERT INTO erp.products (id, sku, name, category_id, price, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, ?, ?)",
                    UUID.randomUUID(), "NO-CAT-" + UUID.randomUUID(), "No category", UUID.randomUUID(), new BigDecimal("1.00"), now, now));
            assertThat(products.findById(productId)).isPresent();
        } finally {
            jdbc.update("DELETE FROM erp.products WHERE id = ?", productId);
            jdbc.update("DELETE FROM erp.categories WHERE id = ?", categoryId);
        }
    }

    private UUID createCategory(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/categories").with(csrf()).contentType("application/json")
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("id").asText());
    }

    private String productBody(String sku, String name, UUID categoryId, String price) {
        return "{\"sku\":\"" + sku + "\",\"name\":\"" + name + "\",\"description\":null,\"categoryId\":\""
                + categoryId + "\",\"price\":" + price + "}";
    }

    private void assertDatabaseConstraint(Runnable statement) {
        org.assertj.core.api.Assertions.assertThatThrownBy(statement::run)
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
