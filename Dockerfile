FROM maven:3.9.9-eclipse-temurin-21-alpine@sha256:5a8b906c4faa11d33f6c74758f67db8eac25441e14b0729f6d50ff78992be58a AS build

WORKDIR /app

COPY pom.xml .

COPY VERSIONFILE ./VERSIONFILE

RUN mvn dependency:go-offline -B -Djavafx.platform=linux

COPY src ./src

RUN mvn clean package -DskipTests -Djavafx.platform=linux

FROM eclipse-temurin:21.0.7_6-jre-alpine@sha256:8728e354e012e18310faa7f364d00185277dec741f4f6d593af6c61fc0eb15fd

WORKDIR /app

COPY --from=build /app/target/dot-org-redirector-*.jar app.jar

COPY --from=build /app/VERSIONFILE ./VERSIONFILE

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]