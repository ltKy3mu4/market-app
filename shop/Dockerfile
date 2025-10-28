FROM eclipse-temurin:21-jre-alpine

WORKDIR /opt/app/
COPY target/shop-0.0.1.jar /opt/app/app.jar

EXPOSE 8080 8000
ENTRYPOINT ["java", "-jar", "app.jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000"]