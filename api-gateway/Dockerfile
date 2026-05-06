# ===== ÉTAPE 1 : BUILD =====
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copier pom.xml d'abord (optimisation cache Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler et packager
RUN mvn clean package -DskipTests

# ===== ÉTAPE 2 : RUNTIME =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copier le jar depuis l'étape build
COPY --from=build /app/target/*.jar app.jar

# Port exposé
EXPOSE 8080

# Créer un utilisateur non-root
RUN addgroup -S sgitu && adduser -S g10 -G sgitu
USER g10

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Lancer l'application avec le profil docker
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar", "--spring.profiles.active=docker"]