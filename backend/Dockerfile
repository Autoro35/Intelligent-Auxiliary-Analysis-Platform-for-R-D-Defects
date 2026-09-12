# ============================================================
# 后端镜像：多阶段构建
#   阶段一 maven 编译打包，阶段二仅保留 JRE 与 jar，镜像更小
# ============================================================

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# 先单独拷贝 pom 并预热依赖，源码变动时可复用这一层缓存
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre

WORKDIR /app

# 容器内时区设置为东八区，避免日志与时间字段出现 8 小时偏差
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=build /build/target/*.jar app.jar

# 附件本地存储目录，docker-compose 中以卷挂载持久化
RUN mkdir -p /app/uploads
VOLUME ["/app/uploads"]

EXPOSE 8080

# 依赖启动阶段无外网访问，先给 JVM 一个合理的堆上限
ENV JAVA_OPTS="-Xms256m -Xmx512m"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD bash -c 'cat < /dev/null > /dev/tcp/localhost/8080' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
