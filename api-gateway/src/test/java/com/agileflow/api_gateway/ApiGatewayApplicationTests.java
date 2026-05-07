package com.agileflow.api_gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {

    private static final String JWT_SECRET = "G10_SECRET_KEY_SGITU_2025_SUPER_SECURE_KEY_32CHARS";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void gatewayErrorsAreStructuredForMissingInvalidAndUnknownRoutes() {
        webTestClient.get()
                .uri("/api/payments/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.correlationId").exists();

        webTestClient.get()
                .uri("/api/payments/1")
                .headers(headers -> headers.setBearerAuth("invalid-token"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_TOKEN")
                .jsonPath("$.correlationId").exists();

        webTestClient.get()
                .uri("/api/unknown/test")
                .headers(headers -> headers.setBearerAuth(jwtWithRolesClaim(
                        "admin.g3@sgitu.ma",
                        List.of("ROLE_ADMIN"),
                        "1"
                )))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ROUTE_NOT_FOUND")
                .jsonPath("$.correlationId").exists();
    }

    @Test
    void adminRoutesRequireRoleAdminFromJwtClaims() {
        webTestClient.get()
                .uri("/api/users/1/roles")
                .headers(headers -> headers.setBearerAuth(jwt("user@sgitu.ma", "ROLE_USER", "10")))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.get()
                .uri("/api/v1/admin/dashboard")
                .headers(headers -> headers.setBearerAuth(jwt("user@sgitu.ma", "ROLE_USER", "10")))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.get()
                .uri("/api/v1/ticket-types")
                .headers(headers -> headers.setBearerAuth(jwt("user@sgitu.ma", "ROLE_USER", "10")))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.get()
                .uri("/api/users/1/roles")
                .headers(headers -> headers.setBearerAuth(jwtWithRolesClaim(
                        "user@sgitu.ma",
                        List.of("ROLE_USER"),
                        "10"
                )))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void g2AdminRoutesRequireRoleAdminFromJwtClaims() {
        webTestClient.post()
                .uri("/api/abonnements/admin/1/suspendre?motif=test")
                .headers(headers -> headers.setBearerAuth(jwt("user@sgitu.ma", "ROLE_USER", "10")))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void g8AnalyticsAndPredictionRoutesRequireRoleAdminOrAgent() {
        webTestClient.get()
                .uri("/api/v1/analytics/dashboard")
                .headers(headers -> headers.setBearerAuth(jwt("user@sgitu.ma", "ROLE_USER", "10")))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.post()
                .uri("/predict/peak-hours")
                .headers(headers -> headers.setBearerAuth(jwt("user@sgitu.ma", "ROLE_USER", "10")))
                .bodyValue(Map.of("horizonHours", 24))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void g3AuthRouteOwnsAuthPrefixAndRewritesToUserServiceApiContext() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g3-auth".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).anySatisfy(filter ->
                assertThat(filter.getName()).isEqualTo("RewritePath"));
        assertThat(route.getPredicates()).anySatisfy(predicate ->
                assertThat(predicate.getArgs().values())
                        .contains("/auth/**"));
    }

    @Test
    void g3UsersRouteOwnsUsersAndProfilesPrefixesWithoutRewrite() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g3-utilisateurs".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).isEmpty();
        assertThat(route.getPredicates()).anySatisfy(predicate ->
                assertThat(predicate.getArgs().values())
                        .contains("/api/users", "/api/users/**", "/api/profiles", "/api/profiles/**"));
    }

    @Test
    void g5RouteKeepsApiNotificationsPrefixWithoutRewrite() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g5-notifications".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).isEmpty();
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .anySatisfy(value -> assertThat(value)
                            .contains("/api/notifications")
                            .doesNotContain("/api/notify"));
        });
    }

    @Test
    void g1RouteUsesApiV1BilletterieContractPrefixWithoutLegacyAlias() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g1-billetterie".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).isEmpty();
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .contains(
                            "/api/v1/tickets",
                            "/api/v1/tickets/**",
                            "/api/v1/admin/tickets",
                            "/api/v1/admin/tickets/**",
                            "/api/v1/admin/dashboard",
                            "/api/v1/ticket-types",
                            "/api/v1/ticket-types/**"
                    )
                    .noneMatch(value -> value.equals("/api/tickets") || value.equals("/api/tickets/**"));
        });
    }

    @Test
    void g2RouteUsesAbonnementContractPrefixesAndRewritesApiPrefix() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g2-abonnements".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).anySatisfy(filter ->
                assertThat(filter.getName()).isEqualTo("RewritePath"));
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .contains(
                            "/api/abonnements",
                            "/api/abonnements/**",
                            "/api/plans",
                            "/api/plans/**"
                    );
        });
    }

    @Test
    void g4RouteUsesApiG4ContractPrefixWithoutLegacyAliases() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g4-coordination".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).isEmpty();
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .contains(
                            "/api/g4",
                            "/api/g4/**",
                            "/api/v1/operator/status"
                    )
                    .noneMatch(value -> value.equals("/api/coordination/**")
                            || value.equals("/api/routes/**")
                            || value.equals("/api/schedules/**"));
        });
    }

    @Test
    void g6RouteUsesPaymentContractPrefixesAndRewritesApiPrefix() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g6-paiement".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).anySatisfy(filter ->
                assertThat(filter.getName()).isEqualTo("RewritePath"));
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .contains(
                            "/api/payments",
                            "/api/payments/**",
                            "/api/refunds",
                            "/api/refunds/**",
                            "/api/payment-accounts",
                            "/api/payment-accounts/**",
                            "/api/invoices",
                            "/api/invoices/**",
                            "/api/test-cards",
                            "/api/test-mobile-money-accounts",
                            "/api/health"
                    )
                    .noneMatch(value -> value.equals("/api/paiement") || value.equals("/api/paiement/**"));
        });
    }

    @Test
    void g7RouteUsesSuiviVehiculesContractPrefixWithoutLegacyAliases() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g7-suivi-vehicules".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).isEmpty();
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .anySatisfy(value -> assertThat(value)
                            .contains("/api/suivi-vehicules")
                            .doesNotContain("/api/vehicles")
                            .doesNotContain("/api/vehicules"));
        });
    }

    @Test
    void g8RouteUsesApiV1AnalyticsContractPrefixesWithoutRewrite() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g8-analytics".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).isEmpty();
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .contains(
                            "/api/v1/ingestion/**",
                            "/api/v1/analytics/**",
                            "/predict/peak-hours",
                            "/predict/incidents"
                    )
                    .noneMatch(value -> value.equals("/api/analytics/**")
                            || value.equals("/api/reports/**"));
        });
    }

    @Test
    void g9RouteUsesIncidentContractPrefixesAndRewritesApiPrefix() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g9-incidents".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getFilters()).anySatisfy(filter ->
                assertThat(filter.getName()).isEqualTo("RewritePath"));
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .contains(
                            "/api/incidents/**",
                            "/api/rapports/**"
                    );
        });
    }

    private String jwt(String email, String role, String userId) {
        return buildJwt(email, Map.of(
                "role", role,
                "userId", userId,
                "jti", UUID.randomUUID().toString()
        ));
    }

    private String jwtWithRolesClaim(String email, List<String> roles, String userId) {
        return buildJwt(email, Map.of(
                "roles", roles,
                "userId", userId,
                "jti", UUID.randomUUID().toString()
        ));
    }

    private String buildJwt(String email, Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
}
