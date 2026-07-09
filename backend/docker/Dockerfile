# 多阶段构建：编译 + 运行
# 通过 BUILD_MODULE 构建参数指定模块名称
ARG BUILD_MODULE

# === Stage 1: 编译 ===
FROM maven:3.9-eclipse-temurin-21 AS build
COPY settings.xml /root/.m2/settings.xml
ARG BUILD_MODULE
WORKDIR /build
COPY pom.xml .
COPY inspire-common/pom.xml inspire-common/
COPY inspire-mq/pom.xml inspire-mq/
COPY flink-jobs/pom.xml flink-jobs/
COPY inspire-auth/pom.xml inspire-auth/
COPY inspire-ai/pom.xml inspire-ai/
COPY inspire-core/pom.xml inspire-core/
COPY inspire-admin/pom.xml inspire-admin/
COPY inspire-search/pom.xml inspire-search/
COPY inspire-gateway/pom.xml inspire-gateway/

# 先下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -pl ${BUILD_MODULE} -am -q 2>/dev/null || true

COPY . .
RUN mvn package -DskipTests -pl ${BUILD_MODULE} -am -q

# === Stage 2: 运行 ===
FROM eclipse-temurin:21-jre
ARG BUILD_MODULE
WORKDIR /app
COPY --from=build /build/${BUILD_MODULE}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "rm -f ${LOGGING_FILE_NAME:-/logs/app.log} 2>/dev/null && exec java -jar app.jar"]
