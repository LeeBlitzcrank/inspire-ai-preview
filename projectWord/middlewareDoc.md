# 一、中间件集群部署文档 + 统一连接地址清单
## 环境区分：测试环境 / 生产环境
### 1. MySQL 主从读写分离部署文档
#### 集群拓扑
- Master（写入）：192.168.1.10:3306
- Slave1（读业务查询）：192.168.1.11:3306
- Slave2（Canal数据源+离线统计）：192.168.1.12:3306
  库名：`inspire_ai_preview`
  账号：
- 业务读写账号：`inspire_rw` / 密码 Inspire@2026
- Canal同步专用账号：`canal_sync` / 密码 Canal@Sync2026

#### mybatis-plus 数据源配置 application.yml
```yaml
spring:
  datasource:
    master:
      url: jdbc:mysql://192.168.1.10:3306/inspire_ai_preview?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
      username: inspire_rw
      password: Inspire@2026
    slave1:
      url: jdbc:mysql://192.168.1.11:3306/inspire_ai_preview?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
      username: inspire_rw
      password: Inspire@2026
```
#### 部署要点
1. Master开启binlog，格式 `ROW`，binlog-do-db=inspire_ai_preview
2. 从库开启只读，分担分页、详情查询压力
3. 每日凌晨2点全量备份 + binlog增量7天留存

### 2. Redis 3主3从哨兵集群部署文档
#### 集群地址
哨兵节点：192.168.1.20:26379,192.168.1.21:26379,192.168.1.22:26379
集群名称：`redis-sentinel-master`
密码：Redis@Inspire2026
#### 业务分片规划
1. 分片1：登录JWT、分布式锁
2. 分片2：用户画像、在线城市缓存
3. 分片3：热点灵感、浏览/点赞计数
#### SpringBoot连接配置
```yaml
spring:
  redis:
    sentinel:
      master: redis-sentinel-master
      nodes: 192.168.1.20:26379,192.168.1.21:26379,192.168.1.22:26379
    password: Redis@Inspire2026
    timeout: 3000ms
```
#### 持久化策略
- RDB 15分钟快照；AOF每秒刷盘，防止计数丢失

### 3. RocketMQ 部署文档
#### 集群地址
NameServer集群：192.168.1.30:9876;192.168.1.31:9876
Broker集群：4节点
生产/消费分组：
- 用户组：`inspire_user_group`
- 灵感发布组：`inspire_publish_group`
- 行为流消费组(Flink)：`flink_behavior_consumer`
#### SpringBoot连接配置
```yaml
rocketmq:
  name-server: 192.168.1.30:9876;192.168.1.31:9876
  producer:
    group: inspire_user_group
    send-message-timeout: 3000
```
#### 业务Topic清单
1. topic_user_register
2. topic_user_behavior
3. topic_inspire_publish
4. topic_push_msg
5. topic_stat_offline

### 4. Elasticsearch 集群部署文档
ES集群地址：http://192.168.1.40:9200,http://192.168.1.41:9200,http://192.168.1.42:9200
账号密码：elastic / Es@Inspire2026
索引名：`inspire_index`
分片3，副本1
#### SpringBoot ES配置
```yaml
spring:
  elasticsearch:
    uris: http://192.168.1.40:9200,http://192.168.1.41:9200
    username: elastic
    password: Es@Inspire2026
```

### 5. Flink 集群部署文档
JobManager：192.168.1.50:8081
TaskManager 4台：192.168.1.51~54
Checkpoint存储地址：hdfs://192.168.1.60/flink/checkpoint
状态保存间隔：5min
水印延迟：2min

### 6. Canal 部署地址
Canal服务地址：192.168.1.70:11111
instance名称：inspire_instance

# 二、Canal 完整配置文件
## 1. canal.properties 核心配置
```properties
canal.id = canal-01
canal.ip = 192.168.1.70
canal.port = 11111
canal.metrics.pull.port = 11112

# zk集群，用于HA
canal.zkServers = 192.168.1.30:2181,192.168.1.31:2181,192.168.1.32:2181
canal.zookeeper.flush.period = 1000

# 数据存储目录
canal.file.data.dir = ../data
canal.file.flush.period = 1000
canal.file.buffer.size = 16384
canal.file.log.max.index.files = 1000

# 网络参数
canal.serverMode = tcp
canal.detecting.sql = select 1

# 内存缓冲
canal.instance.global.buffer.size = 10240
canal.instance.global.batch.size = 1000
```

## 2. instances/inspire_instance/instance.properties 同步实例配置
```properties
## mysql master地址
canal.instance.master.address=192.168.1.10:3306
canal.instance.master.journal.name=
canal.instance.master.position=
canal.instance.master.timestamp=

# mysql账号（仅同步权限）
canal.instance.dbUsername=canal_sync
canal.instance.dbPassword=Canal@Sync2026
canal.instance.connectionCharset = UTF-8
canal.instance.enableDruid = false

# 过滤库表：只同步灵感主表
canal.instance.filter.regex = inspire_ai_preview\\.inspire_main
# 忽略删除同步（下架通过deleted字段逻辑删除，不物理删）
canal.instance.filter.black.regex = inspire_ai_preview\\.inspire_main:DELETE

# 目标es配置
canal.instance.es.addressList=http://192.168.1.40:9200,http://192.168.1.41:9200
canal.instance.es.userName=elastic
canal.instance.es.password=Es@Inspire2026
canal.instance.es.index = inspire_index
canal.instance.es.type = _doc

# 批量同步参数
canal.instance.es.bulkSize = 2000
canal.instance.es.flushInterval = 1000
canal.instance.es.backoff.millis = 1000
canal.instance.es.retry.max = 3
```

# 三、ES inspire_index 完整Mapping
## 执行命令：PUT http://192.168.1.40:9200/inspire_index
```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "1s",
    "analysis": {
      "analyzer": {
        "ik_max_word": {
          "type": "ik",
          "use_smart": false
        },
        "ik_smart": {
          "type": "ik",
          "use_smart": true
        }
      }
    }
  },
  "mappings": {
    "_doc": {
      "properties": {
        "id": {
          "type": "keyword"
        },
        "title": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart",
          "boost": 10
        },
        "img": {
          "type": "keyword"
        },
        "tag": {
          "type": "keyword"
        },
        "user_id": {
          "type": "keyword"
        },
        "view_count": {
          "type": "long"
        },
        "like_count": {
          "type": "integer"
        },
        "collect_count": {
          "type": "integer"
        },
        "heat": {
          "type": "integer"
        },
        "publishing_city": {
          "type": "keyword"
        },
        "create_time": {
          "type": "date",
          "format": "yyyy-MM-dd HH:mm:ss"
        },
        "deleted": {
          "type": "byte"
        }
      }
    }
  }
}
```
## Mapping说明
1. title分词检索，权重10，搜索优先级高于其他文本；
2. tag、city、id使用keyword精确过滤；
3. deleted字段用于过滤已下架灵感，搜索时统一增加 `{"term":{"deleted":0}}`；
4. heat热度字段用于排序，优先展示高热度灵感。

# 四、Canal同步数据映射规则
MySQL `inspire_main` 字段 → ES `inspire_index` 一一对应：
1. id → id
2. title → title
3. img → img
4. tag → tag
5. user_id → user_id
6. view_count → view_count
7. like_count → like_count
8. collect_count → collect_count
9. heat → heat
10. publishing_city → publishing_city
11. create_time → create_time
12. deleted → deleted

附表 inspire_content 不同步ES，仅前端详情页JOIN查询展示正文。