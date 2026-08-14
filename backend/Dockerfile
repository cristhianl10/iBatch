FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S ibatch && adduser -S ibatch -G ibatch
WORKDIR /app
COPY --from=build /workspace/target/ibatch-backend-*.jar app.jar
RUN mkdir -p /data/input && chown -R ibatch:ibatch /app /data
USER ibatch
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
