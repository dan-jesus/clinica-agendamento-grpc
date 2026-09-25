FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -B -pl cadastro-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/cadastro-service/target/cadastro-service-2.0.0-SNAPSHOT.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
