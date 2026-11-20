# Build stage: compile and package the app.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew :app:bootJar -x test --no-daemon

# Run stage: only the JRE and the packaged jar, as a non-root user.
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home recall
USER recall
WORKDIR /home/recall
COPY --from=build /src/app/build/libs/*.jar app.jar
EXPOSE 8080
# Configure with environment variables: DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD, PORT.
# Do NOT set SPRING_PROFILES_ACTIVE=dev in production: it loads the demo data and demo login.
ENTRYPOINT ["java", "-jar", "app.jar"]
