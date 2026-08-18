FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY backend/gradlew backend/gradlew.bat backend/
COPY backend/gradle backend/gradle
COPY backend/settings.gradle backend/build.gradle backend/
COPY backend/src backend/src

RUN chmod +x backend/gradlew \
  && backend/gradlew -p backend bootJar --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /workspace/backend/build/libs/*.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
