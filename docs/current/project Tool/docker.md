# Docker 全套常用命令（Mac/Linux 通用，适配你 ES、SpringBoot 项目场景）
## 一、环境启停 & 信息查看
```bash
# 打开 Docker Desktop（Mac）
open /Applications/Docker.app

# 查看 Docker 服务信息
docker info

# 查看版本
docker -v
docker version
```

## 二、镜像 image 操作
```bash
# 列出本地所有镜像
docker images
# 只输出镜像ID（批量删除用）
docker images -q

# 拉取镜像
docker pull 镜像名:标签
# 示例拉 ES7.17.20
docker pull docker.elastic.co/elasticsearch/elasticsearch:7.17.20

# 构建镜像（当前目录Dockerfile）
docker build -t 镜像名:版本 .

# 删除单个镜像
docker rmi 镜像ID/镜像名
# 强制删除（被容器占用时）
docker rmi -f 镜像ID

# 批量删除所有镜像
docker rmi -f $(docker images -q)

# 清理无用悬空镜像 <none>:<none>
docker image prune
# 清理所有未使用镜像
docker image prune -a
```

## 三、容器 container 核心（你最常用）
### 1. 运行容器
```bash
# 后台启动容器 -d 守护进程
docker run -d --name 容器名 -p 宿主机端口:容器端口 镜像名
# ES 示例
docker run -d --name elasticsearch \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  docker.elastic.co/elasticsearch/elasticsearch:7.17.20

# 交互式启动（进入容器终端）
docker run -it --name test centos:7 /bin/bash
```

### 2. 查看容器
```bash
# 运行中的容器
docker ps
# 包含已停止的容器
docker ps -a
# 只输出容器ID
docker ps -aq
```

### 3. 启停/重启容器
```bash
# 启动已存在容器
docker start 容器名/ID
# 停止（优雅关闭）
docker stop 容器名/ID
# 强制杀死
docker kill 容器名/ID
# 重启
docker restart 容器名/ID
```

### 4. 删除容器
```bash
# 删除已停止容器
docker rm 容器ID/名称
# 强制删除运行中容器
docker rm -f 容器ID
# 批量删除所有容器（慎用）
docker rm -f $(docker ps -aq)
# 清理所有停止的容器
docker container prune
```

### 5. 进入运行中容器
```bash
docker exec -it 容器名 /bin/bash
# ES 进入示例
docker exec -it elasticsearch /bin/bash
```

### 6. 日志 & 资源监控
```bash
# 查看容器日志
docker logs 容器名
# 实时滚动日志（-f）
docker logs -f elasticsearch
# 只看最后200行
docker logs --tail 200 elasticsearch

# 查看容器CPU/内存占用
docker stats
```

## 四、端口、文件拷贝
```bash
# 宿主机文件 → 容器内
docker cp /本地路径 容器ID:/容器内路径
# 容器内文件 → 宿主机
docker cp 容器ID:/容器内文件 /本地目录
```

## 五、数据卷 Volume（持久化存储）
```bash
# 查看所有卷
docker volume ls
# 创建卷
docker volume create es-data
# 删除卷
docker volume rm es-data
# 清理未使用卷
docker volume prune
```

## 六、网络（多容器通信）
```bash
# 查看网络
docker network ls
# 创建自定义网络
docker network create my-net
# 容器连接网络
docker run --network my-net ...
```

## 七、导出/导入镜像（迁移）
```bash
# 导出镜像到本地文件
docker save -o es.tar docker.elastic.co/elasticsearch/elasticsearch:7.17.20
# 导入本地镜像包
docker load -i es.tar
```

## 八、一键清理全套垃圾（日常维护）
```bash
# 清理停止容器、无用镜像、无用卷、网络
docker system prune -a
```

## 高频场景速记（你项目常用）
1. 启动ES容器
```bash
docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "xpack.security.enabled=false" -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" docker.elastic.co/elasticsearch/elasticsearch:7.17.20
```
2. 查看ES实时日志
```bash
docker logs -f elasticsearch
```
3. 杀掉并删除ES容器重建
```bash
docker rm -f elasticsearch
```
4. 查看所有容器端口占用
```bash
docker ps
```