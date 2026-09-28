# Flink 全套常用命令（分三类：本地嵌入式不用命令、Standalone集群/Docker集群、Flink SQL）
## 前置说明
1. **嵌入式本地模式**：直接 IDE 运行 main，**不需要任何 flink 命令**；
2. 下面命令适用于 Standalone 集群（Docker 启动的 Flink 容器、服务器解压包）；
3. 进入 Flink 容器执行命令：
```bash
docker exec -it flink-local /bin/bash
```

## 一、集群启停命令（Standalone 原生）
```bash
# 启动本地单节点集群（服务器解压包用）
./bin/start-cluster.sh

# 关闭集群
./bin/stop-cluster.sh

# 只启动 JobManager
./bin/jobmanager.sh start
# 停止 JobManager
./bin/jobmanager.sh stop

# 启动 TaskManager
./bin/taskmanager.sh start
# 停止 TaskManager
./bin/taskmanager.sh stop
```

## 二、任务提交核心命令（最常用）
### 1. 提交 Jar 运行任务
```bash
# 基础提交
./bin/flink run -c com.xxx.task.MqToEsTask /opt/jar/flink-task.jar

# 指定并行度提交
./bin/flink run -p 4 -c com.xxx.task.MqToEsTask /opt/jar/flink-task.jar

# 开启Checkpoint（故障恢复）
./bin/flink run -s /opt/checkpoint/savepoint-xxx -c com.xxx.task.MqToEsTask /opt/jar/flink-task.jar

# 后台分离运行（长时间实时任务）
./bin/flink run -d -c com.xxx.task.MqToEsTask /opt/jar/flink-task.jar
```
参数说明：
- `-c`：指定程序入口 main 类（多模块必须加）
- `-p`：并行度
- `-d`：detached 后台运行，关闭终端不停止任务
- `-s`：从 savepoint 快照恢复任务

### 2. 停止任务（两种方式）
```bash
# 优雅停止，生成 savepoint（推荐，可恢复）
./bin/flink stop -s /opt/checkpoint sp-xxx 任务ID

# 直接取消任务，不保存快照
./bin/flink cancel 任务ID
```

### 3. 从快照重启任务
```bash
./bin/flink run -d -s /opt/checkpoint/sp-xxx -c com.xxx.task.MqToEsTask flink-task.jar
```

## 三、任务查询、状态查看命令
```bash
# 查看正在运行的所有任务
./bin/flink list

# 查看任务详情（算子、并行度、资源）
./bin/flink list -v

# 查看集群资源、TaskManager状态
./bin/flink cluster info
```

## 四、Savepoint 快照管理（实时任务必备）
```bash
# 手动触发任务生成快照
./bin/flink savepoint 任务ID /opt/checkpoint

# 取消任务同时生成快照
./bin/flink stop -s /opt/checkpoint 任务ID
```

## 五、Flink SQL 客户端命令（写SQL实时任务）
```bash
# 启动sql客户端
./bin/sql-client.sh

# 指定SQL脚本文件执行
./bin/sql-client.sh -f /opt/sql/mq2es.sql
```
SQL 客户端内常用操作：
```sql
-- 查看已注册表
SHOW TABLES;
-- 查看运行任务
SHOW JOBS;
-- 停止SQL任务
STOP JOB 'jobId';
```

## 六、Docker 环境配套操作（你本地开发专用）
### 1. 把本地jar上传到Flink容器
```bash
docker cp ~/code/flink-task.jar flink-local:/opt/jar/
```
### 2. 进入容器提交任务
```bash
docker exec -it flink-local bash
cd /opt/flink
./bin/flink run -d -c com.xxx.MqEsTask /opt/jar/flink-task.jar
```
### 3. 查看Flink容器日志
```bash
# 查看JobManager日志
docker logs -f flink-local
```

## 七、快速场景速查表
1. 打包代码上传Docker Flink并后台运行
```bash
docker cp target/flink-task.jar flink-local:/opt/jar/
docker exec -it flink-local ./bin/flink run -d -c com.xxx.MqToEs /opt/jar/flink-task.jar
```
2. 查看运行中的任务ID
```bash
docker exec -it flink-local ./bin/flink list
```
3. 优雅停任务并保存快照
```bash
docker exec -it flink-local ./bin/flink stop -s /opt/checkpoint 任务ID
```

## 补充区分
- 嵌入式本地模式：**无任何 flink 命令**，IDE 直接跑 main；
- Docker/服务器集群：必须用 `./bin/flink` 系列命令提交、管理任务；
- WebUI：http://localhost:8081 可视化操作，替代一部分命令。