FROM openjdk:21-slim
WORKDIR /app
COPY target/image-search-mcp-*.jar app.jar
ENV JAVA_OPTS="-Xmx256m -Xms128m"
EXPOSE 8127
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]