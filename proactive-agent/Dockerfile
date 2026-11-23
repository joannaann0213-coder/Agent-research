# Builds both services and packs them into one image, so that only Docker is needed on the host.
# docker-compose.yml runs it; docs/en/tutorial.md explains how. The argument names the service to run:
#   docker run proactive-agent inbox-server.jar     or     docker run proactive-agent agent-server.jar

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
# The cache keeps the downloaded dependencies from one build to the next.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B --no-transfer-progress -DskipTests package \
 && cp inbox-server/target/inbox-server-*-exec.jar /inbox-server.jar \
 && cp agent-server/target/agent-server-*-exec.jar /agent-server.jar

FROM eclipse-temurin:21-jre
RUN useradd --no-create-home --uid 10001 app
WORKDIR /app
COPY --from=build /inbox-server.jar /agent-server.jar /app/
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar"]
CMD ["agent-server.jar"]
