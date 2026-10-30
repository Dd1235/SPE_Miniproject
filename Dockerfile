# ---------------------------------------------------------------------------
# Stage 1 - build: full JDK + Maven, compiles the source into a runnable JAR.
# --platform=$BUILDPLATFORM runs this stage natively even when cross-building
# (e.g. for linux/amd64 on an ARM Mac); a JAR is platform-independent anyway.
# ---------------------------------------------------------------------------
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Dependencies first: this layer is cached until pom.xml changes,
# so editing Java code does not re-download everything.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 - runtime: JRE only (no compiler, Maven or source code).
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Unprivileged system user without a password, home dir or login shell
RUN addgroup -S app && adduser -S -G app -H -s /sbin/nologin app

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/calculator-api.jar app.jar
USER app

EXPOSE 8080

# Size the heap from the container's memory limit; exit (and let Docker restart us) on OOM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

# Exec form: java runs as PID 1 and receives SIGTERM directly, enabling graceful shutdown.
ENTRYPOINT ["java", "-jar", "app.jar"]
