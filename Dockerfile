FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY pom.xml /workspace/pom.xml

RUN apt-get update && apt-get install -y maven && \
    mvn -f /workspace/pom.xml install -N -q && \
    mvn -f /workspace/pom.xml clean package -DskipTests -q

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]