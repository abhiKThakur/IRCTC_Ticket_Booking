# ==========================================
# Stage 1: Build the Spring Boot application
# ==========================================
FROM gradle:9.5.1-jdk25 AS build

WORKDIR /workspace

COPY . .

RUN gradle :app:bootJar --no-daemon

# ==========================================
# Stage 2: Run the application
# ==========================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the executable Spring Boot JAR.
COPY --from=build /workspace/app/build/libs/ /tmp/build/

RUN find /tmp/build -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} /app/app.jar \; \
    && test -f /app/app.jar \
    && rm -rf /tmp/build

# Copy the JSON data files required by the application.
COPY --from=build /workspace/app/src/main/java/com/irctc/localDB/ /app/src/main/java/com/irctc/localDB/

# Render supplies the PORT environment variable.
EXPOSE 8080

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]