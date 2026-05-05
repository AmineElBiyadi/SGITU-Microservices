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

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]