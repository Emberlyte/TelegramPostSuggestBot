FROM maven:3.9.9-eclipse-temurin-21-noble AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests



FROM openjdk:21

WORKDIR /app

EXPOSE $PORT

COPY --from=builder /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=server
ENTRYPOINT ["java", "-jar", "app.jar"]