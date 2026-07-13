# MySQL + Redis 持久化+定时备份完整商用方案
分两块：Redis 持久化配置（RDB+AOF）、MySQL 定时全量备份+Binlog增量备份，适配你 Docker Compose 环境，开发/生产两套配置区分。

# 一、Redis 持久化配置（防止容器重启丢失会话、缓存数据）
## 1. 持久化机制说明
- RDB：定时全量快照，恢复速度快；
- AOF：每条写操作追加日志，数据零丢失，两者同时开启（商用标准）。

## 2. docker-compose.yml redis 完整配置
```yaml
redis:
  image: redis:7-alpine
  container_name: inspire-redis
  ports:
    - "6379:6379"
  command: >
    redis-server
    --requirepass ${REDIS_PASSWORD}
    --appendonly yes
    --appendfsync everysec
    --save 60 1000
    --rdbcompression yes
    --dbfilename dump.rdb
    --dir /data
  volumes:
    # 持久化文件挂载宿主机，容器销毁数据不丢
    - ./redis-data:/data
  environment:
    REDIS_PASSWORD: ${REDIS_PASSWORD}
  restart: always
  healthcheck:
    test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
    interval: 10s
    timeout: 5s
    retries: 3
```
### 参数解释
1. `--appendonly yes`：开启AOF持久化
2. `--appendfsync everysec`：每秒刷盘，性能与安全平衡（生产推荐）
3. `--save 60 1000`：60秒内至少1000次写入，自动生成RDB快照
4. `--dir /data`：rdb、aof文件存放目录，挂载宿主机目录持久化

## 3. 额外定时备份脚本（商用必加）
仅持久化还不够，磁盘损坏会全部丢失，需要定时把 `/redis-data` 打包备份到其他目录/云存储。
新建 `redis-backup.sh`
```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
# 备份目录
BACKUP_DIR=./redis-backup
SOURCE=./redis-data
mkdir -p $BACKUP_DIR
# 打包快照
tar -zcvf $BACKUP_DIR/redis-$DATE.tar.gz $SOURCE
# 保留7天备份，自动清理旧文件
find $BACKUP_DIR -name "redis-*.tar.gz" -mtime +7 -delete
```
添加定时任务 crontab 每天凌晨2点执行
```cron
0 2 * * * /xxx/inspire-ai-preview/redis-backup.sh
```

## 4. 开发环境简化
本地开发可只开RDB，AOF可选；生产必须RDB+AOF双开。

---

# 二、MySQL 持久化 + 定时备份（MySql8.0）
分为两层：
1. 容器挂载数据目录，重启库表不丢失；
2. 定时mysqldump全量备份 + 开启binlog增量恢复（可回滚任意时间点）。

## 1. docker-compose mysql 基础持久化配置
```yaml
mysql:
  image: mysql:8.0-debian
  container_name: inspire-mysql
  ports:
    - "3306:3306"
  volumes:
    # 数据库实体文件持久化
    - ./mysql-data:/var/lib/mysql
    # 自定义my.cnf配置
    - ./mysql-conf/my.cnf:/etc/mysql/conf.d/my.cnf
    # 备份文件挂载
    - ./mysql-backup:/backup
  environment:
    MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PWD}
    MYSQL_DATABASE: inspire_ai
    TZ: Asia/Shanghai
  restart: always
  command:
    --default-authentication-plugin=mysql_native_password
  healthcheck:
    test: ["CMD", "mysqladmin", "-uroot", "-p${MYSQL_ROOT_PWD}", "ping"]
    interval: 10s
    timeout: 5s
    retries: 3
```

## 2. my.cnf 开启Binlog（增量备份核心）
路径 `./mysql-conf/my.cnf`
```ini
[mysqld]
server-id=1
# 开启binlog
log_bin=/var/lib/mysql/mysql-bin
binlog_format=ROW
# 日志保留7天
expire_logs_days=7
# 时区
default-time_zone='+8:00'
# 字符集
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
```
- binlog作用：全量备份丢失的数据，可基于binlog恢复到任意时间点，误删表/数据可回滚。

## 3. 定时全量备份脚本 mysql-backup.sh
```bash
#!/bin/bash
DATE=$(date +%Y%m%d)
DB_NAME=inspire_ai
DB_USER=root
DB_PWD=${MYSQL_ROOT_PWD}
BACKUP_DIR=./mysql-backup
mkdir -p $BACKUP_DIR
# 进入容器执行备份
docker exec inspire-mysql mysqldump \
-u$DB_USER -p$DB_PWD \
--single-transaction \
--routines \
--triggers \
--databases $DB_NAME > $BACKUP_DIR/$DB_NAME-$DATE.sql
# 压缩
gzip $BACKUP_DIR/$DB_NAME-$DATE.sql
# 自动删除7天前备份
find $BACKUP_DIR -name "$DB_NAME-*.sql.gz" -mtime +7 -delete
```
### 关键参数说明
`--single-transaction`：InnoDB无锁备份，不阻塞业务读写，生产必备。

## 4. 配置定时任务 crontab
每日凌晨3点自动备份数据库
```cron
0 3 * * * /xxx/inspire-ai-preview/mysql-backup.sh
```

## 5. 故障恢复操作演示
### 5.1 从全量备份恢复
```bash
# 解压
gunzip mysql-backup/inspire_ai-20260713.sql.gz
# 导入mysql
docker exec -i inspire-mysql mysql -uroot -p${MYSQL_ROOT_PWD} < mysql-backup/inspire_ai-20260713.sql
```
### 5.2 结合binlog增量恢复（误删数据急救）
1. 先恢复最近一次全量备份
2. 基于binlog文件，指定时间点恢复丢失数据
```bash
docker exec inspire-mysql mysqlbinlog --stop-datetime="2026-07-13 10:00:00" /var/lib/mysql/mysql-bin.000001 | docker exec -i inspire-mysql mysql -uroot -p${MYSQL_ROOT_PWD}
```

---

# 三、生产环境额外商用加固（必做）
1. **备份异地存储**
   本地打包后的备份文件，同步到云存储（阿里云OSS/腾讯COS），防止服务器硬盘全盘损坏。
2. **定期校验备份有效性**
   每周自动执行一次备份恢复测试，避免备份文件损坏无法使用。
3. **权限管控**
   redis、mysql密码全部使用环境变量注入，绝不写死yml/compose。
4. **备份告警**
   脚本执行失败发送钉钉/邮件告警，避免备份中断无人知晓。
5. **Redis主从+MySQL主从**
   单机持久化只能防重启；生产搭建主从集群，一台实例宕机另一台无缝接管。

# 四、开发环境简化方案
本地调试可以省略定时备份脚本，仅开启基础持久化挂载目录即可，不用配置crontab；
上线前必须补全定时备份、binlog、异地存储全套逻辑。