FROM openjdk:17-slim

WORKDIR /app

COPY target/*.jar /app/app.jar

ENTRYPOINT ["java","-jar","app.jar"]