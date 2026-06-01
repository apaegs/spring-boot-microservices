FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/*.jar app.jar
RUN useradd --system --uid 1001 appuser
USER 1001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
