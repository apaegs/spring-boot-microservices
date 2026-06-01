FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 appuser
COPY --chown=1001:1001 target/*.jar app.jar
USER 1001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
