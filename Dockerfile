FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY ["mvnw.cmd", "./mvnw.cmd"]
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:25
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
