# 灵思集 Flink 实时计算指南

## 一、架构概述

RocketMQ → Flink → Redis

- 任务1：用户画像计算  → Redis (profile:{userId})
- 任务2：同城热点聚合  → Redis (hot:{city}:{tag})

## 二、任务说明

### 2.1 用户画像 (UserProfileJob)

行为 → 按 userId 分组 → 按类型加权 → Redis Hash

权重规则：publish+10, collect+8, like+5, view+3, search+2

### 2.2 热点聚合 (HotAggregationJob)

行为 → 按 city+tag 聚合 → Redis 自增(600s TTL)

## 三、本地测试

### 3.1 启动数据源
```bash
nc -lk 9999
```

### 3.2 启动 Flink
```bash
cd backend/flink-jobs
MAVEN_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED" \
  mvn exec:java -Dexec.mainClass="com.inspire.platform.flink.InspireFlinkApp"
```

### 3.3 发送数据
```json
{"userId":1,"type":"like","tag":"美食","city":"长沙"}
{"userId":1,"type":"collect","tag":"美食","city":"长沙"}
{"userId":2,"type":"publish","tag":"穿搭","city":"上海"}
```

### 3.4 验证
```bash
docker exec my-redis redis-cli -a 123456 HGETALL "profile:1"
docker exec my-redis redis-cli -a 123456 KEYS "profile:*"
```

## 四、项目结构

```
flink-jobs/
├── pom.xml
└── src/main/java/.../flink/
    ├── InspireFlinkApp.java
    ├── model/UserBehavior.java
    └── job/
        ├── UserProfileJob.java
        └── HotAggregationJob.java
```

## 五、常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| InaccessibleObjectException | JDK 17+ 模块限制 | MAVEN_OPTS 加 --add-opens |
| InvalidTypesException | Lambda 泛型擦除 | 用 POJO 替代 Map<> |
| 操作名冲突 | 分流算子重名 | 加 .name() 唯一命名 |
| 连不上 Redis | 地址不对 | 检查容器名和端口 |

## 六、后续规划

- [ ] 对接 RocketMQ Source
- [ ] 7天权重衰减
- [ ] 推送匹配任务
