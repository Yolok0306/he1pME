FROM openjdk:17-slim

WORKDIR /app

COPY target/*.jar /app

ENTRYPOINT ["java","-jar","he1pME-2.0.0-SNAPSHOT.jar"]