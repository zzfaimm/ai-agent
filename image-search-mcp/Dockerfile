FROM registry.cn-hangzhou.aliyuncs.com/acs/openjdk:21-jdk
WORKDIR /app
COPY target/image-search-mcp-*.jar app.jar
ENV JAVA_OPTS="-Xmx256m -Xms128m"
EXPOSE 8127
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]