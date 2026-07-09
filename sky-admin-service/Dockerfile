FROM eclipse-temurin:17-jre
WORKDIR /app

# JAR 文件在宿主机上通过 mvn package 构建
ARG JAR_FILE=target/sky-admin-service-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
