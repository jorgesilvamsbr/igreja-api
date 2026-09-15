FROM ubuntu:latest AS build
LABEL authors="rgsilva"

# 1. Instalar o JDK e o Maven na mesma camada
RUN apt-get update && apt-get install -y openjdk-21-jdk maven

# 2. Copiar os arquivos do projeto
COPY . .

# 3. Executar o build
RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

EXPOSE 8080

COPY --from=build /target/api-igreja-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
