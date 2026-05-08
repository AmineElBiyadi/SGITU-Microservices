package com.agileflow.api_gateway.config;

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
                .addSchemas("LoginResponse", loginResponseSchema())
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
                .addPathItem("/auth/register", registerPath())
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
                                        "accessToken", "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken", "eyJhbGciOiJIUzI1NiJ9...",
                                        "tokenType", "Bearer")))
                        .addApiResponse("401", errorResponse("Identifiants invalides", "UNAUTHORIZED", 401)));

        return new PathItem().post(operation);
    }

    private PathItem registerPath() {
        Operation operation = publicOperation(
                "G3 - Auth routee",
                "registerViaGateway",
                "Register route vers G3",
                "Route publique de creation de compte, traitee par le service G3."
        ).requestBody(jsonRequest(
                "Informations de creation de compte",
                ref("LoginRequest"),
                "registerRequest",
                map("email", "new.user@sgitu.ma", "password", "Password123")))
                .responses(new ApiResponses()
                        .addApiResponse("201", jsonResponse(
                                "Compte cree par G3",
                                new MapSchema(),
                                "registerResponse",
                                map("message", "Compte cree avec succes")))
                        .addApiResponse("409", errorResponse("Compte deja existant", "CONFLICT", 409)));

        return new PathItem().post(operation);
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

                        Acces : JWT valide. Les roles USER, AGENT et ADMIN peuvent envoyer
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
                description + "\n\nAcces : ROLE_ADMIN ou ROLE_AGENT."
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
                "Genere un rapport G8 pour une periode et une liste de types de snapshots. Acces : ROLE_ADMIN ou ROLE_AGENT."
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
                "Retourne un rapport G8 deja genere. Acces : ROLE_ADMIN ou ROLE_AGENT."
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
                "Route Gateway vers G8 ML (`ml-service:5000`). Acces : ROLE_ADMIN ou ROLE_AGENT."
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
                "Route Gateway vers G8 ML (`ml-service:5000`). Acces : ROLE_ADMIN ou ROLE_AGENT."
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

    private Schema<?> loginResponseSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Reponse typique du service G3. Le format exact reste proprietaire de G3.");
        schema.addProperty("accessToken", new StringSchema().example("eyJhbGciOiJIUzI1NiJ9..."));
        schema.addProperty("refreshToken", new StringSchema().example("eyJhbGciOiJIUzI1NiJ9..."));
        schema.addProperty("tokenType", new StringSchema().example("Bearer"));
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
