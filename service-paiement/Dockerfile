# =============================================================
# SGITU — Dockerfile Microservice G6 Paiement
# Multi-stage build optimisé
# =============================================================

# ── ÉTAPE 1 : BUILD ──────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copie pom.xml (cache Docker)
COPY pom.xml .

# Télécharge les dépendances
RUN mvn dependency:go-offline -B

# Copie le code source
COPY src ./src

# Build du JAR
RUN mvn clean package -DskipTests

# ── ÉTAPE 2 : IMAGE RUNTIME LÉGÈRE ──────────────────────────
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="G6-Paiement-SGITU"
LABEL service="payment-service"
LABEL version="1.0.0"

WORKDIR /app

# Créer utilisateur non-root (sécurité)
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup

# Copier le JAR depuis build
COPY --from=build /build/target/service-paiement-*.jar app.jar

# Permissions
RUN chown appuser:appgroup app.jar

# Utilisateur non-root
USER appuser

# Port exposé
EXPOSE 8086

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8086/health || exit 1

# Lancer l'application
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseContainerSupport", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]