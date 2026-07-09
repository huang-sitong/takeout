FROM eclipse-temurin:17-jre
WORKDIR /app

ARG JAR_FILE=target/sky-menu-service-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8084

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
