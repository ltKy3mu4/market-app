FROM eclipse-temurin:21-jre-alpine

WORKDIR /opt/app/
COPY target/my-market-app-0.0.1-SNAPSHOT.jar /opt/app/app.jar

EXPOSE 8080 8000
ENTRYPOINT ["java", "-jar", "app.jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000"]