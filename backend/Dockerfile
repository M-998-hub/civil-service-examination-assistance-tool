# 该镜像需要依赖的基础镜像
FROM eclipse-temurin:8-jre-alpine
# 将当前目录下的jar包复制到docker容器的/目录下
ARG JAR_FILE=civil-service-assistance-api-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar
# 声明服务运行在8080端口
EXPOSE 8080
# 指定docker容器启动时运行jar包
ENTRYPOINT ["java", "-jar", "/app.jar"]
# 指定维护者的名字
LABEL org.opencontainers.image.title="Civil Service Examination Assistance Tool"
