---
title: RocketMQ 部署与测试指南
---

# 灵思集 - RocketMQ 部署与测试指南

## 一、部署 RocketMQ

### 1.1 启动 NameServer

```bash
docker run -d --name rmq-namesrv \
  -p 9876:9876 \
  apache/rocketmq:5.2.0 \
  sh mqnamesrv
```

### 1.2 创建 Broker 配置文件

```bash
mkdir -p ~/docker/rocketmq/conf
cat > ~/docker/rocketmq/conf/broker.conf << 'EOF'
namesrvAddr=host.docker.internal:9876
brokerIP1=host.docker.internal
brokerId=0
autoCreateTopicEnable=true
EOF
```

> **注意：** Docker Desktop for Mac 必须用 `host.docker.internal` 替代 `localhost`

### 1.3 启动 Broker

```bash
sleep 3
docker run -d --name rmq-broker \
  -p 10911:10911 -p 10909:10909 \
  -v ~/docker/rocketmq/conf/broker.conf:/home/rocketmq/rocketmq-5.2.0/conf/broker.conf \
  -e "NAMESRV_ADDR=host.docker.internal:9876" \
  apache/rocketmq:5.2.0 \
  sh mqbroker -c /home/rocketmq/rocketmq-5.2.0/conf/broker.conf

sleep 3
```

### 1.4 创建 Topic（必须手动创建）

```bash
# topic_user_register 注册事件
docker exec rmq-broker sh mqadmin updateTopic \
  -n host.docker.internal:9876 \
  -b localhost:10911 \
  -t topic_user_register

# topic_user_behavior 用户行为事件
docker exec rmq-broker sh mqadmin updateTopic \
  -n host.docker.internal:9876 \
  -b localhost:10911 \
  -t topic_user_behavior

# topic_inspire_publish 灵感发布事件
docker exec rmq-broker sh mqadmin updateTopic \
  -n host.docker.internal:9876 \
  -b localhost:10911 \
  -t topic_inspire_publish
```

### 1.5 验证部署

```bash
docker exec rmq-broker sh mqadmin topicList -n host.docker.internal:9876 | grep topic_user
# 应输出: topic_user_register / topic_user_behavior / topic_inspire_publish
```

## 二、重启后端服务

### 2.1 安装 MQ 模块到本地仓库

```bash
cd /Users/lee/Desktop/Project/Study\ Project/Codex/inspire-ai-preview/backend
/usr/local/bin/mvn install -pl inspire-mq -DskipTests
```

### 2.2 启动三个核心服务（三个终端）

```bash
# 终端 1 - 网关
/usr/local/bin/mvn spring-boot:run -pl inspire-gateway

# 终端 2 - 用户服务
/usr/local/bin/mvn spring-boot:run -pl inspire-auth

# 终端 3 - 灵感核心
/usr/local/bin/mvn spring-boot:run -pl inspire-core
```

启动时观察日志中出现以下两行即为 MQ 连接成功：

```
RocketMQ生产者启动成功: namesrv=host.docker.internal:9876
MQ消费者[consumer_publish]启动: topic=topic_inspire_publish
```

## 三、测试 MQ 消息

### 3.1 注册触发消息

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"mqdemo","password":"123456","confirmPassword":"123456","email":"mq@demo.com"}'
```

auth 服务日志应出现：
```
MQ发送成功: topic=topic_user_register
【topic_user_register】收到消息: {"userId":..., "username":"mqdemo"}
```

### 3.2 创建灵感触发消息

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"mqdemo","password":"123456"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

curl -s -X POST http://localhost:8080/api/inspire \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"MQ测试","content":"测试RocketMQ消息","tag":"美食","status":1}'
```

core 服务日志应出现：
```
MQ发送成功: topic=topic_inspire_publish
【topic_inspire_publish】收到消息: {"inspireId":..., "title":"MQ测试"}
```

## 四、常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| `connect to null failed` | 容器内 `localhost` 不通 | 全部使用 `host.docker.internal` |
| `No route info of this topic` | Topic 未自动创建 | 手动创建 topic |
| `BrokersSent: [docker-desktop]` | brokerIP1 配错了 | `brokerIP1=localhost` |
| `.m2 Operation not permitted` | Maven 缓存锁 | `rm -rf` 或重启电脑 |
| `rocketmq-client` 不存在 | `.m2` 缓存问题 | `mvn install -pl inspire-mq -DskipTests` |

## 六、消息 Topic 说明

| Topic | 发送时机 | 发送方 | 数据内容 |
|-------|---------|--------|---------|
| `topic_user_register` | 用户注册 | auth | `userId`, `username`, `email` |
| `topic_user_behavior` | 登录/收藏/点赞 | auth + core | `userId`, `type`, `inspireId` |
| `topic_inspire_publish` | 灵感发布 | core | `inspireId`, `userId`, `title`, `tag` |

> 后续接入 Flink 后，这些 Topic 将作为实时计算的输入源。

