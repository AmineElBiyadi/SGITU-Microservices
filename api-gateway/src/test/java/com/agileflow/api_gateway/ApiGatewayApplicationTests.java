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

    private static final String JWT_SECRET = "SGITU_G3_JWT_SECRET_KEY_CHANGE_ME_IN_PRODUCTION_256BITS!!";

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
                .uri("/api/users")
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
    void g3RoleLookupRequiresSupervisorOrDispatcher() {
        webTestClient.get()
                .uri("/api/users/roles/ROLE_DRIVER")
                .headers(headers -> headers.setBearerAuth(jwt("operator@sgitu.ma", "ROLE_OPERATOR", "12")))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.get()
                .uri("/api/users/roles/ROLE_DRIVER")
                .headers(headers -> headers.setBearerAuth(jwt("supervisor@sgitu.ma", "ROLE_SUPERVISOR", "13")))
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotIn(401, 403, 404));
    }

    @Test
    void g3LogoutRequiresJwtBecauseItRevokesCurrentAccessToken() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
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
    void g8AnalyticsAndPredictionRoutesRequireRoleAdminOperatorOrStaff() {
        webTestClient.get()
                .uri("/api/v1/analytics/dashboard")
                .headers(headers -> headers.setBearerAuth(jwt("user@sgitu.ma", "ROLE_USER", "10")))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.post()
                .uri("/predict/peak-hours")
                .headers(headers -> headers.setBearerAuth(jwt("user@sgitu.ma", "ROLE_USER", "10")))
                .bodyValue(Map.of("data", List.of(
                        Map.of("hour", 8, "validationCount", 120),
                        Map.of("hour", 17, "validationCount", 180)
                )))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void userRegistrationThroughG3IsPublicAtApiUsers() {
        webTestClient.post()
                .uri("/api/users")
                .bodyValue(Map.of(
                        "email", "new.user@sgitu.ma",
                        "password", "Password123",
                        "role", "ROLE_PASSENGER"
                ))
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotIn(401, 403, 404));
    }

    @Test
    void swaggerDocumentsGatewaySecurityAndG8Contract() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.info.title").isEqualTo("SGITU - API Gateway G10")
                .jsonPath("$.components.securitySchemes.bearerAuth.type").isEqualTo("http")
                .jsonPath("$.components.securitySchemes.bearerAuth.scheme").isEqualTo("bearer")
                .jsonPath("$.paths['/auth/refresh'].post.summary").isEqualTo("Refresh token route vers G3")
                .jsonPath("$.paths['/auth/logout'].post.summary").isEqualTo("Logout route vers G3")
                .jsonPath("$.paths['/api/users'].post.summary").isEqualTo("Create user route vers G3")
                .jsonPath("$.paths['/api/users'].get.summary").isEqualTo("List users")
                .jsonPath("$.paths['/api/users/roles/{roleName}'].get.summary").isEqualTo("Get users by role")
                .jsonPath("$.paths['/api/users/drivers/ids'].get.summary").isEqualTo("Get driver ids")
                .jsonPath("$.paths['/api/users/{id}'].get.summary").isEqualTo("Get user by id")
                .jsonPath("$.paths['/api/users/{id}'].put.summary").isEqualTo("Update user profile")
                .jsonPath("$.paths['/api/users/{id}'].delete.summary").isEqualTo("Delete user")
                .jsonPath("$.paths['/api/users/{id}/roles'].put.summary").isEqualTo("Update user roles")
                .jsonPath("$.paths['/api/users/{id}/activate'].put.summary").isEqualTo("Activate user")
                .jsonPath("$.paths['/api/users/{id}/deactivate'].put.summary").isEqualTo("Deactivate user")
                .jsonPath("$.paths['/api/v1/ingestion/payments'].post.summary").isEqualTo("Ingest payments")
                .jsonPath("$.paths['/api/v1/analytics/dashboard'].get.summary").isEqualTo("Get complete analytics dashboard")
                .jsonPath("$.paths['/api/v1/analytics/reports/generate'].post.summary").isEqualTo("Generate analytics report")
                .jsonPath("$.paths['/predict/peak-hours'].post.summary").isEqualTo("Predict peak hours")
                .jsonPath("$.paths['/predict/incidents'].post.summary").isEqualTo("Predict incident risk zones");
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
    void g8AnalyticsRouteUsesApiV1ContractPrefixesWithoutRewrite() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g8-analytics".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getUri().toString()).contains("g8-analytics:8088");
        assertThat(route.getFilters()).isEmpty();
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .contains(
                            "/api/v1/ingestion/**",
                            "/api/v1/analytics/**"
                    )
                    .noneMatch(value -> value.equals("/api/analytics/**")
                            || value.equals("/api/reports/**")
                            || value.equals("/predict/peak-hours")
                            || value.equals("/predict/incidents"));
        });
    }

    @Test
    void g8MlRouteUsesPredictionPrefixesWithoutRewrite() {
        var route = routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> "g8-ml-predictions".equals(routeDefinition.getId()))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getUri().toString()).contains("ml-service:5000");
        assertThat(route.getFilters()).isEmpty();
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getArgs().values())
                    .contains(
                            "/predict/peak-hours",
                            "/predict/incidents"
                    )
                    .noneMatch(value -> value.equals("/api/v1/ingestion/**")
                            || value.equals("/api/v1/analytics/**"));
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
