# Stage 1 : Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2 : Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S g7group && adduser -S g7user -G g7group
COPY --from=builder /app/target/*.jar app.jar
RUN chown g7user:g7group app.jar
USER g7user
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]