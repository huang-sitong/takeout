FROM eclipse-temurin:17-jre
COPY sentinel-dashboard-1.8.9.jar /app.jar
EXPOSE 8858
ENV JAVA_OPTS="-Dserver.port=8858 -Dcsp.sentinel.dashboard.server=localhost:8858 -Dproject.name=sentinel-dashboard"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]