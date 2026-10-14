FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system currency && useradd --system --gid currency currency
COPY --from=build /app/target/currency-rpc.jar /app/currency-rpc.jar
ENV PORT=9090
EXPOSE 9090
USER currency
ENTRYPOINT ["java", "-jar", "/app/currency-rpc.jar"]
