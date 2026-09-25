FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -B -pl agenda-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/agenda-service/target/agenda-service-2.0.0-SNAPSHOT.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
