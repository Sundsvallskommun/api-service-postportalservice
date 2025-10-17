FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests -Dstyle.color=always \
    dependency:go-offline dependency:resolve-plugins

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests clean package

RUN cp target/*.jar app.jar


FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -u 10001 -r -s /usr/sbin/nologin appuser
COPY --from=build /app/app.jar /app/app.jar
RUN chown -R appuser:appuser /app
USER appuser

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
