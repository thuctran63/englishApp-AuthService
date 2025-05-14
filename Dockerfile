# ---- Build Stage ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# ---- Run Stage ----
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]
