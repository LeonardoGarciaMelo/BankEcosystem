# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY bank-api/pom.xml      bank-api/
COPY payment-api/pom.xml   payment-api/
COPY antifraud-api/pom.xml antifraud-api/

RUN mvn dependency:go-offline -pl antifraud-api -am -q

COPY antifraud-api/src antifraud-api/src
RUN mvn package -pl antifraud-api -am -DskipTests -q

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM registry.access.redhat.com/ubi8/openjdk-21:1.20

COPY --chown=185 --from=builder /app/antifraud-api/target/quarkus-app/lib/     /deployments/lib/
COPY --chown=185 --from=builder /app/antifraud-api/target/quarkus-app/*.jar    /deployments/
COPY --chown=185 --from=builder /app/antifraud-api/target/quarkus-app/app/     /deployments/app/
COPY --chown=185 --from=builder /app/antifraud-api/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8081
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
ENTRYPOINT ["/opt/jboss/container/java/run/run-java.sh"]
