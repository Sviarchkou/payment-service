FROM eclipse-temurin:21-jre-jammy
LABEL authors="Yauheni Sviarchkou"

WORKDIR /app
COPY build/libs/payment-service-0.0.1-SNAPSHOT.jar payment-service.jar

ENTRYPOINT ["java", "-jar", "payment-service.jar", "--spring.profiles.active=docker", "--server.address=0.0.0.0"]

