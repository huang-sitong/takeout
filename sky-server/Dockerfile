FROM eclipse-temurin:17-jre
WORKDIR /app

# JAR 文件在宿主机上通过 mvn package 构建
ARG JAR_FILE=target/sky-server-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

# JDK 17+ 模块系统：放行 Seata 反射访问 java.lang.reflect.Proxy.h
ENV JAVA_OPTS="--add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
