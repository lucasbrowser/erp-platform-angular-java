package com.erp.core.platform.contract;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {

    @Test
    void contractIsSemanticallyValidOpenApi31AndListsImplementedRoutes() {
        Path contract = Path.of("../../docs/api-contract.yaml").toAbsolutePath().normalize();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(contract.toString(), null, options);

        assertThat(result.getMessages()).as("OpenAPI validation messages").isEmpty();
        assertThat(result.getOpenAPI()).isNotNull();
        assertThat(result.getOpenAPI().getOpenapi()).startsWith("3.1.");
        assertThat(result.getOpenAPI().getPaths())
                .containsOnlyKeys(
                        "/actuator/health",
                        "/actuator/info",
                        "/api/v1/platform/me",
                        "/api/v1/categories",
                        "/api/v1/categories/{id}",
                        "/api/v1/products",
                        "/api/v1/products/{id}",
                        "/api/v2/products",
                        "/api/v2/products/{id}");

        Map<String, Set<HttpMethod>> methods = Map.of(
                "/actuator/health", Set.of(HttpMethod.GET),
                "/actuator/info", Set.of(HttpMethod.GET),
                "/api/v1/platform/me", Set.of(HttpMethod.GET),
                "/api/v1/categories", Set.of(HttpMethod.GET, HttpMethod.POST),
                "/api/v1/categories/{id}", Set.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE),
                "/api/v1/products", Set.of(HttpMethod.GET, HttpMethod.POST),
                "/api/v1/products/{id}", Set.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE),
                "/api/v2/products", Set.of(HttpMethod.GET, HttpMethod.POST),
                "/api/v2/products/{id}", Set.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));
        methods.forEach((path, expected) -> assertThat(result.getOpenAPI().getPaths().get(path).readOperationsMap().keySet())
                .as("operations for " + path)
                .containsExactlyInAnyOrderElementsOf(expected));
        assertThat(schemaTypes(propertySchema(result.getOpenAPI(), "ProductV2Request", "price")))
                .containsExactly("string");
        assertThat(schemaTypes(propertySchema(result.getOpenAPI(), "ProductV2", "price")))
                .containsExactly("string");
        assertThat(schemaTypes(propertySchema(result.getOpenAPI(), "ProductRequest", "price")))
                .containsExactly("number");
        assertThat(schemaTypes(propertySchema(result.getOpenAPI(), "Product", "price")))
                .containsExactly("number");
        assertThat(propertySchema(result.getOpenAPI(), "ProductV2Request", "price").getPattern())
                .isEqualTo("^(0|[1-9][0-9]{0,11})\\.[0-9]{2}$");
        assertThat(propertySchema(result.getOpenAPI(), "ProductV2", "price").getPattern())
                .isEqualTo("^(0|[1-9][0-9]{0,11})\\.[0-9]{2}$");
    }

    private Schema<?> propertySchema(OpenAPI openAPI, String schemaName, String propertyName) {
        Object component = openAPI.getComponents().getSchemas().get(schemaName);
        if (!(component instanceof Schema<?> componentSchema)) {
            throw new AssertionError("Missing or invalid OpenAPI schema: " + schemaName);
        }

        Object property = componentSchema.getProperties().get(propertyName);
        if (!(property instanceof Schema<?> propertySchema)) {
            throw new AssertionError("Missing or invalid OpenAPI property: " + schemaName + "." + propertyName);
        }
        return propertySchema;
    }

    private Set<String> schemaTypes(Schema<?> schema) {
        if (schema.getTypes() != null) {
            return schema.getTypes();
        }
        return schema.getType() == null ? Set.of() : Set.of(schema.getType());
    }
}
