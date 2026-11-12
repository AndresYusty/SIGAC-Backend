# ---- Build: Maven + JDK 21 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime: JRE 21 ----
FROM eclipse-temurin:21-jre

# curl for the healthcheck; Arial (msttcorefonts) so PDFs match the Word models.
# If the font download fails the image still builds and PDFs fall back to the embedded DejaVu Sans.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl fontconfig \
 && echo "ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true" | debconf-set-selections \
 && (apt-get install -y --no-install-recommends ttf-mscorefonts-installer || echo "WARN: msttcorefonts not installed") \
 && rm -rf /var/lib/apt/lists/*

RUN groupadd --system sigac && useradd --system --gid sigac --home /app sigac \
 && mkdir -p /app /var/sigac/storage && chown -R sigac:sigac /app /var/sigac
WORKDIR /app
COPY --from=build --chown=sigac:sigac /build/target/sigac-backend.jar app.jar
USER sigac

ENV SIGAC_STORAGE_PATH=/var/sigac/storage \
    TZ=America/Bogota \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=America/Bogota"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
