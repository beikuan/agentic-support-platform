# Demo image keeps Vaadin's generated frontend and Node runtime available.
FROM maven:3.9-eclipse-temurin-21
WORKDIR /app
COPY . .
RUN mvn -B test package
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/agentic-support-platform-1.0-SNAPSHOT.jar"]
