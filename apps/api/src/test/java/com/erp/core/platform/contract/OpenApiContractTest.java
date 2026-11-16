package com.erp.core.platform.contract;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.PathItem.HttpMethod;
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
        assertThat(result.getOpenAPI().getComponents().getSchemas().get("ProductV2Request")
                .getProperties().get("price").getType()).isEqualTo("string");
        assertThat(result.getOpenAPI().getComponents().getSchemas().get("ProductV2")
                .getProperties().get("price").getType()).isEqualTo("string");
        assertThat(result.getOpenAPI().getComponents().getSchemas().get("ProductRequest")
                .getProperties().get("price").getType()).isEqualTo("number");
    }
}
