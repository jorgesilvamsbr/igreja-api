FROM ubuntu:latest AS build
LABEL authors="rgsilva"

RUN apt-get update && apt-get install -y openjdk-21-jdk
COPY . .

RUN apt-get install maven -y
RUN mvn clean install

FROM amazoncorretto:21-alpine

EXPOSE 8080

COPY --from=build /target/api-igreja-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ENTRYPOINT ["java", "-jar", "app.jar"]
