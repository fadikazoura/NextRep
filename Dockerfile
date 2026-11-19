# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar with the Maven wrapper ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Build files first: dependencies are downloaded once and cached until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Tests need Docker (Testcontainers), so they run in CI, not inside this image build.
RUN ./mvnw -B -q package -DskipTests

# ---------- Stage 2: small runtime image with only a JRE and the jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Never run the app as root.
RUN groupadd --system nextrep && useradd --system --gid nextrep nextrep
COPY --from=build /workspace/target/*.jar app.jar
USER nextrep

EXPOSE 8080
# Size the heap from the container's memory limit instead of the host's.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
