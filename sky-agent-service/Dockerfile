FROM eclipse-temurin:21-jre
WORKDIR /app

ARG JAR_FILE=target/sky-agent-service-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8087

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
