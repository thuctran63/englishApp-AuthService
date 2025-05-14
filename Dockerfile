# ---- Build Stage với hỗ trợ Lombok đầy đủ ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Sao chép chỉ các file cấu hình gradle trước để tận dụng cache
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# Cài đặt các dependencies
RUN ./gradlew dependencies --no-daemon

# Sao chép toàn bộ mã nguồn
COPY . .

# Build với hỗ trợ đầy đủ cho annotation processing
RUN ./gradlew clean bootJar --no-daemon

# ---- Run Stage ----
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]