# 多阶段构建 - 编译阶段
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /build

# 复制pom文件（利用Docker缓存）
COPY pom.xml .
COPY like-admin/pom.xml like-admin/
COPY like-front/pom.xml like-front/
COPY like-common/pom.xml like-common/
COPY like-generator/pom.xml like-generator/

# 下载依赖（缓存层）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY . .

# 编译打包
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# 运行阶段 - 使用 Eclipse Temurin (OpenJDK 的替代品)
FROM eclipse-temurin:8-jre

LABEL maintainer="Museum Reading Team"
LABEL version="1.0.0"

WORKDIR /app

# 安装必要工具
RUN apt-get update && apt-get install -y --no-install-recommends     curl     && rm -rf /var/lib/apt/lists/*

# 从编译阶段复制jar包
COPY --from=builder /build/like-admin/target/like-admin-*.jar app.jar

# 创建日志和上传目录
RUN mkdir -p /app/logs /app/uploads

# 暴露端口
EXPOSE 8082

# JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s   CMD curl -f http://localhost:8082/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
