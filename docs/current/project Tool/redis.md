# Redis 本地开发高频常用命令（适配你的灵感平台业务）
## 一、基础连接 & 全局
```bash
# 本地连接（带密码）
redis-cli -h 127.0.0.1 -p 6379 -a Redis@2026

# 登录后认证（忘记带-a参数）
AUTH Redis@2026

# 切换数据库（默认db0，业务可分库隔离）
SELECT 0

# 查看所有key
KEYS *
# 模糊匹配key（例：所有用户画像）
KEYS user_profile:*

# 清空当前库（测试用，生产禁止）
FLUSHDB
# 清空所有库
FLUSHALL
```

## 二、字符串 String（项目使用最多：JWT、计数、简单缓存）
### 业务场景：JWT令牌、AI调用次数、浏览/点赞临时计数、配置缓存
```bash
# 存值
SET jwt:user:1001 "eyJhbGcixxx"
# 存值+过期7天（JWT）
SET jwt:user:1001 "xxx" EX 604800

# 取值
GET jwt:user:1001

# 自增（浏览、点赞计数）
INCR hot:inspire:2001
# 指定步长自增
INCRBY hot:inspire:2001 5

# 删除key
DEL jwt:user:1001

# 设置过期时间（秒）
EXPIRE hot:inspire:2001 3600
# 查看剩余过期时间
TTL jwt:user:1001
```

## 三、Hash 哈希（用户画像、多字段热点数据）
场景：`user_profile:1001` 存储用户各分类兴趣权重
```bash
# 批量设置hash字段
HSET user_profile:1001 food 48 sport 12 movie 20

# 单个字段更新
HSET user_profile:1001 wear 30

# 获取单个字段
HGET user_profile:1001 food
# 获取全部字段+值
HGETALL user_profile:1001
# 只取多个指定字段
HMGET user_profile:1001 food movie

# 删除hash内某个属性
HDEL user_profile:1001 sport

# 获取hash字段数量
HLEN user_profile:1001
```

## 四、Set 集合（在线用户、分布式去重）
场景：在线用户集合、防止重复点赞临时校验
```bash
# 添加成员
SADD online_city:长沙 1001 1002 1003

# 判断用户是否在线
SISMEMBER online_city:长沙 1001

# 取出全部在线用户
SMEMBERS online_city:长沙

# 删除某个用户
SREM online_city:长沙 1001

# 获取在线人数
SCARD online_city:长沙
```

## 五、Sorted Set 有序集合 ZSet（同城热点灵感池，按热度排序）
场景：`hot:city:长沙:14` 14点同城灵感热度排行榜
```bash
# 添加 灵感ID + 热度分值
ZADD hot:city:长沙:14 980 2001
ZADD hot:city:长沙:14 860 2002

# 从高到低取前20条热点（首页推荐）
ZREVRANGE hot:city:长沙:14 0 19 WITHSCORES

# 获取单条灵感热度分数
ZSCORE hot:city:长沙:14 2001

# 热度值增加（新互动提升热度）
ZINCRBY hot:city:长沙:14 50 2001

# 删除过期低热度灵感
ZREM hot:city:长沙:14 2001
```

## 六、分布式锁（防重复点赞、并发发布）
```bash
# 加锁 key=lock:inspire:like:2001, 值=随机标识，过期10秒
SET lock:inspire:like:2001 uuid-xxx EX 10 NX

# 释放锁（Lua脚本防止误删其他线程锁，代码里封装，命令行仅演示）
DEL lock:inspire:like:2001
```

## 七、JSON 操作（Redis 6.2+，存储扩展json字段）
```bash
# 写入json
JSON.SET user:ext:1001 $ '{"sign":"热爱美食","gender":1}'
# 读取全部
JSON.GET user:ext:1001
# 读取单个属性
JSON.GET user:ext:1001 $.sign
```

## 八、运维/调试命令
```bash
# 查看redis信息（内存、连接、持久化）
INFO

# 查看大key（排查内存占用过高）
-- redis-cli进入后执行
MEMORY USAGE user_profile:1001

# 监控实时所有请求（调试接口缓存逻辑）
MONITOR
# 停止监控
Ctrl+C
```

## 九、项目配套业务Key命名规范（统一格式）
1. JWT登录：`jwt:user:{userId}`
2. 用户画像Hash：`user_profile:{userId}`
3. 同城热点ZSet：`hot:city:{城市}:{小时}`
4. 在线用户Set：`online_city:{城市}`
5. 点赞计数：`count:like:inspire:{inspireId}`
6. 分布式锁：`lock:业务标识:id`
7. AI调用次数限制：`ai:limit:user:{userId}`