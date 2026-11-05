# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
# Optional Maven Central mirror (e.g. https://maven-central-eu.storage-download.googleapis.com/maven2)
ARG MAVEN_MIRROR_URL=
WORKDIR /build
RUN if [ -n "$MAVEN_MIRROR_URL" ]; then mkdir -p /root/.m2 && printf '<settings><mirrors><mirror><id>mirror</id><mirrorOf>central</mirrorOf><url>%s</url></mirror></mirrors></settings>' "$MAVEN_MIRROR_URL" > /root/.m2/settings.xml; fi
COPY pom.xml .
COPY src src
COPY WebContent WebContent
RUN --mount=type=cache,target=/root/.m2/repository mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/CSE545-SS.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.war"]
