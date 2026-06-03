package com.sgitu.apigateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SwaggerConfig {

    private static final String APPLICATION_JSON = "application/json";
    private static final String BEARER_AUTH = "bearerAuth";
    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .tags(apiTags())
                .components(apiComponents())
                .paths(apiPaths());
    }

    private Info apiInfo() {
        return new Info()
                .title("SGITU - API Gateway G10")
                .version("1.0.0")
                .description("""
                        Documentation OpenAPI du microservice **G10 API Gateway & Securite**.

                        Role de G10 :
                        - point d'entree unique du systeme SGITU ;
                        - validation des JWT emis par G3 Gestion des utilisateurs ;
                        - controle RBAC par roles Spring Security ;
                        - routage vers les microservices G1 a G9 ;
                        - propagation des headers `X-User-Id`, `X-User-Email`, `X-Roles`
                          et `X-Correlation-Id`.

                        G10 ne genere pas les JWT et ne possede pas la base officielle des
                        utilisateurs. G3 reste la source de verite pour les comptes, roles,
                        permissions et informations d'authentification.

                        Cette specification documente surtout les routes Gateway testables en
                        demonstration, notamment l'integration G10 -> G8 Analytics / ML.
                        """)
                .contact(new Contact()
                        .name("Groupe 10 - API Gateway & Securite"));
    }

    private List<Server> apiServers() {
        return List.of(
                new Server()
                        .url("/")
                        .description("Serveur courant - recommande pour Swagger UI"),
                new Server()
                        .url("http://localhost:8080")
                        .description("Developpement local - Postman / navigateur")
        );
    }

    private List<Tag> apiTags() {
        return List.of(
                new Tag()
                        .name("G10 - Public")
                        .description("Endpoints publics exposes par la Gateway."),
                new Tag()
                        .name("G3 - Auth routee")
                        .description("Authentification deleguee au service G3 Utilisateurs."),
                new Tag()
                        .name("G3 - Utilisateurs via Gateway")
                        .description("Gestion des comptes, profils et roles G3 routee par G10."),
                new Tag()
                        .name("G3 - Administration via Gateway")
                        .description("Operations admin G3 protegees par ROLE_ADMIN au niveau Gateway."),
                new Tag()
                        .name("G1 - Billetterie via Gateway")
                        .description("Tickets dematerialises G1 routes par G10."),
                new Tag()
                        .name("G1 - Administration via Gateway")
                        .description("Operations administratives G1 protegees par ROLE_ADMIN."),
                new Tag()
                        .name("G2 - Abonnements via Gateway")
                        .description("Plans et abonnements G2 routes par G10."),
                new Tag()
                        .name("G2 - Administration via Gateway")
                        .description("Operations G2 protegees par ROLE_ADMIN_G2."),
                new Tag()
                        .name("G4 - Coordination via Gateway")
                        .description("Lignes, missions et supervision G4 routees par G10 avec RBAC dedie."),
                new Tag()
                        .name("G5 - Notifications via Gateway")
                        .description("Envoi, consultation et administration des notifications G5 routees par G10."),
                new Tag()
                        .name("G6 - Paiement via Gateway")
                        .description("Paiements, moyens de paiement, factures et remboursements G6 routes par G10."),
                new Tag()
                        .name("G7 - Suivi vehicules via Gateway")
                        .description("Vehicules, positions GPS et alertes G7 routes par G10 avec RBAC dedie."),
                new Tag()
                        .name("G8 - Ingestion via Gateway")
                        .description("Reception d'evenements metier destines aux calculs analytiques G8."),
                new Tag()
                        .name("G8 - Analytics via Gateway")
                        .description("Consultation des statistiques et tableaux de bord G8."),
                new Tag()
                        .name("G8 - Reports via Gateway")
                        .description("Generation et consultation des rapports G8."),
                new Tag()
                        .name("G8 - ML via Gateway")
                        .description("Predictions ML G8 routees par la Gateway."),
                new Tag()
                        .name("G10 - Erreurs")
                        .description("Format commun des erreurs Gateway : 401, 403, 404, 503 et 500.")
        );
    }

    private Components apiComponents() {
        return new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                                Access token JWT emis par G3.
                                Claims attendus par G10 : `sub`, `userId` ou `id`,
                                `roles` ou `role`, `exp`.
                                """))
                .addSchemas("ApiError", apiErrorSchema())
                .addSchemas("HealthResponse", healthSchema())
                .addSchemas("LoginRequest", loginRequestSchema())
                .addSchemas("RefreshRequest", refreshRequestSchema())
                .addSchemas("UserRegistrationRequest", userRegistrationRequestSchema())
                .addSchemas("UserUpdateRequest", userUpdateRequestSchema())
                .addSchemas("PasswordChangeRequest", passwordChangeRequestSchema())
                .addSchemas("RoleUpdateRequest", roleUpdateRequestSchema())
                .addSchemas("UserExistsResponse", userExistsResponseSchema())
                .addSchemas("UserResponse", userResponseSchema())
                .addSchemas("Profile", profileSchema())
                .addSchemas("LoginResponse", loginResponseSchema())
                .addSchemas("PlanAbonnement", planAbonnementSchema())
                .addSchemas("G4Ligne", g4LigneSchema())
                .addSchemas("G4Mission", g4MissionSchema())
                .addSchemas("G5NotificationRequest", g5NotificationRequestSchema())
                .addSchemas("G5NotificationResponse", g5NotificationResponseSchema())
                .addSchemas("G7Vehicule", g7VehiculeSchema())
                .addSchemas("G7Position", g7PositionSchema())
                .addSchemas("BatchIngestionResponse", batchIngestionResponseSchema())
                .addSchemas("IncomingEvent", incomingEventSchema())
                .addSchemas("ReportRequest", reportRequestSchema())
                .addSchemas("Report", reportSchema())
                .addSchemas("StatSnapshot", statSnapshotSchema())
                .addSchemas("PeakHoursPredictionRequest", peakHoursPredictionRequestSchema())
                .addSchemas("PeakHoursPredictionResponse", peakHoursPredictionResponseSchema())
                .addSchemas("IncidentPredictionRequest", incidentPredictionRequestSchema())
                .addSchemas("IncidentPredictionResponse", incidentPredictionResponseSchema());
    }

    private Paths apiPaths() {
        return new Paths()
                .addPathItem("/actuator/health", healthPath())
                .addPathItem("/auth/login", loginPath())
                .addPathItem("/auth/refresh", refreshPath())
                .addPathItem("/auth/logout", logoutPath())
                .addPathItem("/api/users", registerPath())
                .addPathItem("/api/users/roles/{roleName}", usersByRolePath())
                .addPathItem("/api/users/drivers/ids", driverIdsPath())
                .addPathItem("/api/users/notification-recipients", notificationRecipientsPath())
                .addPathItem("/api/users/{id}", userByIdPath())
                .addPathItem("/api/users/{id}/exists", userExistsPath())
                .addPathItem("/api/users/{id}/password", changePasswordPath())
                .addPathItem("/api/users/{id}/roles", updateRolesPath())
                .addPathItem("/api/users/{id}/deactivate", userStatusPath(
                        "deactivate",
                        "Deactivate user",
                        "Desactive un compte utilisateur. Acces : ROLE_ADMIN.",
                        false))
                .addPathItem("/api/users/{id}/activate", userStatusPath(
                        "activate",
                        "Activate user",
                        "Reactive un compte utilisateur. Acces : ROLE_ADMIN.",
                        true))
                .addPathItem("/api/v1/tickets", g1TicketsPath())
                .addPathItem("/api/v1/tickets/{ticketId}", g1TicketByIdPath())
                .addPathItem("/api/v1/tickets/user/{userId}", g1TicketsByUserPath())
                .addPathItem("/api/v1/tickets/{ticketId}/pay", g1TicketActionPath(
                        "payTicketViaGateway",
                        "Pay ticket",
                        "Declenche le paiement d'un ticket G1. Acces : JWT valide."))
                .addPathItem("/api/v1/tickets/{ticketId}/validate", g1TicketActionPath(
                        "validateTicketViaGateway",
                        "Validate ticket",
                        "Valide ou consomme un ticket G1. Acces : JWT valide."))
                .addPathItem("/api/v1/tickets/{ticketId}/transfer", g1TicketActionPath(
                        "transferTicketViaGateway",
                        "Transfer ticket",
                        "Initie le transfert d'un ticket G1. Acces : JWT valide."))
                .addPathItem("/api/v1/tickets/{ticketId}/cancel", g1TicketNoBodyActionPath(
                        "cancelTicketViaGateway",
                        "Cancel ticket",
                        "Annule un ticket G1. Acces : JWT valide."))
                .addPathItem("/api/v1/tickets/{ticketId}/refund", g1TicketNoBodyActionPath(
                        "refundTicketViaGateway",
                        "Refund ticket",
                        "Demande le remboursement d'un ticket G1. Acces : JWT valide."))
                .addPathItem("/api/v1/tickets/{ticketId}/transfer/accept", g1TransferDecisionPath(
                        "acceptTransferViaGateway",
                        "Accept ticket transfer",
                        "Accepte un transfert de ticket G1 en attente. Acces : JWT valide."))
                .addPathItem("/api/v1/tickets/{ticketId}/transfer/reject", g1TransferDecisionPath(
                        "rejectTransferViaGateway",
                        "Reject ticket transfer",
                        "Refuse un transfert de ticket G1 en attente. Acces : JWT valide."))
                .addPathItem("/api/v1/tickets/{ticketId}/transfer/cancel", g1TicketNoBodyActionPath(
                        "cancelTransferViaGateway",
                        "Cancel ticket transfer",
                        "Annule un transfert de ticket G1 en attente. Acces : JWT valide."))
                .addPathItem("/api/v1/admin/dashboard", g1AdminSimpleGetPath(
                        "g1AdminDashboardViaGateway",
                        "G1 admin dashboard",
                        "Retourne les indicateurs administratifs de billetterie. Acces : ROLE_ADMIN."))
                .addPathItem("/api/v1/admin/tickets", g1AdminSimpleGetPath(
                        "g1AdminTicketsViaGateway",
                        "List G1 tickets",
                        "Liste les tickets G1 pour le back-office. Acces : ROLE_ADMIN."))
                .addPathItem("/api/v1/admin/tickets/flagged", g1AdminSimpleGetPath(
                        "g1AdminFlaggedTicketsViaGateway",
                        "List flagged tickets",
                        "Liste les tickets signales G1. Acces : ROLE_ADMIN."))
                .addPathItem("/api/v1/admin/tickets/{ticketId}/audit", g1AdminTicketGetPath(
                        "g1AdminTicketAuditViaGateway",
                        "Get ticket audit",
                        "Retourne l'audit d'un ticket G1. Acces : ROLE_ADMIN."))
                .addPathItem("/api/v1/admin/tickets/{ticketId}/flagged", g1AdminTicketGetPath(
                        "g1AdminFlaggedTicketDetailViaGateway",
                        "Get flagged ticket detail",
                        "Retourne le detail d'un ticket signale G1. Acces : ROLE_ADMIN."))
                .addPathItem("/api/v1/admin/tickets/{ticketId}/flag/resolve", g1AdminTicketBodyPath(
                        "g1AdminResolveFlagViaGateway",
                        "Resolve ticket flag",
                        "Traite et resout un signalement de ticket G1. Acces : ROLE_ADMIN.",
                        "PUT",
                        map("resolutionNote", "Signalement revu via G10", "resolvedBy", "1")))
                .addPathItem("/api/v1/admin/tickets/{ticketId}/flag/confirmfraud", g1AdminTicketBodyPath(
                        "g1AdminConfirmFraudViaGateway",
                        "Confirm ticket fraud",
                        "Confirme une fraude sur un ticket G1. Acces : ROLE_ADMIN.",
                        "PUT",
                        map("fraudReason", "Fraude confirmee via G10", "confirmedBy", "1", "blacklistHolder", false)))
                .addPathItem("/api/v1/admin/tickets/{ticketId}/forcerefund", g1AdminTicketBodyPath(
                        "g1AdminForceRefundViaGateway",
                        "Force ticket refund",
                        "Force le remboursement administratif d'un ticket G1. Acces : ROLE_ADMIN.",
                        "POST",
                        map("reason", "Remboursement force via G10", "refundedBy", "1")))
                .addPathItem("/api/plans", plansPath())
                .addPathItem("/api/plans/{id}", planByIdPath())
                .addPathItem("/api/abonnements/souscrire", subscribePath())
                .addPathItem("/api/abonnements/{id}", subscriptionByIdPath())
                .addPathItem("/api/abonnements/utilisateur/{userId}", subscriptionsByUserPath())
                .addPathItem("/api/abonnements/paiement/confirmation", paymentCallbackPath())
                .addPathItem("/api/abonnements/remboursement/confirmation", refundCallbackPath())
                .addPathItem("/api/abonnements/admin/{id}/suspendre", adminSuspendSubscriptionPath())
                .addPathItem("/api/g4/health", g4HealthPath())
                .addPathItem("/api/g4/logs", g4LogsPath())
                .addPathItem("/api/g4/lignes", g4LignesPath())
                .addPathItem("/api/g4/lignes/{id}", g4LigneByIdPath())
                .addPathItem("/api/g4/missions", g4MissionsPath())
                .addPathItem("/api/g4/missions/{id}/status", g4MissionStatusPath())
                .addPathItem("/api/v1/operator/status", g4OperatorStatusPath())
                .addPathItem("/api/notifications/health", g5HealthPath())
                .addPathItem("/api/notifications/send", g5SendNotificationPath())
                .addPathItem("/api/notifications", g5NotificationsPath())
                .addPathItem("/api/notifications/{notificationId}", g5NotificationByIdPath())
                .addPathItem("/api/notifications/{notificationId}/retry", g5RetryNotificationPath())
                .addPathItem("/api/notifications/admin/stats", g5AdminStatsPath())
                .addPathItem("/api/health", g6HealthPath())
                .addPathItem("/api/test-cards", g6TestCardsPath())
                .addPathItem("/api/test-mobile-money-accounts", g6TestMobileMoneyPath())
                .addPathItem("/api/payment-accounts/user/{userId}", g6PaymentAccountsByUserPath())
                .addPathItem("/api/payment-accounts/id/{id}", g6PaymentAccountByIdPath())
                .addPathItem("/api/payment-accounts/card", g6AddCardPath())
                .addPathItem("/api/payments", g6PaymentsPath())
                .addPathItem("/api/payments/{paymentId}", g6PaymentByIdPath())
                .addPathItem("/api/payments/{paymentId}/invoice", g6InvoiceByPaymentPath())
                .addPathItem("/api/payments/{paymentId}/refund", g6RefundPaymentPath())
                .addPathItem("/api/refunds/payment/{paymentId}", g6RefundsByPaymentPath())
                .addPathItem("/api/refunds/user/{userId}", g6RefundsByUserPath())
                .addPathItem("/api/suivi-vehicules/health", g7HealthPath())
                .addPathItem("/api/suivi-vehicules/vehicules", g7VehiculesPath())
                .addPathItem("/api/suivi-vehicules/vehicules/{id}", g7VehiculeByIdPath())
                .addPathItem("/api/suivi-vehicules/vehicules/{id}/statut", g7VehicleStatusPath())
                .addPathItem("/api/suivi-vehicules/positions", g7PositionsPath())
                .addPathItem("/api/suivi-vehicules/positions/{vehiculeId}", g7PositionByVehiclePath())
                .addPathItem("/api/suivi-vehicules/alerts/active", g7ActiveAlertsPath())
                .addPathItem("/api/suivi-vehicules/alerts/stats", g7AlertStatsPath())
                .addPathItem("/api/suivi-vehicules/alerts/{id}/cancel", g7CancelAlertPath())
                .addPathItem("/api/v1/ingestion/tickets", ingestionPath("tickets", "Ingest tickets"))
                .addPathItem("/api/v1/ingestion/incidents", ingestionPath("incidents", "Ingest incidents"))
                .addPathItem("/api/v1/ingestion/payments", ingestionPath("payments", "Ingest payments"))
                .addPathItem("/api/v1/ingestion/subscriptions", ingestionPath("subscriptions", "Ingest subscriptions"))
                .addPathItem("/api/v1/ingestion/vehicles", ingestionPath("vehicles", "Ingest vehicles"))
                .addPathItem("/api/v1/ingestion/users", ingestionPath("users", "Ingest users"))
                .addPathItem("/api/v1/analytics/dashboard", analyticsPath(
                        "dashboard",
                        "Get complete analytics dashboard",
                        "Retourne toutes les statistiques courantes calculees par G8."))
                .addPathItem("/api/v1/analytics/trips/summary", analyticsPath(
                        "tripsSummary",
                        "Get trips summary",
                        "Retourne les snapshots TRIPS : validations, heures de pointe, lignes et stations."))
                .addPathItem("/api/v1/analytics/revenue/summary", analyticsPath(
                        "revenueSummary",
                        "Get revenue summary",
                        "Retourne les snapshots REVENUE : revenus, methodes de paiement et tendances."))
                .addPathItem("/api/v1/analytics/incidents/stats", analyticsPath(
                        "incidentsStats",
                        "Get incidents statistics",
                        "Retourne les snapshots INCIDENTS : volume, severite, zones et temps de resolution."))
                .addPathItem("/api/v1/analytics/vehicles/activity", analyticsPath(
                        "vehiclesActivity",
                        "Get vehicles activity",
                        "Retourne les snapshots VEHICLES : activite, retards, vitesse et occupation."))
                .addPathItem("/api/v1/analytics/users/stats", analyticsPath(
                        "usersStats",
                        "Get users statistics",
                        "Retourne les snapshots USERS : activite et repartition plateforme."))
                .addPathItem("/api/v1/analytics/subscriptions/stats", analyticsPath(
                        "subscriptionsStats",
                        "Get subscriptions statistics",
                        "Retourne les snapshots SUBSCRIPTIONS : actifs, renouvellements, churn et types."))
                .addPathItem("/api/v1/analytics/reports/generate", generateReportPath())
                .addPathItem("/api/v1/analytics/reports/{id}", getReportByIdPath())
                .addPathItem("/predict/peak-hours", predictPeakHoursPath())
                .addPathItem("/predict/incidents", predictIncidentsPath());
    }

    private PathItem healthPath() {
        Operation operation = publicOperation(
                "G10 - Public",
                "getGatewayHealth",
                "Health check Gateway",
                "Verifie que la Gateway est demarree et accepte le trafic."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse(
                        "Gateway UP",
                        ref("HealthResponse"),
                        "health",
                        map("status", "UP"))));

        return new PathItem().get(operation);
    }

    private PathItem loginPath() {
        Operation operation = publicOperation(
                "G3 - Auth routee",
                "loginViaGateway",
                "Login route vers G3",
                """
                        La Gateway ne verifie pas le mot de passe.
                        Elle route `/auth/login` vers G3 User Management, qui authentifie
                        l'utilisateur et emet le JWT.
                        """
        ).requestBody(jsonRequest(
                "Identifiants utilisateur",
                ref("LoginRequest"),
                "loginRequest",
                map("email", "admin.g10@sgitu.ma", "password", "Password123")))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "JWT emis par G3",
                                ref("LoginResponse"),
                                "loginResponse",
                                map(
                                        "token", "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken", "550e8400-e29b-41d4-a716-446655440000",
                                        "userId", 1,
                                        "email", "admin.g10@sgitu.ma",
                                        "roles", List.of("ROLE_ADMIN"))))
                        .addApiResponse("401", errorResponse("Identifiants invalides", "UNAUTHORIZED", 401)));

        return new PathItem().post(operation);
    }

    private PathItem refreshPath() {
        Operation operation = publicOperation(
                "G3 - Auth routee",
                "refreshViaGateway",
                "Refresh token route vers G3",
                """
                        Echange un refresh token opaque stocke dans Redis cote G3 contre
                        un nouveau couple access token + refresh token. G3 effectue une
                        rotation du refresh token.
                        """
        ).requestBody(jsonRequest(
                "Refresh token emis par G3",
                ref("RefreshRequest"),
                "refreshRequest",
                map("refreshToken", "550e8400-e29b-41d4-a716-446655440000")))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Nouveaux tokens emis par G3",
                                ref("LoginResponse"),
                                "refreshResponse",
                                map(
                                        "token", "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken", "7d0f450d-1c6c-4d33-b0a7-bf856d3f21aa",
                                        "userId", 1,
                                        "email", "admin.g10@sgitu.ma",
                                        "roles", List.of("ROLE_ADMIN"))))
                        .addApiResponse("401", errorResponse("Refresh token invalide ou expire", "UNAUTHORIZED", 401)));

        return new PathItem().post(operation);
    }

    private PathItem logoutPath() {
        Operation operation = securedOperation(
                "G3 - Auth routee",
                "logoutViaGateway",
                "Logout route vers G3",
                """
                        Revoque le JWT courant dans Redis cote G3. G10 consulte cette
                        blacklist Redis lorsqu'elle est active afin de refuser les tokens
                        deja revoques apres logout.
                        """
        ).requestBody(jsonRequest(
                "Refresh token optionnel a revoquer",
                ref("RefreshRequest"),
                "logoutRequest",
                map("refreshToken", "550e8400-e29b-41d4-a716-446655440000")))
                .responses(new ApiResponses()
                        .addApiResponse("204", noContentResponse("Deconnexion reussie"))
                        .addApiResponse("400", errorResponse("Authorization header manquant ou incorrect", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent, invalide ou revoque", "UNAUTHORIZED", 401)));

        return new PathItem().post(operation);
    }

    private PathItem registerPath() {
        Operation createOperation = publicOperation(
                "G3 - Auth routee",
                "registerViaGateway",
                "Create user route vers G3",
                """
                        Route publique de creation de compte, traitee par le service G3.
                        Dans G3, l'inscription est exposee sur `POST /api/users`.
                        """
        ).requestBody(jsonRequest(
                "Informations de creation de compte",
                ref("UserRegistrationRequest"),
                "registerRequest",
                map("email", "new.user@sgitu.ma", "password", "Password123", "role", "ROLE_PASSENGER")))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse(
                                "Compte cree par G3",
                                ref("UserResponse"),
                                "registerResponse",
                                userExample()))
                        .addApiResponse("400", errorResponse("Payload invalide", "BAD_REQUEST", 400))
                        .addApiResponse("409", errorResponse("Compte deja existant", "CONFLICT", 409)));

        Operation listOperation = securedOperation(
                "G3 - Administration via Gateway",
                "listUsersViaGateway",
                "List users",
                """
                        Liste les utilisateurs G3, avec filtre optionnel par role.
                        Acces : ROLE_ADMIN.
                        """
        ).addParametersItem(new Parameter()
                .name("role")
                .in("query")
                .required(false)
                .description("Filtre optionnel par role, par exemple ROLE_PASSENGER ou ROLE_OPERATOR.")
                .schema(new StringSchema().example("ROLE_PASSENGER")))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Liste utilisateurs",
                                arrayOf(ref("UserResponse")),
                                "users",
                                List.of(userExample())))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN requis", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem()
                .post(createOperation)
                .get(listOperation);
    }

    private PathItem usersByRolePath() {
        Operation operation = securedOperation(
                "G3 - Utilisateurs via Gateway",
                "getUsersByRoleViaGateway",
                "Get users by role",
                """
                        Retourne les utilisateurs possedant un role donne.
                        Acces aligne avec G3 : ROLE_SUPERVISOR ou ROLE_DISPATCHER.
                        """
        ).addParametersItem(pathParameter("roleName", "Role recherche", "ROLE_DRIVER"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Liste utilisateurs par role",
                                arrayOf(ref("UserResponse")),
                                "usersByRole",
                                List.of(userExample())))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_SUPERVISOR ou ROLE_DISPATCHER requis", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem driverIdsPath() {
        Operation operation = securedOperation(
                "G3 - Utilisateurs via Gateway",
                "getDriverIdsViaGateway",
                "Get driver ids",
                "Retourne les identifiants des utilisateurs avec ROLE_DRIVER. Acces : JWT valide."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse(
                        "Liste des ids chauffeurs",
                        arrayOf(new IntegerSchema().format("int64").example(1)),
                        "driverIds",
                        List.of(1, 2, 3)))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem notificationRecipientsPath() {
        Operation operation = securedOperation(
                "G3 - Utilisateurs via Gateway",
                "getNotificationRecipientsViaGateway",
                "Get notification recipients",
                """
                        Retourne les destinataires actifs utilisables par G4/G5.
                        Acces aligne avec G3 : ROLE_G4_OPERATOR ou ROLE_DISPATCHER.
                        """
        ).addParametersItem(queryParameter("page", "Numero de page 0-based", "0"))
                .addParametersItem(queryParameter("size", "Taille de page", "100"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Destinataires notification",
                                new ObjectSchema(),
                                "notificationRecipients",
                                map(
                                        "recipients", List.of(map(
                                                "userId", 1,
                                                "email", "dispatcher@sgitu.ma")),
                                        "page", 0,
                                        "size", 100,
                                        "totalElements", 1)))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_G4_OPERATOR ou ROLE_DISPATCHER requis", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem userByIdPath() {
        Operation getOperation = securedOperation(
                "G3 - Utilisateurs via Gateway",
                "getUserByIdViaGateway",
                "Get user by id",
                "Retourne le profil complet d'un utilisateur G3. Acces : JWT valide."
        ).addParametersItem(pathParameter("id", "Identifiant utilisateur G3", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Utilisateur trouve", ref("UserResponse"), "user", userExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Utilisateur introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        Operation updateOperation = securedOperation(
                "G3 - Utilisateurs via Gateway",
                "updateUserViaGateway",
                "Update user profile",
                "Met a jour l'email et/ou le profil utilisateur. Acces : JWT valide."
        ).addParametersItem(pathParameter("id", "Identifiant utilisateur G3", "1"))
                .requestBody(jsonRequest(
                        "Nouvelles informations utilisateur",
                        ref("UserUpdateRequest"),
                        "updateUser",
                        userUpdateExample()))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Utilisateur mis a jour", ref("UserResponse"), "user", userExample()))
                        .addApiResponse("400", errorResponse("Payload invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Utilisateur introuvable", "NOT_FOUND", 404))
                        .addApiResponse("409", errorResponse("Email deja utilise", "CONFLICT", 409))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        Operation deleteOperation = securedOperation(
                "G3 - Administration via Gateway",
                "deleteUserViaGateway",
                "Delete user",
                "Supprime definitivement un utilisateur. Acces : ROLE_ADMIN."
        ).addParametersItem(pathParameter("id", "Identifiant utilisateur G3", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("204", noContentResponse("Utilisateur supprime"))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN requis", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Utilisateur introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem()
                .get(getOperation)
                .put(updateOperation)
                .delete(deleteOperation);
    }

    private PathItem userExistsPath() {
        Operation operation = securedOperation(
                "G3 - Utilisateurs via Gateway",
                "userExistsViaGateway",
                "Check user existence",
                "Verifie si un utilisateur existe sans charger son profil complet. Acces : JWT valide."
        ).addParametersItem(pathParameter("id", "Identifiant utilisateur G3", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Resultat existence",
                                ref("UserExistsResponse"),
                                "exists",
                                map("exists", true)))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem changePasswordPath() {
        Operation operation = securedOperation(
                "G3 - Utilisateurs via Gateway",
                "changePasswordViaGateway",
                "Change user password",
                "Change le mot de passe utilisateur. Le corps accepte `newPassword` ou `password`. Acces : JWT valide."
        ).addParametersItem(pathParameter("id", "Identifiant utilisateur G3", "1"))
                .requestBody(jsonRequest(
                        "Nouveau mot de passe",
                        ref("PasswordChangeRequest"),
                        "passwordChange",
                        map("newPassword", "NewPassword123")))
                .responses(new ApiResponses()
                        .addApiResponse("200", noContentResponse("Mot de passe modifie"))
                        .addApiResponse("400", errorResponse("Nouveau mot de passe manquant ou invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Utilisateur introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().put(operation);
    }

    private PathItem updateRolesPath() {
        Operation operation = securedOperation(
                "G3 - Administration via Gateway",
                "updateUserRolesViaGateway",
                "Update user roles",
                """
                        Remplace les roles de l'utilisateur.
                        Roles valides : ROLE_PASSENGER, ROLE_STUDENT, ROLE_DRIVER,
                        ROLE_STAFF, ROLE_OPERATOR, ROLE_TECHNICIAN, ROLE_ADMIN.
                        Acces : ROLE_ADMIN.
                        """
        ).addParametersItem(pathParameter("id", "Identifiant utilisateur G3", "1"))
                .requestBody(jsonRequest(
                        "Nouveaux roles",
                        ref("RoleUpdateRequest"),
                        "roles",
                        map("roles", List.of("ROLE_OPERATOR", "ROLE_STAFF"))))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Roles mis a jour", ref("UserResponse"), "user", userExample()))
                        .addApiResponse("400", errorResponse("Liste de roles invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN requis", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Utilisateur introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().put(operation);
    }

    private PathItem userStatusPath(String action, String summary, String description, boolean active) {
        Operation operation = securedOperation(
                "G3 - Administration via Gateway",
                action + "UserViaGateway",
                summary,
                description
        ).addParametersItem(pathParameter("id", "Identifiant utilisateur G3", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Statut utilisateur modifie",
                                ref("UserResponse"),
                                "user",
                                userExample(active)))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN requis", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Utilisateur introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G3 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().put(operation);
    }

    private PathItem g1TicketsPath() {
        Operation createOperation = securedOperation(
                "G1 - Billetterie via Gateway",
                "createG1TicketViaGateway",
                "Create ticket",
                "Cree un ticket dematerialise G1. Acces : JWT valide."
        ).requestBody(jsonRequest(
                        "Ticket a creer",
                        new ObjectSchema(),
                        "createTicket",
                        g1TicketCreateExample()))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse("Ticket cree", new ObjectSchema(), "ticket", g1TicketExample()))
                        .addApiResponse("400", errorResponse("Payload invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G1 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(createOperation);
    }

    private PathItem g1TicketByIdPath() {
        Operation getOperation = securedOperation(
                "G1 - Billetterie via Gateway",
                "getG1TicketViaGateway",
                "Get ticket",
                "Retourne un ticket G1 par identifiant. Acces : JWT valide."
        ).addParametersItem(pathParameter("ticketId", "Identifiant du ticket G1", "TCK-001"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Ticket trouve", new ObjectSchema(), "ticket", g1TicketExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Ticket introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G1 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(getOperation);
    }

    private PathItem g1TicketsByUserPath() {
        Operation operation = securedOperation(
                "G1 - Billetterie via Gateway",
                "listG1TicketsByUserViaGateway",
                "List user tickets",
                "Retourne l'historique des tickets d'un utilisateur. Acces : JWT valide."
        ).addParametersItem(pathParameter("userId", "Identifiant utilisateur", "101"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Tickets utilisateur",
                                arrayOf(new ObjectSchema()),
                                "tickets",
                                List.of(g1TicketExample())))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G1 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g1TicketActionPath(String operationId, String summary, String description) {
        Operation operation = securedOperation(
                "G1 - Billetterie via Gateway",
                operationId,
                summary,
                description
        ).addParametersItem(pathParameter("ticketId", "Identifiant du ticket G1", "TCK-001"))
                .requestBody(jsonRequest(
                        "Payload action ticket",
                        new ObjectSchema(),
                        "ticketAction",
                        map("userId", "101")))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Action traitee", new ObjectSchema(), "ticket", g1TicketExample()))
                        .addApiResponse("400", errorResponse("Payload invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Ticket introuvable", "NOT_FOUND", 404))
                        .addApiResponse("422", errorResponse("Regle metier non respectee", "UNPROCESSABLE_ENTITY", 422))
                        .addApiResponse("503", errorResponse("G1 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem g1TicketNoBodyActionPath(String operationId, String summary, String description) {
        Operation operation = securedOperation(
                "G1 - Billetterie via Gateway",
                operationId,
                summary,
                description
        ).addParametersItem(pathParameter("ticketId", "Identifiant du ticket G1", "TCK-001"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Action traitee", new ObjectSchema(), "ticket", g1TicketExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Ticket introuvable", "NOT_FOUND", 404))
                        .addApiResponse("422", errorResponse("Regle metier non respectee", "UNPROCESSABLE_ENTITY", 422))
                        .addApiResponse("503", errorResponse("G1 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem g1TransferDecisionPath(String operationId, String summary, String description) {
        Operation operation = securedOperation(
                "G1 - Billetterie via Gateway",
                operationId,
                summary,
                description
        ).addParametersItem(pathParameter("ticketId", "Identifiant du ticket G1 en transfert", "TCK-PENDING-001"))
                .requestBody(jsonRequest(
                        "Decision de transfert",
                        new ObjectSchema(),
                        "transferDecision",
                        map("acceptingUserId", "102", "reason", "Decision via Gateway G10")))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Decision traitee", new ObjectSchema(), "ticket", g1TicketExample()))
                        .addApiResponse("400", errorResponse("Payload invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Ticket introuvable", "NOT_FOUND", 404))
                        .addApiResponse("422", errorResponse("Regle metier non respectee", "UNPROCESSABLE_ENTITY", 422))
                        .addApiResponse("503", errorResponse("G1 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem g1AdminSimpleGetPath(String operationId, String summary, String description) {
        Operation operation = securedOperation(
                "G1 - Administration via Gateway",
                operationId,
                summary,
                description
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Reponse G1 admin", new ObjectSchema(), "g1Admin", map("status", "OK")))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("ROLE_ADMIN requis", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G1 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g1AdminTicketGetPath(String operationId, String summary, String description) {
        Operation operation = securedOperation(
                "G1 - Administration via Gateway",
                operationId,
                summary,
                description
        ).addParametersItem(pathParameter("ticketId", "Identifiant du ticket G1", "TCK-001"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Reponse G1 admin", new ObjectSchema(), "g1Admin", map("status", "OK")))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN requis", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Ticket introuvable", "NOT_FOUND", 404))
                        .addApiResponse("422", errorResponse("Regle metier non respectee", "UNPROCESSABLE_ENTITY", 422))
                        .addApiResponse("503", errorResponse("G1 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g1AdminTicketBodyPath(
            String operationId,
            String summary,
            String description,
            String method,
            Map<String, Object> example
    ) {
        Operation operation = securedOperation(
                "G1 - Administration via Gateway",
                operationId,
                summary,
                description
        ).addParametersItem(pathParameter("ticketId", "Identifiant du ticket G1", "TCK-001"))
                .requestBody(jsonRequest("Payload admin G1", new ObjectSchema(), "g1AdminAction", example))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Action admin traitee", new ObjectSchema(), "ticket", g1TicketExample()))
                        .addApiResponse("400", errorResponse("Payload invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN requis", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Ticket introuvable", "NOT_FOUND", 404))
                        .addApiResponse("422", errorResponse("Regle metier non respectee", "UNPROCESSABLE_ENTITY", 422))
                        .addApiResponse("503", errorResponse("G1 indisponible", "SERVICE_UNAVAILABLE", 503)));

        if ("PUT".equalsIgnoreCase(method)) {
            return new PathItem().put(operation);
        }
        return new PathItem().post(operation);
    }

    private PathItem plansPath() {
        Operation listOperation = publicOperation(
                "G2 - Abonnements via Gateway",
                "listG2PlansViaGateway",
                "List subscription plans",
                "Route Gateway vers G2 `/plans`. Acces public en lecture."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Plans G2", new ObjectSchema(), "plansPage", map("content", List.of(planExample()))))
                .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        Operation createOperation = securedOperation(
                "G2 - Administration via Gateway",
                "createG2PlanViaGateway",
                "Create subscription plan",
                "Route Gateway vers G2 `/plans`. Acces : ROLE_ADMIN_G2."
        ).requestBody(jsonRequest("Plan a creer", ref("PlanAbonnement"), "plan", planExample()))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Plan cree", ref("PlanAbonnement"), "plan", planExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN_G2 requis", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(listOperation).post(createOperation);
    }

    private PathItem planByIdPath() {
        Operation getOperation = publicOperation(
                "G2 - Abonnements via Gateway",
                "getG2PlanViaGateway",
                "Get subscription plan",
                "Route Gateway vers G2 `/plans/{id}`. Acces public en lecture."
        ).addParametersItem(pathParameter("id", "Identifiant du plan G2", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Plan trouve", ref("PlanAbonnement"), "plan", planExample()))
                        .addApiResponse("404", errorResponse("Plan introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        Operation updateOperation = securedOperation(
                "G2 - Administration via Gateway",
                "updateG2PlanViaGateway",
                "Update subscription plan",
                "Route Gateway vers G2 `/plans/{id}`. Acces : ROLE_ADMIN_G2."
        ).addParametersItem(pathParameter("id", "Identifiant du plan G2", "1"))
                .requestBody(jsonRequest("Plan mis a jour", ref("PlanAbonnement"), "plan", planExample()))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Plan mis a jour", ref("PlanAbonnement"), "plan", planExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN_G2 requis", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Plan introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        Operation deleteOperation = securedOperation(
                "G2 - Administration via Gateway",
                "deleteG2PlanViaGateway",
                "Delete subscription plan",
                "Route Gateway vers G2 `/plans/{id}`. Acces : ROLE_ADMIN_G2."
        ).addParametersItem(pathParameter("id", "Identifiant du plan G2", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("204", noContentResponse("Plan supprime"))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN_G2 requis", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Plan introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(getOperation).put(updateOperation).delete(deleteOperation);
    }

    private PathItem subscribePath() {
        Operation operation = securedOperation(
                "G2 - Abonnements via Gateway",
                "subscribeViaGateway",
                "Subscribe to plan",
                "Route Gateway vers G2 `/abonnements/souscrire`. Acces : JWT valide."
        ).addParametersItem(new Parameter().name("userId").in("query").required(true)
                        .schema(new IntegerSchema().format("int64").example(1)))
                .addParametersItem(new Parameter().name("planId").in("query").required(true)
                        .schema(new IntegerSchema().format("int64").example(1)))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse("Abonnement cree", new ObjectSchema(), "subscription", map("id", 1, "statut", "EN_ATTENTE_PAIEMENT")))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Plan ou utilisateur introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem subscriptionByIdPath() {
        Operation operation = publicOperation(
                "G2 - Abonnements via Gateway",
                "getSubscriptionViaGateway",
                "Get subscription",
                "Route Gateway vers G2 `/abonnements/{id}`. Acces public en lecture selon le contrat G2."
        ).addParametersItem(pathParameter("id", "Identifiant abonnement G2", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Abonnement trouve", new ObjectSchema(), "subscription", map("id", 1, "statut", "ACTIF")))
                        .addApiResponse("404", errorResponse("Abonnement introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem subscriptionsByUserPath() {
        Operation operation = publicOperation(
                "G2 - Abonnements via Gateway",
                "getUserSubscriptionsViaGateway",
                "List user subscriptions",
                "Route Gateway vers G2 `/abonnements/utilisateur/{userId}`. Acces public en lecture selon le contrat G2."
        ).addParametersItem(pathParameter("userId", "Identifiant utilisateur", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Abonnements utilisateur", arrayOf(new ObjectSchema()), "subscriptions", List.of(map("id", 1, "statut", "ACTIF"))))
                        .addApiResponse("404", errorResponse("Utilisateur introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem paymentCallbackPath() {
        Operation operation = publicOperation(
                "G2 - Abonnements via Gateway",
                "confirmG2PaymentViaGateway",
                "Payment callback",
                "Callback G6 route vers G2 `/abonnements/paiement/confirmation`. Acces public."
        ).requestBody(jsonRequest("Callback paiement", new ObjectSchema(), "paymentCallback",
                        map("transactionToken", "tx-demo", "status", "SUCCESS", "message", "Paiement accepte")))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Paiement traite", new ObjectSchema(), "ok", map("statut", "OK")))
                        .addApiResponse("404", errorResponse("Transaction introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem refundCallbackPath() {
        Operation operation = publicOperation(
                "G2 - Abonnements via Gateway",
                "confirmG2RefundViaGateway",
                "Refund callback",
                "Callback G6 route vers G2 `/abonnements/remboursement/confirmation`. Acces public."
        ).requestBody(jsonRequest("Callback remboursement", new ObjectSchema(), "refundCallback",
                        map("transactionId", "refund-demo", "statut", "REMBOURSE", "montantRembourse", 25.0, "motif", "Annulation")))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Remboursement traite", new ObjectSchema(), "ok", map("statut", "OK")))
                        .addApiResponse("404", errorResponse("Transaction introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem adminSuspendSubscriptionPath() {
        Operation operation = securedOperation(
                "G2 - Administration via Gateway",
                "suspendG2SubscriptionViaGateway",
                "Suspend subscription",
                "Route Gateway vers G2 `/abonnements/admin/{id}/suspendre`. Acces : ROLE_ADMIN_G2."
        ).addParametersItem(pathParameter("id", "Identifiant abonnement G2", "1"))
                .addParametersItem(new Parameter().name("motif").in("query").required(true)
                        .schema(new StringSchema().example("Controle administratif")))
                .responses(new ApiResponses()
                        .addApiResponse("200", noContentResponse("Abonnement suspendu"))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN_G2 requis", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Abonnement introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G2 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem g4HealthPath() {
        Operation operation = publicOperation(
                "G4 - Coordination via Gateway",
                "getG4HealthViaGateway",
                "Health G4 route vers Coordination",
                "Verifie l'etat du service G4 Coordination via la Gateway. Acces public."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("G4 disponible", new ObjectSchema(), "g4Health",
                        map("status", "UP", "component", "G4-Coordination-Transport")))
                .addApiResponse("503", errorResponse("G4 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g4LogsPath() {
        Operation operation = publicOperation(
                "G4 - Coordination via Gateway",
                "getG4LogsViaGateway",
                "Logs G4 route vers Coordination",
                "Retourne les logs de supervision exposes par G4. Acces public selon contrat G4."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Logs G4", arrayOf(new ObjectSchema()), "g4Logs",
                        List.of(map("level", "INFO", "message", "G4 request processed"))))
                .addApiResponse("503", errorResponse("G4 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g4LignesPath() {
        Operation listOperation = securedOperation(
                "G4 - Coordination via Gateway",
                "listG4LinesViaGateway",
                "List G4 lines",
                "Liste les lignes de transport G4. Acces : ROLE_G4_OPERATOR, ROLE_DISPATCHER ou ROLE_G4_ADMIN."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Lignes G4", arrayOf(ref("G4Ligne")), "g4Lines", List.of(g4LineExample())))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G4 indisponible", "SERVICE_UNAVAILABLE", 503)));

        Operation createOperation = securedOperation(
                "G4 - Coordination via Gateway",
                "createG4LineViaGateway",
                "Create G4 line",
                "Cree une ligne G4. Acces : ROLE_G4_OPERATOR ou ROLE_G4_ADMIN."
        ).requestBody(jsonRequest("Ligne a creer", ref("G4Ligne"), "g4Line", g4LineExample()))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse("Ligne creee", ref("G4Ligne"), "g4Line", g4LineExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G4 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(listOperation).post(createOperation);
    }

    private PathItem g4LigneByIdPath() {
        Operation operation = securedOperation(
                "G4 - Coordination via Gateway",
                "getG4LineByIdViaGateway",
                "Get G4 line by id",
                "Consulte une ligne G4 par id. Acces : ROLE_G4_OPERATOR, ROLE_DISPATCHER ou ROLE_G4_ADMIN."
        ).addParametersItem(pathParameter("id", "Identifiant ligne G4", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Ligne trouvee", ref("G4Ligne"), "g4Line", g4LineExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Ligne introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G4 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g4MissionsPath() {
        Operation listOperation = securedOperation(
                "G4 - Coordination via Gateway",
                "listG4MissionsViaGateway",
                "List G4 missions",
                "Liste les missions G4. Acces : ROLE_G4_OPERATOR, ROLE_DISPATCHER ou ROLE_G4_ADMIN."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Missions G4", arrayOf(ref("G4Mission")), "g4Missions", List.of(g4MissionExample())))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G4 indisponible", "SERVICE_UNAVAILABLE", 503)));

        Operation createOperation = securedOperation(
                "G4 - Coordination via Gateway",
                "createG4MissionViaGateway",
                "Create G4 mission",
                "Cree une mission G4. Acces : ROLE_DISPATCHER ou ROLE_G4_ADMIN."
        ).requestBody(jsonRequest("Mission a creer", ref("G4Mission"), "g4Mission", g4MissionExample()))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse("Mission creee", ref("G4Mission"), "g4Mission", g4MissionExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G4 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(listOperation).post(createOperation);
    }

    private PathItem g4MissionStatusPath() {
        Operation operation = securedOperation(
                "G4 - Coordination via Gateway",
                "getG4MissionStatusViaGateway",
                "Get G4 mission status",
                "Consulte le statut d'une mission G4. Acces : ROLE_G4_OPERATOR, ROLE_DISPATCHER ou ROLE_G4_ADMIN."
        ).addParametersItem(pathParameter("id", "Identifiant mission G4", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Statut mission", new ObjectSchema(), "g4MissionStatus",
                                map("missionId", 1, "statut", "PLANIFIEE", "vehiculeId", "VH-G10-G4-001", "ligneId", 1)))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G4 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g4OperatorStatusPath() {
        Operation operation = securedOperation(
                "G4 - Coordination via Gateway",
                "getG4OperatorStatusViaGateway",
                "Get G4 operator status",
                "Retourne la supervision operateur G4. Acces : ROLE_G4_ADMIN."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Statut operateur G4", new ObjectSchema(), "g4OperatorStatus",
                        map("status", "UP", "activeMissions", 3, "pendingNotifications", 0)))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G4 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g5HealthPath() {
        Operation operation = publicOperation(
                "G5 - Notifications via Gateway",
                "getG5HealthViaGateway",
                "Health G5 route vers Notifications",
                "Verifie l'etat du service G5 via la Gateway. Acces public pour faciliter les tests d'integration."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("G5 disponible", new ObjectSchema(), "g5Health",
                        map("status", "UP", "timestamp", "2026-05-31T12:00:00")))
                .addApiResponse("503", errorResponse("G5 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g5SendNotificationPath() {
        Operation operation = securedOperation(
                "G5 - Notifications via Gateway",
                "sendG5NotificationViaGateway",
                "Send G5 notification",
                """
                        Route Gateway vers G5 Notifications (`notification-service:8085`).
                        Acces : JWT valide. Les endpoints admin G5 sont separement reserves a ROLE_ADMIN.
                        """
        ).requestBody(jsonRequest("Notification a envoyer", ref("G5NotificationRequest"), "g5NotificationRequest",
                        g5NotificationRequestExample()))
                .responses(new ApiResponses()
                        .addApiResponse("202", jsonResponse("Notification acceptee", ref("G5NotificationResponse"),
                                "g5NotificationResponse", g5NotificationResponseExample()))
                        .addApiResponse("400", errorResponse("Payload notification invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G5 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem g5NotificationsPath() {
        Operation operation = securedOperation(
                "G5 - Notifications via Gateway",
                "listG5NotificationsViaGateway",
                "List G5 notifications",
                "Liste paginee des notifications G5. Acces : JWT valide."
        ).addParametersItem(queryParameter("userId", "Filtre optionnel par utilisateur", "501"))
                .addParametersItem(queryParameter("status", "Filtre optionnel : PENDING, SENT ou FAILED", "PENDING"))
                .addParametersItem(queryParameter("sourceService", "Filtre optionnel par service source", "G10_GATEWAY"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Notifications G5", new ObjectSchema(), "g5Notifications",
                                map("content", List.of(g5NotificationResponseExample()), "totalElements", 1)))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G5 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g5NotificationByIdPath() {
        Operation operation = securedOperation(
                "G5 - Notifications via Gateway",
                "getG5NotificationByIdViaGateway",
                "Get G5 notification by id",
                "Consulte une notification G5 par son identifiant fonctionnel. Acces : JWT valide."
        ).addParametersItem(pathParameter("notificationId", "Identifiant notification G5", "g10-g5-demo"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Notification G5", ref("G5NotificationResponse"),
                                "g5NotificationResponse", g5NotificationResponseExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Notification introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G5 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g5RetryNotificationPath() {
        Operation operation = securedOperation(
                "G5 - Notifications via Gateway",
                "retryG5NotificationViaGateway",
                "Retry G5 notification",
                "Relance une notification G5. Acces : JWT valide."
        ).addParametersItem(pathParameter("notificationId", "Identifiant notification G5", "g10-g5-demo"))
                .responses(new ApiResponses()
                        .addApiResponse("202", jsonResponse("Retry accepte", ref("G5NotificationResponse"),
                                "g5NotificationResponse", g5NotificationResponseExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Notification introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G5 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem g5AdminStatsPath() {
        Operation operation = securedOperation(
                "G5 - Notifications via Gateway",
                "getG5AdminStatsViaGateway",
                "Get G5 admin stats",
                "Retourne les statistiques G5. Acces : ROLE_ADMIN."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Stats G5", new ObjectSchema(), "g5AdminStats",
                        map("total", 12, "byStatus", map("PENDING", 2, "SENT", 9, "FAILED", 1))))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("ROLE_ADMIN requis", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G5 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g6HealthPath() {
        Operation operation = publicOperation(
                "G6 - Paiement via Gateway",
                "getG6HealthViaGateway",
                "Health G6 route vers Paiement",
                "Verifie l'etat du service G6 via la Gateway. Acces public pour les tests d'integration."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("G6 disponible", new StringSchema().example("G6 Payment Service - UP"),
                        "g6Health", "G6 Payment Service - UP"))
                .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g6TestCardsPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "listG6TestCardsViaGateway",
                "List G6 test cards",
                "Retourne les cartes de test G6. Acces : JWT valide."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Cartes de test", arrayOf(new ObjectSchema()), "g6TestCards",
                        List.of(map("id", 1, "last4", "0366", "provider", "VISA", "balance", 1000.0, "status", "ACTIVE"))))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g6TestMobileMoneyPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "listG6TestMobileMoneyViaGateway",
                "List G6 test mobile money accounts",
                "Retourne les comptes Mobile Money de test G6. Acces : JWT valide."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Comptes Mobile Money", arrayOf(new ObjectSchema()), "g6MobileMoney",
                        List.of(map("id", 1, "maskedPhone", "0612****78", "provider", "INWI", "balance", 500.0, "status", "ACTIVE"))))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g6PaymentAccountsByUserPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "listG6PaymentAccountsByUserViaGateway",
                "List G6 payment accounts by user",
                "Retourne les moyens de paiement enregistres pour un utilisateur. Acces : JWT valide."
        ).addParametersItem(pathParameter("userId", "Identifiant utilisateur", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Moyens de paiement", arrayOf(new ObjectSchema()), "g6PaymentAccounts",
                                List.of(g6PaymentAccountExample())))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g6PaymentAccountByIdPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "getG6PaymentAccountByIdViaGateway",
                "Get G6 payment account by id",
                "Consulte un moyen de paiement G6. Acces : JWT valide."
        ).addParametersItem(pathParameter("id", "Identifiant du moyen de paiement", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Moyen de paiement", new ObjectSchema(), "g6PaymentAccount",
                                g6PaymentAccountExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Moyen de paiement introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g6AddCardPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "addG6CardViaGateway",
                "Add G6 card",
                "Ajoute une carte bancaire de test G6. Acces : JWT valide."
        ).requestBody(jsonRequest("Carte a ajouter", new ObjectSchema(), "g6AddCard", map(
                        "userId", 6,
                        "cardNumber", "5500000000000004",
                        "cvv", "999",
                        "expiryMonth", 6,
                        "expiryYear", 2028,
                        "email", "passenger.g6@sgitu.ma")))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse("Carte creee", new ObjectSchema(), "g6PaymentAccount",
                                g6PaymentAccountExample()))
                        .addApiResponse("400", errorResponse("Carte invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem g6PaymentsPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "processG6PaymentViaGateway",
                "Process G6 payment",
                "Cree et traite un paiement G6. Acces : JWT valide."
        ).requestBody(jsonRequest("Paiement a traiter", new ObjectSchema(), "g6PaymentRequest", g6PaymentRequestExample()))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Paiement traite", new ObjectSchema(), "g6PaymentResponse",
                                g6PaymentResponseExample("FAILED")))
                        .addApiResponse("201", jsonResponse("Paiement valide", new ObjectSchema(), "g6PaymentResponseSuccess",
                                g6PaymentResponseExample("SUCCESS")))
                        .addApiResponse("400", errorResponse("Payload invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem g6PaymentByIdPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "getG6PaymentByIdViaGateway",
                "Get G6 payment by id",
                "Consulte un paiement G6 par son ID. Acces : JWT valide."
        ).addParametersItem(pathParameter("paymentId", "Identifiant paiement", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Paiement", new ObjectSchema(), "g6Payment", g6PaymentDetailsExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Paiement introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g6InvoiceByPaymentPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "getG6InvoiceByPaymentViaGateway",
                "Get G6 invoice by payment",
                "Retourne la facture associee a un paiement G6. Acces : JWT valide."
        ).addParametersItem(pathParameter("paymentId", "Identifiant paiement", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Facture", new ObjectSchema(), "g6Invoice",
                                map("invoiceId", 1, "paymentId", 1, "invoiceNumber", "INV-2026-0001", "amount", 25.0)))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("404", errorResponse("Facture introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g6RefundPaymentPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "refundG6PaymentViaGateway",
                "Refund G6 payment",
                "Demande un remboursement sur un paiement G6. Acces : JWT valide."
        ).addParametersItem(pathParameter("paymentId", "Identifiant paiement", "1"))
                .requestBody(jsonRequest("Remboursement", new ObjectSchema(), "g6RefundRequest",
                        map("amount", 5.0, "reason", "Test remboursement via G10")))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse("Remboursement cree", new ObjectSchema(), "g6Refund",
                                map("refundId", 1, "paymentId", 1, "status", "REFUNDED", "amount", 5.0)))
                        .addApiResponse("400", errorResponse("Remboursement impossible", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem g6RefundsByPaymentPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "listG6RefundsByPaymentViaGateway",
                "List G6 refunds by payment",
                "Liste les remboursements d'un paiement G6. Acces : JWT valide."
        ).addParametersItem(pathParameter("paymentId", "Identifiant paiement", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Remboursements", arrayOf(new ObjectSchema()), "g6Refunds",
                                List.of(map("refundId", 1, "paymentId", 1, "status", "REFUNDED", "amount", 5.0))))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g6RefundsByUserPath() {
        Operation operation = securedOperation(
                "G6 - Paiement via Gateway",
                "listG6RefundsByUserViaGateway",
                "List G6 refunds by user",
                "Liste les remboursements d'un utilisateur G6. Acces : JWT valide."
        ).addParametersItem(pathParameter("userId", "Identifiant utilisateur", "1"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Remboursements utilisateur", arrayOf(new ObjectSchema()), "g6Refunds",
                                List.of(map("refundId", 1, "paymentId", 1, "status", "REFUNDED", "amount", 5.0))))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G6 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g7HealthPath() {
        Operation operation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "getG7HealthViaGateway",
                "Health G7 route vers Suivi Vehicules",
                "Verifie l'etat du service G7 via la Gateway. Acces : ROLE_ADMIN_G7, ROLE_OPERATOR ou ROLE_TECHNICIAN."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("G7 disponible", new ObjectSchema(), "g7Health",
                        map("status", "UP", "service", "g7-suivi-vehicules", "version", "1.0.0")))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g7VehiculesPath() {
        Operation listOperation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "listG7VehiclesViaGateway",
                "List G7 vehicles",
                "Liste les vehicules G7. Acces : ROLE_ADMIN_G7, ROLE_OPERATOR ou ROLE_TECHNICIAN."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Vehicules G7", arrayOf(ref("G7Vehicule")), "g7Vehicles", List.of(g7VehicleExample())))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        Operation createOperation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "createG7VehicleViaGateway",
                "Create G7 vehicle",
                "Cree un vehicule G7. Acces : ROLE_ADMIN_G7."
        ).requestBody(jsonRequest("Vehicule a creer", ref("G7Vehicule"), "g7Vehicle", g7VehicleExample()))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse("Vehicule cree", ref("G7Vehicule"), "g7Vehicle", g7VehicleExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN_G7 requis", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(listOperation).post(createOperation);
    }

    private PathItem g7VehiculeByIdPath() {
        Operation operation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "getG7VehicleByIdViaGateway",
                "Get G7 vehicle by id",
                "Consulte un vehicule G7 par UUID. Acces : ROLE_ADMIN_G7, ROLE_OPERATOR ou ROLE_TECHNICIAN."
        ).addParametersItem(pathParameter("id", "UUID vehicule G7", "53c31262-591a-44d4-8872-51e84611ac5e"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Vehicule trouve", ref("G7Vehicule"), "g7Vehicle", g7VehicleExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Vehicule introuvable", "NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g7VehicleStatusPath() {
        Operation operation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "updateG7VehicleStatusViaGateway",
                "Update G7 vehicle status",
                "Change le statut d'un vehicule G7. Acces : ROLE_ADMIN_G7."
        ).addParametersItem(pathParameter("id", "UUID vehicule G7", "53c31262-591a-44d4-8872-51e84611ac5e"))
                .addParametersItem(new Parameter().name("statut").in("query").required(true)
                        .schema(new StringSchema().example("EN_SERVICE")))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Statut mis a jour", ref("G7Vehicule"), "g7Vehicle", g7VehicleExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_ADMIN_G7 requis", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().put(operation);
    }

    private PathItem g7PositionsPath() {
        Operation listOperation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "listG7PositionsViaGateway",
                "List G7 positions",
                "Liste les positions GPS G7. Acces : ROLE_ADMIN_G7, ROLE_OPERATOR ou ROLE_TECHNICIAN."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Positions G7", arrayOf(ref("G7Position")), "g7Positions", List.of(g7PositionExample())))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        Operation createOperation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "createG7PositionViaGateway",
                "Create G7 GPS position",
                "Enregistre une position GPS G7. Acces : ROLE_DRIVER ou ROLE_ADMIN_G7."
        ).requestBody(jsonRequest("Position GPS", ref("G7Position"), "g7Position", g7PositionExample()))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse("Position creee", ref("G7Position"), "g7Position", g7PositionExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_DRIVER ou ROLE_ADMIN_G7 requis", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(listOperation).post(createOperation);
    }

    private PathItem g7PositionByVehiclePath() {
        Operation operation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "getG7CurrentPositionViaGateway",
                "Get G7 current vehicle position",
                "Consulte la position courante d'un vehicule G7. Acces : ROLE_ADMIN_G7, ROLE_OPERATOR ou ROLE_TECHNICIAN."
        ).addParametersItem(pathParameter("vehiculeId", "UUID vehicule G7", "53c31262-591a-44d4-8872-51e84611ac5e"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Position courante", ref("G7Position"), "g7Position", g7PositionExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g7ActiveAlertsPath() {
        Operation operation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "listG7ActiveAlertsViaGateway",
                "List G7 active alerts",
                "Liste les alertes actives G7. Acces : ROLE_ADMIN_G7, ROLE_OPERATOR ou ROLE_TECHNICIAN."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Alertes actives", arrayOf(new ObjectSchema()), "g7Alerts",
                        List.of(map("statut", "OUVERTE", "typeAlert", "VITESSE_EXCESSIVE"))))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g7AlertStatsPath() {
        Operation operation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "getG7AlertStatsViaGateway",
                "Get G7 alert stats",
                "Retourne les statistiques d'alertes G7. Acces : ROLE_ADMIN_G7, ROLE_OPERATOR ou ROLE_TECHNICIAN."
        ).responses(new ApiResponses()
                .addApiResponse("200", jsonResponse("Stats alertes", new ObjectSchema(), "g7AlertStats",
                        map("totalAlertes", 0, "parType", map(), "parStatut", map())))
                .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem g7CancelAlertPath() {
        Operation operation = securedOperation(
                "G7 - Suivi vehicules via Gateway",
                "cancelG7AlertViaGateway",
                "Cancel G7 alert",
                "Annule une alerte G7. Acces : ROLE_OPERATOR ou ROLE_ADMIN_G7."
        ).addParametersItem(pathParameter("id", "UUID alerte G7", "36b01ab6-31f8-47cb-82f4-5ffad4a90b95"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse("Alerte annulee", new ObjectSchema(), "g7Alert",
                                map("statut", "ANNULEE")))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("ROLE_OPERATOR ou ROLE_ADMIN_G7 requis", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G7 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().put(operation);
    }

    private PathItem ingestionPath(String source, String summary) {
        Operation operation = securedOperation(
                "G8 - Ingestion via Gateway",
                "ingest" + capitalize(source) + "ViaGateway",
                summary,
                """
                        Route Gateway vers G8 Analytics Java (`g8-analytics:8088`).
                        Elle recoit un tableau d'evenements JSON. Chaque evenement doit
                        contenir au minimum un `timestamp`.

                        Acces : JWT valide. Tout utilisateur authentifie peut envoyer
                        des evenements d'ingestion.
                        """
        ).requestBody(jsonRequest(
                "Batch d'evenements " + source,
                arrayOf(ref("IncomingEvent")),
                source + "Batch",
                ingestionExample(source)))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse(
                                "Batch accepte",
                                ref("BatchIngestionResponse"),
                                "success",
                                map(
                                        "totalReceived", 1,
                                        "totalAccepted", 1,
                                        "totalRejected", 0,
                                        "rejectedReasons", List.of(),
                                        "status", "SUCCESS")))
                        .addApiResponse("207", jsonResponse(
                                "Batch partiellement accepte",
                                ref("BatchIngestionResponse"),
                                "partial",
                                map(
                                        "totalReceived", 2,
                                        "totalAccepted", 1,
                                        "totalRejected", 1,
                                        "rejectedReasons", List.of("Event 1: Missing required timestamp"),
                                        "status", "PARTIAL")))
                        .addApiResponse("400", errorResponse("Batch vide ou payload invalide", "BAD_REQUEST", 400))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("503", errorResponse("G8 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem analyticsPath(String operationSuffix, String summary, String description) {
        Operation operation = securedOperation(
                "G8 - Analytics via Gateway",
                "getG8" + capitalize(operationSuffix) + "ViaGateway",
                summary,
                description + "\n\nAcces : ROLE_ADMIN, ROLE_OPERATOR ou ROLE_STAFF."
        ).addParametersItem(periodQueryParameter())
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Snapshots G8",
                                arrayOf(ref("StatSnapshot")),
                                "snapshots",
                                List.of(statSnapshotExample())))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G8 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem generateReportPath() {
        Operation operation = securedOperation(
                "G8 - Reports via Gateway",
                "generateG8ReportViaGateway",
                "Generate analytics report",
                "Genere un rapport G8 pour une periode et une liste de types de snapshots. Acces : ROLE_ADMIN, ROLE_OPERATOR ou ROLE_STAFF."
        ).requestBody(jsonRequest(
                "Parametres du rapport",
                ref("ReportRequest"),
                "reportRequest",
                map(
                        "period", "2026-05-08",
                        "types", List.of("TRIPS", "REVENUE", "INCIDENTS", "VEHICLES", "SUBSCRIPTIONS", "USERS"))))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Rapport genere",
                                ref("Report"),
                                "report",
                                reportExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("G8 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem getReportByIdPath() {
        Operation operation = securedOperation(
                "G8 - Reports via Gateway",
                "getG8ReportByIdViaGateway",
                "Get analytics report by id",
                "Retourne un rapport G8 deja genere. Acces : ROLE_ADMIN, ROLE_OPERATOR ou ROLE_STAFF."
        ).addParametersItem(pathParameter("id", "Identifiant MongoDB du rapport", "000000000000000000000000"))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Rapport trouve",
                                ref("Report"),
                                "report",
                                reportExample()))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("404", errorResponse("Rapport introuvable", "ROUTE_NOT_FOUND", 404))
                        .addApiResponse("503", errorResponse("G8 indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().get(operation);
    }

    private PathItem predictPeakHoursPath() {
        Operation operation = securedOperation(
                "G8 - ML via Gateway",
                "predictPeakHoursViaGateway",
                "Predict peak hours",
                "Route Gateway vers G8 ML (`ml-service:5000`). Acces : ROLE_ADMIN, ROLE_OPERATOR ou ROLE_STAFF."
        ).requestBody(jsonRequest(
                "Donnees horaires de validation",
                ref("PeakHoursPredictionRequest"),
                "peakHoursRequest",
                map("data", List.of(
                        map("hour", 8, "validationCount", 120),
                        map("hour", 17, "validationCount", 180),
                        map("hour", 18, "validationCount", 160)))))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Prediction heures de pointe",
                                ref("PeakHoursPredictionResponse"),
                                "peakHoursResponse",
                                map(
                                        "predicted_peak_hours", List.of(17, 18, 8),
                                        "distribution", List.of(
                                                map("hour", 8, "score", 0.2608),
                                                map("hour", 17, "score", 0.3913)),
                                        "generatedAt", "2026-05-08T00:24:38.356034")))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("ML indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private PathItem predictIncidentsPath() {
        Operation operation = securedOperation(
                "G8 - ML via Gateway",
                "predictIncidentsViaGateway",
                "Predict incident risk zones",
                "Route Gateway vers G8 ML (`ml-service:5000`). Acces : ROLE_ADMIN, ROLE_OPERATOR ou ROLE_STAFF."
        ).requestBody(jsonRequest(
                "Donnees incidents par zone",
                ref("IncidentPredictionRequest"),
                "incidentPredictionRequest",
                map("data", List.of(
                        map("zone", "Z_CENTER", "incidentCount", 5, "severity", "HIGH"),
                        map("zone", "Z_NORTH", "incidentCount", 2, "severity", "CRITICAL"),
                        map("zone", "Z_WEST", "incidentCount", 8, "severity", "LOW")))))
                .responses(new ApiResponses()
                        .addApiResponse("200", jsonResponse(
                                "Prediction zones a risque",
                                ref("IncidentPredictionResponse"),
                                "incidentPredictionResponse",
                                map(
                                        "at_risk_zones", List.of(
                                                map("zone", "Z_CENTER", "riskScore", 1.0, "riskLevel", "HIGH"),
                                                map("zone", "Z_NORTH", "riskScore", 0.533, "riskLevel", "MEDIUM")),
                                        "generatedAt", "2026-05-08T00:24:39.120000")))
                        .addApiResponse("401", errorResponse("JWT absent ou invalide", "UNAUTHORIZED", 401))
                        .addApiResponse("403", errorResponse("Role insuffisant", "FORBIDDEN", 403))
                        .addApiResponse("503", errorResponse("ML indisponible", "SERVICE_UNAVAILABLE", 503)));

        return new PathItem().post(operation);
    }

    private Operation publicOperation(String tag, String operationId, String summary, String description) {
        return new Operation()
                .addTagsItem(tag)
                .operationId(operationId)
                .summary(summary)
                .description(description)
                .addParametersItem(correlationHeader());
    }

    private Operation securedOperation(String tag, String operationId, String summary, String description) {
        return publicOperation(tag, operationId, summary, description)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    private Parameter correlationHeader() {
        return new Parameter()
                .name(CORRELATION_ID)
                .in("header")
                .required(false)
                .description("Identifiant de correlation. Si absent, G10 en genere un automatiquement.")
                .schema(new StringSchema().example("g10-g8-dashboard"));
    }

    private Parameter periodQueryParameter() {
        return new Parameter()
                .name("period")
                .in("query")
                .required(false)
                .description("Filtre optionnel de periode selon le format utilise par G8, par exemple 2026-05-08.")
                .schema(new StringSchema().example("2026-05-08"));
    }

    private Parameter queryParameter(String name, String description, String example) {
        return new Parameter()
                .name(name)
                .in("query")
                .required(false)
                .description(description)
                .schema(new StringSchema().example(example));
    }

    private Parameter pathParameter(String name, String description, String example) {
        return new Parameter()
                .name(name)
                .in("path")
                .required(true)
                .description(description)
                .schema(new StringSchema().example(example));
    }

    private RequestBody jsonRequest(String description, Schema<?> schema, String exampleName, Object exampleValue) {
        return new RequestBody()
                .required(true)
                .description(description)
                .content(jsonContent(schema, exampleName, exampleValue));
    }

    private ApiResponse jsonResponse(String description, Schema<?> schema, String exampleName, Object exampleValue) {
        return new ApiResponse()
                .description(description)
                .addHeaderObject(CORRELATION_ID, correlationResponseHeader())
                .content(jsonContent(schema, exampleName, exampleValue));
    }

    private ApiResponse noContentResponse(String description) {
        return new ApiResponse()
                .description(description)
                .addHeaderObject(CORRELATION_ID, correlationResponseHeader());
    }

    private ApiResponse errorResponse(String description, String code, int status) {
        return jsonResponse(
                description,
                ref("ApiError"),
                code.toLowerCase(),
                map(
                        "timestamp", "2026-05-08T00:21:40.830312320Z",
                        "status", status,
                        "error", httpReason(status),
                        "code", code,
                        "message", description,
                        "path", "/api/v1/analytics/dashboard",
                        "correlationId", "g10-g8-dashboard"));
    }

    private Content jsonContent(Schema<?> schema, String exampleName, Object exampleValue) {
        return new Content().addMediaType(APPLICATION_JSON, new MediaType()
                .schema(schema)
                .addExamples(exampleName, new Example().value(exampleValue)));
    }

    private Header correlationResponseHeader() {
        return new Header()
                .description("Correlation id retourne par la Gateway.")
                .schema(new StringSchema().example("g10-g8-dashboard"));
    }

    private Schema<?> apiErrorSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Format commun des erreurs emises par G10.");
        schema.addProperty("timestamp", new StringSchema().format("date-time"));
        schema.addProperty("status", new IntegerSchema().example(401));
        schema.addProperty("error", new StringSchema().example("Unauthorized"));
        schema.addProperty("code", new StringSchema().example("INVALID_TOKEN"));
        schema.addProperty("message", new StringSchema().example("JWT invalide ou expire"));
        schema.addProperty("path", new StringSchema().example("/api/v1/analytics/dashboard"));
        schema.addProperty("correlationId", new StringSchema().example("g10-g8-dashboard"));
        return schema;
    }

    private Schema<?> healthSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("status", new StringSchema().example("UP"));
        return schema;
    }

    private Schema<?> loginRequestSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("email", new StringSchema().format("email").example("admin.g10@sgitu.ma"));
        schema.addProperty("password", new StringSchema().format("password").example("Password123"));
        return schema;
    }

    private Schema<?> refreshRequestSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("refreshToken", new StringSchema().example("550e8400-e29b-41d4-a716-446655440000"));
        return schema;
    }

    private Schema<?> userRegistrationRequestSchema() {
        Schema<?> profile = new ObjectSchema();
        profile.addProperty("firstName", new StringSchema().example("Nouveau"));
        profile.addProperty("lastName", new StringSchema().example("Utilisateur"));
        profile.addProperty("phone", new StringSchema().example("+212600000000"));
        profile.addProperty("address", new StringSchema().example("Casablanca"));
        profile.addProperty("birthDate", new StringSchema().format("date").example("2000-01-01"));

        Schema<?> schema = new ObjectSchema();
        schema.addProperty("email", new StringSchema().format("email").example("new.user@sgitu.ma"));
        schema.addProperty("password", new StringSchema().format("password").example("Password123"));
        schema.addProperty("role", new StringSchema().example("ROLE_PASSENGER"));
        schema.addProperty("profile", profile);
        return schema;
    }

    private Schema<?> userUpdateRequestSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("email", new StringSchema().format("email").example("updated.user@sgitu.ma"));
        schema.addProperty("password", new StringSchema().format("password").example("OptionalPassword123"));
        schema.addProperty("profile", ref("Profile"));
        return schema;
    }

    private Schema<?> passwordChangeRequestSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("newPassword", new StringSchema().format("password").example("NewPassword123"));
        return schema;
    }

    private Schema<?> roleUpdateRequestSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("roles", arrayOf(new StringSchema().example("ROLE_OPERATOR")));
        return schema;
    }

    private Schema<?> userExistsResponseSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("exists", new Schema<Boolean>().type("boolean").example(true));
        return schema;
    }

    private Schema<?> profileSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("firstName", new StringSchema().example("Nouveau"));
        schema.addProperty("lastName", new StringSchema().example("Utilisateur"));
        schema.addProperty("phone", new StringSchema().example("+212600000000"));
        schema.addProperty("address", new StringSchema().example("Casablanca"));
        schema.addProperty("birthDate", new StringSchema().format("date").example("2000-01-01"));
        return schema;
    }

    private Schema<?> userResponseSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("id", new IntegerSchema().example(1));
        schema.addProperty("email", new StringSchema().format("email").example("new.user@sgitu.ma"));
        schema.addProperty("active", new Schema<Boolean>().type("boolean").example(true));
        schema.addProperty("roles", arrayOf(new StringSchema().example("ROLE_PASSENGER")));
        schema.addProperty("profile", ref("Profile"));
        schema.addProperty("createdAt", new StringSchema().format("date-time").example("2026-05-08T10:00:00"));
        return schema;
    }

    private Schema<?> loginResponseSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Reponse retournee par G3 apres authentification.");
        schema.addProperty("token", new StringSchema().example("eyJhbGciOiJIUzI1NiJ9..."));
        schema.addProperty("refreshToken", new StringSchema().example("550e8400-e29b-41d4-a716-446655440000"));
        schema.addProperty("userId", new IntegerSchema().example(1));
        schema.addProperty("email", new StringSchema().format("email").example("admin.g10@sgitu.ma"));
        schema.addProperty("roles", arrayOf(new StringSchema().example("ROLE_ADMIN")));
        return schema;
    }

    private Schema<?> planAbonnementSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Plan d'abonnement G2 route via la Gateway.");
        schema.addProperty("idPlan", new IntegerSchema().format("int64").example(1));
        schema.addProperty("nomPlan", new StringSchema().example("Pass Mensuel Bus"));
        schema.addProperty("description", new StringSchema().example("Abonnement mensuel bus pour passagers"));
        schema.addProperty("prix", new NumberSchema().format("double").example(120.0));
        schema.addProperty("duree", new StringSchema().example("MENSUEL"));
        schema.addProperty("categorie", new StringSchema().example("ROLE_PASSENGER"));
        schema.addProperty("transportType", new StringSchema().example("BUS"));
        schema.addProperty("estActif", new StringSchema().example("ACTIF"));
        schema.addProperty("maxDesactivation", new IntegerSchema().example(2));
        schema.addProperty("minJoursEntreDesactivation", new IntegerSchema().example(7));
        schema.addProperty("maxPeriodeDesactivation", new IntegerSchema().example(15));
        return schema;
    }

    private Schema<?> g4LigneSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Ligne de transport G4 routee via la Gateway.");
        schema.addProperty("id", new IntegerSchema().format("int64").example(1));
        schema.addProperty("code", new StringSchema().example("G10-G4-DEMO"));
        schema.addProperty("nom", new StringSchema().example("Ligne integration G10 G4"));
        schema.addProperty("description", new StringSchema().example("Creee via Gateway"));
        schema.addProperty("active", new Schema<Boolean>().type("boolean").example(true));
        schema.addProperty("createdAt", new StringSchema().format("date-time").example("2026-06-01T08:00:00Z"));
        schema.addProperty("updatedAt", new StringSchema().format("date-time").example("2026-06-01T08:00:00Z"));
        return schema;
    }

    private Schema<?> g4MissionSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Mission G4 routee via la Gateway.");
        schema.addProperty("id", new IntegerSchema().format("int64").example(1));
        schema.addProperty("vehiculeId", new StringSchema().example("VH-G10-G4-001"));
        schema.addProperty("chauffeurId", new StringSchema().example("driver-demo"));
        schema.addProperty("ligneId", new IntegerSchema().format("int64").example(1));
        schema.addProperty("trajetId", new IntegerSchema().format("int64").example(1));
        schema.addProperty("affectationId", new IntegerSchema().format("int64").example(1));
        schema.addProperty("statut", new StringSchema().example("PLANIFIEE"));
        schema.addProperty("plannedStart", new StringSchema().format("date-time").example("2026-06-01T08:00:00Z"));
        schema.addProperty("notes", new StringSchema().example("Mission creee via Gateway"));
        return schema;
    }

    private Schema<?> g5NotificationRequestSchema() {
        Schema<?> recipient = new ObjectSchema();
        recipient.addProperty("userId", new StringSchema().example("501"));
        recipient.addProperty("email", new StringSchema().format("email").example("passenger.g5@sgitu.ma"));
        recipient.addProperty("phone", new StringSchema().example("+212600000000"));
        recipient.addProperty("deviceToken", new StringSchema().example("fcm-token-demo"));

        Schema<?> schema = new ObjectSchema()
                .description("Payload G5 pour envoyer une notification routee via la Gateway.");
        schema.addProperty("notificationId", new StringSchema().example("g10-g5-demo"));
        schema.addProperty("sourceService", new StringSchema().example("G10_GATEWAY"));
        schema.addProperty("eventType", new StringSchema().example("AUTH_LOGIN_SUCCESS"));
        schema.addProperty("channel", new StringSchema().example("LOG"));
        schema.addProperty("priority", new StringSchema().example("NORMAL"));
        schema.addProperty("recipient", recipient);
        schema.addProperty("metadata", new MapSchema().example(map(
                "email", "passenger.g5@sgitu.ma",
                "source", "Swagger G10-G5")));
        return schema;
    }

    private Schema<?> g5NotificationResponseSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Reponse G5 apres acceptation ou consultation d'une notification.");
        schema.addProperty("notificationId", new StringSchema().example("g10-g5-demo"));
        schema.addProperty("status", new StringSchema().example("QUEUED"));
        schema.addProperty("message", new StringSchema().example("Notification prise en charge"));
        schema.addProperty("channel", new StringSchema().example("LOG"));
        schema.addProperty("queuedAt", new StringSchema().format("date-time").example("2026-05-31T12:00:00"));
        schema.addProperty("currentStatus", new StringSchema().example("PENDING"));
        schema.addProperty("originalSourceService", new StringSchema().example("G10_GATEWAY"));
        return schema;
    }

    private Schema<?> g7VehiculeSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Vehicule G7 route via la Gateway.");
        schema.addProperty("id", new StringSchema().format("uuid").example("53c31262-591a-44d4-8872-51e84611ac5e"));
        schema.addProperty("immatriculation", new StringSchema().example("BUS-G10-G7-DEMO"));
        schema.addProperty("type", new StringSchema().example("BUS"));
        schema.addProperty("ligne", new StringSchema().example("L1"));
        schema.addProperty("statut", new StringSchema().example("DISPONIBLE"));
        schema.addProperty("conducteurId", new StringSchema().format("uuid").example("550e8400-e29b-41d4-a716-446655440000"));
        return schema;
    }

    private Schema<?> g7PositionSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Position GPS G7 routee via la Gateway.");
        schema.addProperty("id", new StringSchema().format("uuid").example("98de3c7b-73a2-44a3-9020-0f760bfb3fd1"));
        schema.addProperty("vehiculeId", new StringSchema().format("uuid").example("53c31262-591a-44d4-8872-51e84611ac5e"));
        schema.addProperty("latitude", new NumberSchema().format("double").example(36.7372));
        schema.addProperty("longitude", new NumberSchema().format("double").example(3.0865));
        schema.addProperty("vitesse", new NumberSchema().format("double").example(45.5));
        schema.addProperty("cap", new NumberSchema().format("double").example(180.0));
        schema.addProperty("timestamp", new StringSchema().format("date-time").example("2026-06-01T08:00:00Z"));
        return schema;
    }

    private Schema<?> batchIngestionResponseSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("totalReceived", new IntegerSchema().example(1));
        schema.addProperty("totalAccepted", new IntegerSchema().example(1));
        schema.addProperty("totalRejected", new IntegerSchema().example(0));
        schema.addProperty("rejectedReasons", arrayOf(new StringSchema().example("Event 1: Missing required timestamp")));
        schema.addProperty("status", new StringSchema().example("SUCCESS"));
        return schema;
    }

    private Schema<?> incomingEventSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Evenement metier brut accepte par G8. Les champs varient selon la source.");
        schema.addProperty("timestamp", new StringSchema().format("date-time").example("2026-05-08T10:00:00Z"));
        schema.addProperty("sourceId", new StringSchema().example("TCK-001"));
        schema.addProperty("userId", new StringSchema().example("USR-001"));
        schema.addProperty("status", new StringSchema().example("validated"));
        schema.addProperty("line", new StringSchema().example("L1"));
        schema.addProperty("zone", new StringSchema().example("Z_CENTER"));
        schema.addProperty("amount", new NumberSchema().format("double").example(25.50));
        return schema;
    }

    private Schema<?> statSnapshotSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("id", new StringSchema().example("69fd213900fb283c5ffe91c6"));
        schema.addProperty("snapshotType", new StringSchema().example("TRIPS"));
        schema.addProperty("statId", new StringSchema().example("FREQ_TOTAL_VALIDATIONS"));
        schema.addProperty("period", new StringSchema().example("2026-05-08"));
        schema.addProperty("granularity", new StringSchema().example("DAY"));
        schema.addProperty("value", new NumberSchema().format("double").example(120.0));
        schema.addProperty("metadata", new MapSchema());
        schema.addProperty("computedAt", new StringSchema().format("date-time").example("2026-05-08T00:24:03.017"));
        schema.addProperty("prediction", new Schema<Boolean>().type("boolean").example(false));
        return schema;
    }

    private Schema<?> reportRequestSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("period", new StringSchema().example("2026-05-08"));
        schema.addProperty("types", arrayOf(new StringSchema().example("TRIPS")));
        return schema;
    }

    private Schema<?> reportSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("id", new StringSchema().example("69fd213900fb283c5ffe91ff"));
        schema.addProperty("period", new StringSchema().example("2026-05-08"));
        schema.addProperty("requestedTypes", arrayOf(new StringSchema().example("TRIPS")));
        schema.addProperty("snapshots", arrayOf(ref("StatSnapshot")));
        schema.addProperty("generatedAt", new StringSchema().format("date-time").example("2026-05-08T00:24:03.017"));
        return schema;
    }

    private Schema<?> peakHoursPredictionRequestSchema() {
        Schema<?> dataPoint = new ObjectSchema();
        dataPoint.addProperty("hour", new IntegerSchema().example(17));
        dataPoint.addProperty("validationCount", new IntegerSchema().example(180));

        Schema<?> schema = new ObjectSchema();
        schema.addProperty("data", arrayOf(dataPoint));
        return schema;
    }

    private Schema<?> peakHoursPredictionResponseSchema() {
        Schema<?> distributionPoint = new ObjectSchema();
        distributionPoint.addProperty("hour", new IntegerSchema().example(17));
        distributionPoint.addProperty("score", new NumberSchema().format("double").example(0.3913));

        Schema<?> schema = new ObjectSchema();
        schema.addProperty("predicted_peak_hours", arrayOf(new IntegerSchema().example(17)));
        schema.addProperty("distribution", arrayOf(distributionPoint));
        schema.addProperty("generatedAt", new StringSchema().format("date-time").example("2026-05-08T00:24:38.356034"));
        return schema;
    }

    private Schema<?> incidentPredictionRequestSchema() {
        Schema<?> dataPoint = new ObjectSchema();
        dataPoint.addProperty("zone", new StringSchema().example("Z_CENTER"));
        dataPoint.addProperty("incidentCount", new IntegerSchema().example(5));
        dataPoint.addProperty("severity", new StringSchema().example("HIGH"));

        Schema<?> schema = new ObjectSchema();
        schema.addProperty("data", arrayOf(dataPoint));
        return schema;
    }

    private Schema<?> incidentPredictionResponseSchema() {
        Schema<?> riskZone = new ObjectSchema();
        riskZone.addProperty("zone", new StringSchema().example("Z_CENTER"));
        riskZone.addProperty("riskScore", new NumberSchema().format("double").example(1.0));
        riskZone.addProperty("riskLevel", new StringSchema().example("HIGH"));

        Schema<?> schema = new ObjectSchema();
        schema.addProperty("at_risk_zones", arrayOf(riskZone));
        schema.addProperty("generatedAt", new StringSchema().format("date-time").example("2026-05-08T00:24:39.120000"));
        return schema;
    }

    private Schema<?> ref(String schemaName) {
        return new Schema<>().$ref("#/components/schemas/" + schemaName);
    }

    private ArraySchema arrayOf(Schema<?> itemSchema) {
        return new ArraySchema().items(itemSchema);
    }

    private Object ingestionExample(String source) {
        return switch (source) {
            case "incidents" -> List.of(map(
                    "incidentId", "INC-POSTMAN-001",
                    "type", "breakdown",
                    "line", "L1",
                    "zone", "Z_CENTER",
                    "timestamp", "2026-05-08T10:00:00Z",
                    "severity", "HIGH",
                    "resolutionMinutes", 25));
            case "payments" -> List.of(map(
                    "transactionId", "TXN-POSTMAN-001",
                    "status", "completed",
                    "line", "L1",
                    "timestamp", "2026-05-08T10:00:00Z",
                    "amount", 25.50,
                    "method", "CARD"));
            case "subscriptions" -> List.of(map(
                    "userId", "USR-POSTMAN-SUB",
                    "action", "created",
                    "planType", "MONTHLY_STUDENT",
                    "timestamp", "2026-05-08T10:00:00Z"));
            case "vehicles" -> List.of(map(
                    "vehicleId", "BUS-POSTMAN-001",
                    "status", "in_service",
                    "line", "L1",
                    "zone", "Z_CENTER",
                    "timestamp", "2026-05-08T10:00:00Z",
                    "speed", 42.5,
                    "occupancy", 80,
                    "delayMinutes", 0));
            case "users" -> List.of(map(
                    "userId", "USR-POSTMAN-MOBILE",
                    "action", "active",
                    "timestamp", "2026-05-08T10:00:00Z",
                    "deviceOS", "Android"));
            default -> List.of(map(
                    "ticketId", "TCK-POSTMAN-001",
                    "userId", "USR-POSTMAN-001",
                    "status", "validated",
                    "line", "L1",
                    "stationId", "ST-05",
                    "timestamp", "2026-05-08T10:00:00Z",
                    "scanType", "NFC"));
        };
    }

    private Map<String, Object> userExample() {
        return userExample(true);
    }

    private Map<String, Object> userExample(boolean active) {
        return map(
                "id", 1,
                "email", "new.user@sgitu.ma",
                "active", active,
                "roles", List.of("ROLE_PASSENGER"),
                "profile", map(
                        "firstName", "Nouveau",
                        "lastName", "Utilisateur",
                        "phone", "+212600000000",
                        "address", "Casablanca",
                        "birthDate", "2000-01-01"),
                "createdAt", "2026-05-08T10:00:00");
    }

    private Map<String, Object> userUpdateExample() {
        return map(
                "email", "updated.user@sgitu.ma",
                "profile", map(
                        "firstName", "Updated",
                        "lastName", "Utilisateur",
                        "phone", "+212611111111",
                        "address", "Rabat",
                        "birthDate", "2000-01-01"));
    }

    private Map<String, Object> g1TicketCreateExample() {
        return map(
                "tripId", "MISSION-001",
                "holderId", "101",
                "price", 7.5,
                "currency", "MAD",
                "ticketType", "ONE_WAY",
                "ticketClass", "ORDINARY",
                "identityMethod", "QR_CODE",
                "rawPayload", "qr-g10-g1-demo",
                "expiresAt", "2026-05-09T10:00:00Z",
                "metadata", map("source", "SWAGGER_G10_G1"));
    }

    private Map<String, Object> g1TicketExample() {
        return map(
                "id", "TCK-001",
                "tripId", "MISSION-001",
                "holderId", "101",
                "price", 7.5,
                "currency", "MAD",
                "ticketType", "ONE_WAY",
                "ticketClass", "ORDINARY",
                "identityMethod", "QR_CODE",
                "status", "CREATED",
                "tokenValue", "qr-g10-g1-demo",
                "expiresAt", "2026-05-09T10:00:00Z");
    }

    private Map<String, Object> planExample() {
        return map(
                "nomPlan", "Pass Mensuel Bus",
                "description", "Abonnement mensuel bus pour passagers",
                "prix", 120.0,
                "duree", "MENSUEL",
                "categorie", "ROLE_PASSENGER",
                "transportType", "BUS",
                "estActif", "ACTIF",
                "maxDesactivation", 2,
                "minJoursEntreDesactivation", 7,
                "maxPeriodeDesactivation", 15);
    }

    private Map<String, Object> g4LineExample() {
        return map(
                "id", 1,
                "code", "G10-G4-DEMO",
                "nom", "Ligne integration G10 G4",
                "description", "Creee via Gateway pour tester la coordination",
                "active", true);
    }

    private Map<String, Object> g4MissionExample() {
        return map(
                "id", 1,
                "vehiculeId", "VH-G10-G4-001",
                "chauffeurId", "driver-demo",
                "ligneId", 1,
                "statut", "PLANIFIEE",
                "plannedStart", "2026-06-01T08:00:00Z",
                "notes", "Mission creee via Gateway");
    }

    private Map<String, Object> g5NotificationRequestExample() {
        return map(
                "notificationId", "g10-g5-demo",
                "sourceService", "G10_GATEWAY",
                "eventType", "AUTH_LOGIN_SUCCESS",
                "channel", "LOG",
                "priority", "NORMAL",
                "recipient", map(
                        "userId", "501",
                        "email", "passenger.g5@sgitu.ma"),
                "metadata", map(
                        "email", "passenger.g5@sgitu.ma",
                        "source", "Swagger G10-G5"));
    }

    private Map<String, Object> g5NotificationResponseExample() {
        return map(
                "notificationId", "g10-g5-demo",
                "status", "QUEUED",
                "message", "Notification prise en charge",
                "channel", "LOG",
                "queuedAt", "2026-05-31T12:00:00");
    }

    private Map<String, Object> g6PaymentAccountExample() {
        return map(
                "id", 1,
                "userId", 1,
                "paymentMethod", "CARD",
                "paymentToken", "CARD-TOKEN-001",
                "maskedIdentifier", "****0366",
                "provider", "VISA",
                "balance", 1000.0,
                "status", "ACTIVE",
                "expiryMonth", 12,
                "expiryYear", 2027);
    }

    private Map<String, Object> g6PaymentRequestExample() {
        return map(
                "userId", 6,
                "sourceType", "TICKET",
                "sourceId", 1001,
                "amount", 10.0,
                "paymentMethod", "CARD",
                "savedPaymentToken", "CARD-TOKEN-006",
                "email", "passenger.g6@sgitu.ma",
                "description", "Paiement test via G10 vers G6");
    }

    private Map<String, Object> g6PaymentResponseExample(String status) {
        boolean success = "SUCCESS".equals(status);
        return map(
                "paymentId", 12,
                "transactionToken", "TXN-2026-DEMO",
                "status", status,
                "message", success ? "Paiement valide avec succes" : "Paiement echoue",
                "invoiceId", success ? 1 : null,
                "invoiceNumber", success ? "INV-2026-0001" : null,
                "failureReason", success ? null : "INVALID_TOKEN");
    }

    private Map<String, Object> g6PaymentDetailsExample() {
        return map(
                "paymentId", 1,
                "userId", 1,
                "sourceType", "TICKET",
                "sourceId", 101,
                "amount", 25.0,
                "paymentMethod", "CARD",
                "savedPaymentToken", "CARD-TOKEN-001",
                "status", "SUCCESS",
                "transactionToken", "TXN-2026-DEMO",
                "invoiceId", 1,
                "invoiceNumber", "INV-2026-0001");
    }

    private Map<String, Object> g7VehicleExample() {
        return map(
                "id", "53c31262-591a-44d4-8872-51e84611ac5e",
                "immatriculation", "BUS-G10-G7-DEMO",
                "type", "BUS",
                "ligne", "L1",
                "statut", "DISPONIBLE",
                "conducteurId", "550e8400-e29b-41d4-a716-446655440000");
    }

    private Map<String, Object> g7PositionExample() {
        return map(
                "id", "98de3c7b-73a2-44a3-9020-0f760bfb3fd1",
                "vehiculeId", "53c31262-591a-44d4-8872-51e84611ac5e",
                "latitude", 36.7372,
                "longitude", 3.0865,
                "vitesse", 45.5,
                "cap", 180.0,
                "timestamp", "2026-06-01T08:00:00Z");
    }

    private Map<String, Object> statSnapshotExample() {
        return map(
                "id", "69fd213900fb283c5ffe91c6",
                "snapshotType", "TRIPS",
                "statId", "FREQ_TOTAL_VALIDATIONS",
                "period", "2026-05-08",
                "granularity", "DAY",
                "value", 120.0,
                "metadata", map("data", map("total_validations", 120)),
                "computedAt", "2026-05-08T00:24:03.017",
                "prediction", false);
    }

    private Map<String, Object> reportExample() {
        return map(
                "id", "69fd213900fb283c5ffe91ff",
                "period", "2026-05-08",
                "requestedTypes", List.of("TRIPS", "REVENUE"),
                "snapshots", List.of(statSnapshotExample()),
                "generatedAt", "2026-05-08T00:24:03.017");
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private String httpReason(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 503 -> "Service Unavailable";
            default -> "Internal Server Error";
        };
    }
}
