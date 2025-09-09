# ---- build stage ----
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /src

# Show Maven/Java version for debugging
RUN mvn -v && java -version

# Copy POM first to leverage cache
COPY pom.xml .
# Warm dependency cache (no -q so we can see errors)
RUN mvn -B -DskipTests dependency:go-offline --no-transfer-progress

# Copy the sources
COPY src ./src

# Build (verbose, no tests). If it fails, print useful logs and exit 1.
RUN mvn -B -DskipTests clean package -e --no-transfer-progress || \
  (echo '--- MAVEN BUILD FAILED ---'; \
   echo 'Contents of target/ (if any):'; ls -lah target || true; \
   echo 'Surefire reports:'; ls -lah target/surefire-reports || true; \
   cat target/surefire-reports/*.txt 2>/dev/null || true; \
   exit 1)

# Normalize artifact name -> target/app.jar (handles any jar name)
RUN set -e; \
  JAR_PATH="$(ls -1 target/*.jar | head -n1)"; \
  echo "Artifact detected: ${JAR_PATH}"; \
  mv "${JAR_PATH}" target/app.jar

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/app.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
