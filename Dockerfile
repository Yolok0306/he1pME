FROM maven:3.8.6-jdk-11

COPY src/main/ home/app/src/main/

COPY src/resources/ home/app/src/resources/

COPY pom.xml home/app/

WORKDIR home/app/

RUN mvn clean package

ENTRYPOINT ["java","-jar","target/he1pME-1.1.jar"]