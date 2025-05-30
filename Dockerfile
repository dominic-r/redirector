FROM maven:3.9.9-eclipse-temurin-21-alpine@sha256:bca048b298f5cbcc6e2c966e4f0099245b4720464519dd0dc17e77769f15ca03 AS build

WORKDIR /app

COPY pom.xml .

COPY VERSIONFILE ./VERSIONFILE

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21.0.7_6-jre-alpine@sha256:8728e354e012e18310faa7f364d00185277dec741f4f6d593af6c61fc0eb15fd

WORKDIR /app

COPY --from=build /app/target/dot-org-redirector-*.jar app.jar

COPY --from=build /app/VERSIONFILE ./VERSIONFILE

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"] 