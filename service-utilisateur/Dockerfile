# Étape 1 : Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Image finale légère
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/user-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
