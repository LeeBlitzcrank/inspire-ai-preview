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

# ===  Dockerfile 编译带 image_filter 模块的 Nginx ====
FROM nginx:stable
# 安装编译依赖
RUN apt update && apt install -y libgd-dev wget build-essential gcc make
# 下载源码
RUN wget https://nginx.org/download/nginx-1.26.1.tar.gz && tar zxf nginx-1.26.1.tar.gz
WORKDIR nginx-1.26.1
# 读取原版全部编译参数，保证路径、模块和官方镜像一致
FROM nginx:stable
RUN apt update && apt install -y libgd-dev wget build-essential gcc make
RUN wget https://nginx.org/download/nginx-1.26.1.tar.gz && tar zxf nginx-1.26.1.tar.gz
WORKDIR nginx-1.26.1
RUN nginx -V 2>&1 | grep configure | sed 's/.*configure arguments: //' > build_args
RUN echo " --with-http_image_filter_module" >> build_args
RUN ./configure $(cat build_args)
RUN make -j$(nproc) && make install
RUN rm -f /usr/sbin/nginx
RUN ln -s /usr/local/nginx/sbin/nginx /usr/sbin/nginx
# 上下文是 ./nginx，default.conf 和 Dockerfile 同级，直接COPY
COPY ./default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80