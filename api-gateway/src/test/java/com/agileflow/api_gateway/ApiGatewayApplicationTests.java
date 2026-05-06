package com.agileflow.api_gateway;

import com.agileflow.api_gateway.dto.AuthResponse;
import com.agileflow.api_gateway.model.User;
import com.agileflow.api_gateway.repository.UserRepository;
import com.agileflow.api_gateway.service.NotificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CapturingNotificationClient notificationClient;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @BeforeEach
    void clearNotifications() {
        notificationClient.clear();
    }

    @Test
    void bootstrapAdminCanLoginAndAccessAdminEndpoints() {
        User admin = userRepository.findByEmail("admin@sgitu.ma").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(User.RoleType.ROLE_ADMIN);
        assertThat(admin.isEnabled()).isTrue();
        assertThat(admin.isEmailVerified()).isTrue();

        String adminToken = login("admin@sgitu.ma", "Admin123456").getAccessToken();

        webTestClient.get()
                .uri("/admin/users")
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].email").exists();
    }

    @Test
    void registrationRequiresEmailVerificationAndSupportsRefreshAndLogout() {
        String email = "register-flow@sgitu.ma";

        webTestClient.post()
                .uri("/auth/register")
                .bodyValue(Map.of(
                        "email", email,
                        "password", "User123456",
                        "role", "ROLE_USER"
                ))
                .exchange()
                .expectStatus().isOk();

        String verificationToken = notificationClient.verificationToken(email);
        assertThat(verificationToken).isNotBlank();

        webTestClient.post()
                .uri("/auth/login")
                .bodyValue(Map.of("email", email, "password", "User123456"))
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/auth/verify-email")
                        .queryParam("token", verificationToken)
                        .build())
                .exchange()
                .expectStatus().isOk();

        AuthResponse login = login(email, "User123456");
        assertThat(login.getAccessToken()).isNotBlank();
        assertThat(login.getRefreshToken()).isNotBlank();

        webTestClient.post()
                .uri("/auth/refresh")
                .bodyValue(Map.of("refreshToken", login.getRefreshToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(response -> assertThat(response.getAccessToken()).isNotBlank());

        webTestClient.post()
                .uri("/auth/logout")
                .headers(headers -> headers.setBearerAuth(login.getAccessToken()))
                .bodyValue(Map.of("refreshToken", login.getRefreshToken()))
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/auth/refresh")
                .bodyValue(Map.of("refreshToken", login.getRefreshToken()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void publicRegisterCannotCreateAdminAccount() {
        webTestClient.post()
                .uri("/auth/register")
                .bodyValue(Map.of(
                        "email", "public-admin@sgitu.ma",
                        "password", "User123456",
                        "role", "ROLE_ADMIN"
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void forgotPasswordSendsResetTokenAndAllowsPasswordReset() {
        String email = "reset-flow@sgitu.ma";
        createVerifiedUser(email, "OldPass123", User.RoleType.ROLE_USER);

        webTestClient.post()
                .uri("/auth/forgot-password")
                .bodyValue(Map.of("email", email))
                .exchange()
                .expectStatus().isOk();

        String resetToken = notificationClient.passwordResetToken(email);
        assertThat(resetToken).isNotBlank();

        webTestClient.post()
                .uri("/auth/reset-password")
                .bodyValue(Map.of(
                        "token", resetToken,
                        "newPassword", "NewPass123"
                ))
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/auth/login")
                .bodyValue(Map.of("email", email, "password", "OldPass123"))
                .exchange()
                .expectStatus().isUnauthorized();

        AuthResponse login = login(email, "NewPass123");
        assertThat(login.getAccessToken()).isNotBlank();
    }

    @Test
    void adminEndpointsRequireRoleAdminAndCanUpdateAccount() {
        User user = createVerifiedUser("admin-target@sgitu.ma", "Target123", User.RoleType.ROLE_USER);
        String userToken = login(user.getEmail(), "Target123").getAccessToken();

        webTestClient.get()
                .uri("/admin/users")
                .headers(headers -> headers.setBearerAuth(userToken))
                .exchange()
                .expectStatus().isForbidden();

        String adminToken = login("admin@sgitu.ma", "Admin123456").getAccessToken();

        webTestClient.put()
                .uri("/admin/users/{id}/role", user.getId())
                .headers(headers -> headers.setBearerAuth(adminToken))
                .bodyValue(Map.of("role", "ROLE_OPERATOR"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.role").isEqualTo("ROLE_OPERATOR");

        webTestClient.put()
                .uri("/admin/users/{id}/email-verification", user.getId())
                .headers(headers -> headers.setBearerAuth(adminToken))
                .bodyValue(Map.of("emailVerified", false))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.emailVerified").isEqualTo(false)
                .jsonPath("$.enabled").isEqualTo(false);

        webTestClient.post()
                .uri("/auth/login")
                .bodyValue(Map.of("email", user.getEmail(), "password", "Target123"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void g1AdminRoutesRequireRoleAdmin() {
        User user = createVerifiedUser("g1-admin-forbidden@sgitu.ma", "Target123", User.RoleType.ROLE_USER);
        String userToken = login(user.getEmail(), "Target123").getAccessToken();

        webTestClient.get()
                .uri("/api/v1/admin/dashboard")
                .headers(headers -> headers.setBearerAuth(userToken))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.get()
                .uri("/api/v1/ticket-types")
                .headers(headers -> headers.setBearerAuth(userToken))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void g2AdminRoutesRequireRoleAdmin() {
        User user = createVerifiedUser("g2-admin-forbidden@sgitu.ma", "Target123", User.RoleType.ROLE_USER);
        String userToken = login(user.getEmail(), "Target123").getAccessToken();

        webTestClient.post()
                .uri("/api/abonnements/admin/1/suspendre?motif=test")
                .headers(headers -> headers.setBearerAuth(userToken))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void gatewayErrorsAreStructured() {
        webTestClient.get()
                .uri("/api/payments/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.correlationId").exists();

        String adminToken = login("admin@sgitu.ma", "Admin123456").getAccessToken();

        webTestClient.get()
                .uri("/api/unknown/test")
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ROUTE_NOT_FOUND")
                .jsonPath("$.correlationId").exists();
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
    void g8AnalyticsAndPredictionRoutesRequireRoleAdminOrAgent() {
        User user = createVerifiedUser("g8-analytics-forbidden@sgitu.ma", "Target123", User.RoleType.ROLE_USER);
        String userToken = login(user.getEmail(), "Target123").getAccessToken();

        webTestClient.get()
                .uri("/api/v1/analytics/dashboard")
                .headers(headers -> headers.setBearerAuth(userToken))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.post()
                .uri("/predict/peak-hours")
                .headers(headers -> headers.setBearerAuth(userToken))
                .bodyValue(Map.of("horizonHours", 24))
                .exchange()
                .expectStatus().isForbidden();
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

    private AuthResponse login(String email, String password) {
        return webTestClient.post()
                .uri("/auth/login")
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private User createVerifiedUser(String email, String password, User.RoleType role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    @TestConfiguration
    static class TestNotificationsConfig {

        @Bean
        @Primary
        CapturingNotificationClient capturingNotificationClient() {
            return new CapturingNotificationClient();
        }
    }

    static class CapturingNotificationClient extends NotificationClient {

        private final Map<String, String> verificationTokens = new ConcurrentHashMap<>();
        private final Map<String, String> passwordResetTokens = new ConcurrentHashMap<>();

        CapturingNotificationClient() {
            super(WebClient.builder());
        }

        @Override
        public void sendVerificationEmail(Long userId, String email, String rawToken) {
            verificationTokens.put(email, rawToken);
        }

        @Override
        public void sendPasswordResetEmail(Long userId, String email, String rawToken) {
            passwordResetTokens.put(email, rawToken);
        }

        void clear() {
            verificationTokens.clear();
            passwordResetTokens.clear();
        }

        String verificationToken(String email) {
            return verificationTokens.get(email);
        }

        String passwordResetToken(String email) {
            return passwordResetTokens.get(email);
        }
    }
}
