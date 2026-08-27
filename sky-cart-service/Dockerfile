FROM eclipse-temurin:21-jre
WORKDIR /app

ARG JAR_FILE=target/sky-cart-service-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8085

# JDK 17+ 模块系统：cart-service 已作为 Seata AT 分支参与者，放行 Seata 反射访问
ENV JAVA_OPTS="--add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
