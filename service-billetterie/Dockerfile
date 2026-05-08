FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw mvnw
COPY src/ src/

RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system appgroup && \
    adduser --system --ingroup appgroup --no-create-home appuser

COPY --from=build /workspace/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
