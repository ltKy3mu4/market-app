FROM eclipse-temurin:21-jre-alpine

WORKDIR /opt/app/
COPY target/auth-server.jar /opt/app/auth-server.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "auth-server.jar"]