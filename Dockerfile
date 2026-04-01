FROM openjdk:21-slim
WORKDIR /app
COPY target/ai-agent-system-*.jar app.jar
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
EXPOSE 8123
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]