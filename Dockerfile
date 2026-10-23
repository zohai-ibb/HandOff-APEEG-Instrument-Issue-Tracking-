# Step 1: Use official OpenJDK 17 base image
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Step 2: Copy maven wrapper and build files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Step 3: Build application package
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Step 4: Run application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]