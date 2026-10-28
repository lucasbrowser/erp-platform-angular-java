package com.erp.core.platform.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigurationTest.TestSecuritySupport.class)
class SecurityConfigurationTest {

    private static final String ISSUER = "http://localhost:8081/realms/erp-core";
    private static final RSAKey KEY = generateKey();
    private static final HttpServer JWKS_SERVER = startJwksServer();

    @Autowired MockMvc mockMvc;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @DynamicPropertySource
    static void jwksUri(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + JWKS_SERVER.getAddress().getPort() + "/jwks");
    }

    @AfterAll
    static void stopJwksServer() {
        JWKS_SERVER.stop(0);
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/security/manager")).andExpect(status().isUnauthorized());
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/security/manager").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongIssuerReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/security/manager").header("Authorization", "Bearer " + bearer("MANAGER", "https://wrong-issuer")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongAudienceReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/security/manager").header("Authorization", "Bearer " + bearer("MANAGER", ISSUER, "another-api")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        Instant expiredAt = Instant.now().minusSeconds(60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("test-user")
                .issuedAt(expiredAt.minusSeconds(60))
                .expiresAt(expiredAt)
                .audience(List.of("erp-core-api"))
                .claim("realm_access", Map.of("roles", List.of("MANAGER")))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        mockMvc.perform(get("/api/v1/security/manager").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenWithRequiredPermissionIsAccepted() throws Exception {
        String token = bearer("MANAGER", ISSUER, "erp-core-api");
        assertThat(jwtDecoder.decode(token).getAudience()).contains("erp-core-api");
        mockMvc.perform(get("/api/v1/security/manager").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedUserWithoutRequiredPermissionGets403() throws Exception {
        mockMvc.perform(get("/api/v1/security/manager").header("Authorization", "Bearer " + bearer("SALES", ISSUER, "erp-core-api")))
                .andExpect(status().isForbidden());
    }

    @Test
    void mapsOnlyKnownRealmRolesToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("test")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "FINANCE", "unknown")))
                .build();

        assertThat(jwtAuthenticationConverter.convert(jwt).getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_FINANCE");
    }

    private String bearer(String role, String issuer) {
        return bearer(role, issuer, "erp-core-api");
    }

    private String bearer(String role, String issuer, String audience) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("test-user")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .audience(List.of(audience))
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @TestConfiguration
    static class TestSecuritySupport {
        @Bean
        JwtEncoder testJwtEncoder() {
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(KEY)));
        }

        @Bean
        TestEndpoints testEndpoints() {
            return new TestEndpoints();
        }
    }

    private static RSAKey generateKey() {
        try {
            java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            java.security.KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                    .privateKey((java.security.interfaces.RSAPrivateKey) keyPair.getPrivate())
                    .keyID("security-test-key").build();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer startJwksServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            server.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var response = exchange.getResponseBody()) {
                    response.write(body);
                }
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @RestController
    static class TestEndpoints {
        @GetMapping(value = "/api/v1/security/manager", produces = MediaType.TEXT_PLAIN_VALUE)
        @PreAuthorize("hasRole('MANAGER')")
        String managerOperation() {
            return "authorized";
        }
    }
}
