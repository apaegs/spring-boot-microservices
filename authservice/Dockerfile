FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 1001 appuser
WORKDIR /app
COPY target/*.jar app.jar
USER 1001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
