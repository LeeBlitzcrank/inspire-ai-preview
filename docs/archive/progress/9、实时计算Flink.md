编译通过。测试方式：

```bash
# 终端 1：监听数据源（先启动）
nc -lk 9999

# 终端 2：启动 Flink（后启动）
cd /Users/lee/Desktop/Project/Study\ Project/Codex/inspire-ai-preview/backend/flink-jobs

MAVEN_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED" \
  /usr/local/bin/mvn exec:java -Dexec.mainClass="com.inspire.platform.flink.InspireFlinkApp"
```

Flink 启动后会连上 `localhost:9999`，然后在**终端 1** 输入一条数据回车：

```json
{"userId":1,"type":"like","tag":"美食","city":"长沙"}
```

Flink 终端实时输出：
```
up-parse > userId=1, tag=美食, +5
hot-parse > {city=长沙, tag=美食, count=+1}
```
```

---

**目前的 Flink 任务实现了：**

| 任务 | 输入 | 处理 | 输出 |
|------|------|------|------|
| `UserProfileJob` | 行为 JSON | 按 `type` 计算权重，累加到 Redis | `profile:{userId}` Hash |
| `HotAggregationJob` | 行为 JSON | 按城市+分类聚合，Redis 自增计数 | `hot:{city}:{tag}` 带 TTL |

权重规则（PRD 5.2）：
- 发布灵感 +10
- 收藏灵感 +8
- 点赞灵感 +5
- 浏览详情 +3
- 搜索关键词 +2



