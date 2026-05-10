# Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

# Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S sgitu && adduser -S g4 -G sgitu
COPY --from=build /app/target/service-coordination-transport-0.0.1-SNAPSHOT.jar app.jar
RUN chown g4:sgitu app.jar
USER g4
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
