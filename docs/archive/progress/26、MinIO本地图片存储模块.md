# MinIO本地图片存储模块设计文档
## 文档基础信息
| 项目 | 内容 |
| ---- | ---- |
| 文档名称 | MinIO本地图片存储模块设计文档 |
| 适用环境 | 本地Mac开发/演示，自有服务器存储，无云端OSS、无定时异地备份 |
| 整体架构 | SpringCloud Gateway后端 + MinIO(Docker) + Nginx反向缓存 + Cloudflare Tunnel/CDN |
| 对外域名 | 图片访问域名 `img.20sherry.com` |
| 编写目的 | 规范图片上传、存储、访问、安全校验、私有资源权限全流程开发标准 |

# 1 业务概述
## 1.1 业务场景
1. 前端站点 `ai.20sherry.com`（GitHub Pages静态页面）上传图片素材；
2. 用户上传头像、AI灵感配图、内容封面等图片资源；
3. 区分**公开图片**、**私有私密图片**两套访问权限；
4. 图片本地持久化存储，依托Nginx实现缩略图、WebP压缩、本地磁盘缓存；
5. Cloudflare免费CDN提供外网加速、防盗链、HTTPS，通过Cloudflare Tunnel穿透本地服务对外暴露。

## 1.2 设计约束
1. 不使用阿里云/腾讯云OSS，全部图片存储本地Mac硬盘MinIO；
2. 不配置定时备份、异地灾备脚本（本地演示环境，人为手动管理磁盘文件）；
3. 无付费CDN、无第三方图片审核API，安全校验、图片处理全部本地实现；
4. 域名统一由Cloudflare托管DNS，`img.20sherry.com` 永久固定穿透地址，废弃ngrok。

## 1.3 模块目标
1. 安全：拦截图片马、恶意文件上传，防止盗图、私密素材泄露；
2. 性能：多层缓存（Cloudflare CDN + Nginx本地磁盘缓存）、自动生成缩略图；
3. 规范：统一存储目录、统一访问域名、统一接口出入参；
4. 低成本：零云厂商存储/流量费用，仅消耗本地硬件资源。

# 2 整体架构设计
## 2.1 分层链路图
```
前端页面 ai.20sherry.com(GitHub Pages)
        ↓ 上传接口请求 https://api.20sherry.com/upload/image
Cloudflare CDN → Cloudflare Tunnel → 本地Gateway网关(鉴权)
        ↓ 后端业务服务
文件校验、图片清洗 → MinIO上传存储 → 写入MySQL图片元数据

用户访问图片链路：
前端请求 https://img.20sherry.com/xxx.jpg?w=400
Cloudflare CDN(第一层缓存) → Cloudflare Tunnel → 本地Nginx
Nginx本地磁盘缓存(第二层缓存) → 反向代理MinIO读取原图
Nginx动态生成缩略图/WebP，回流缓存返回前端
```

## 2.2 组件分工
| 组件 | 职责 | 运行环境 |
| ---- | ---- | -------- |
| MinIO | 私有化对象存储，管理图片原始文件、区分公开/私有桶、生成临时签名URL | Docker容器本地 |
| Nginx | 反向代理MinIO、本地磁盘缓存、动态缩略图、WebP压缩、本地防盗链 | Docker容器本地 |
| Cloudflare Tunnel | 内网穿透，将本地80端口Nginx映射外网`img.20sherry.com` | Mac本地终端常驻 |
| Cloudflare CDN | 全局缓存、HTTPS、防盗链、图片自动优化、域名DNS托管 | 云端 |
| Spring Gateway | 上传接口鉴权、限流、跨域拦截、全局请求过滤 | 本地Java后端 |
| SpringBoot业务服务 | 文件四层校验、图片清洗、MinIO SDK交互、元数据入库、私有图签名接口 | 本地Java后端 |
| MySQL | 仅存储图片元数据，不存储图片二进制流 | 本地Docker |

# 3 存储层设计（MinIO）
## 3.1 MinIO部署规范
1. 容器隔离：独立Docker Compose编排，端口9000(API)/9001(管理后台)；
2. 持久化：挂载宿主机目录`./minio-data`，容器重启图片不丢失；
3. 安全配置：账号密钥通过环境变量注入，禁止硬编码；
4. 版本控制：存储桶开启版本控制，误删图片可在管理后台手动恢复；
5. 无自动备份策略，磁盘满后手动清理无用图片。

## 3.2 桶与目录分层规范
### 3.2.1 桶定义
仅创建1个存储桶：`inspire-img`，统一管理全部图片资源。
### 3.2.2 目录分层规则（防单目录文件过多卡顿）
1. 公开图片路径：`upload/{userId}/{yyyyMMdd}/{UUID}.{后缀}`
   示例：`upload/10001/20260713/550e8400-e29b-41d4-a716-446655440000.jpg`
2. 私有图片路径：`upload/private/{userId}/{yyyyMMdd}/{UUID}.{后缀}`
   示例：`upload/private/10001/20260713/550e8400-e29b-41d4-a716-446655440000.png`
### 3.3 文件命名规则
1. 丢弃用户原始文件名，全局使用UUID字符串命名；
2. 仅允许后缀：`jpg、jpeg、png、webp`；
3. 过滤文件名危险字符 `../ \ % & *`，杜绝路径穿越漏洞。

## 3.4 权限规则
1. 公开目录 `/upload/`：Nginx配置允许CDN直接访问，搭配Referer防盗链；
2. 私有目录 `/upload/private/`：Nginx直接拦截访问，仅允许后端生成15分钟临时签名URL访问；
3. 签名时效：私有图片URL有效期固定15分钟，过期自动失效。

# 4 Nginx代理缓存模块设计
## 4.1 核心能力
1. 反向代理MinIO 9000端口，隔离底层存储地址不暴露外网；
2. 磁盘持久化缓存，30天未访问自动清理，最大占用50G本地硬盘；
3. 动态图片处理：URL传参`?w=xxx`自动生成自适应缩略图，自动转WebP格式；
4. 双层防盗链：本地Nginx拦截非法Referer，兜底Cloudflare WAF规则；
5. 长缓存响应头，提升CDN缓存命中率，减少本地回源。

## 4.2 缓存逻辑
1. 首次访问图片：Nginx回源MinIO读取原图，处理后写入本地缓存目录，同时返回前端；
2. 重复访问：直接读取本地缓存文件，不再访问MinIO，降低磁盘IO消耗；
3. 404失效图片缓存10分钟，避免无效重复回源。

# 5 后端业务逻辑设计
## 5.1 上传全链路四层安全校验（核心安全规范）
### 校验1：接口鉴权
上传接口 `/api/upload/image` 必须携带JWT Token，Gateway拦截未登录匿名请求。
### 校验2：基础参数拦截
1. 单文件最大限制5MB，超大文件直接返回400；
2. 校验文件后缀，仅放行jpg/png/webp。
### 校验3：MIME类型校验
读取文件ContentType，拦截伪装后缀的可执行文件。
### 校验4：二进制魔数校验 + 图片重绘清洗（防图片马）
1. 读取文件二进制头部判断真实图片类型；
2. 使用Thumbnails重绘图片，清除内嵌EXIF、恶意脚本、隐藏木马，生成全新干净图片流上传MinIO。

## 5.2 上传业务流程
1. 接收前端MultipartFile文件；
2. 执行四层安全校验，校验失败直接抛出异常；
3. 重绘清洗图片二进制流；
4. 根据用户ID、日期、UUID生成分层存储路径；
5. 将清洗后的图片流上传至MinIO对应目录；
6. 拼接CDN域名生成完整访问地址；
7. 将图片元数据写入MySQL；
8. 返回CDN地址、文件唯一标识给前端。

## 5.3 私有图片访问流程
1. 前端请求私有图片时，不能直接拼接`img.20sherry.com`地址；
2. 前端调用鉴权接口 `GET /api/image/private`，传入文件fileKey；
3. 后端校验当前登录用户是否为图片上传所有者，非本人返回403无权限；
4. 校验通过后，调用MinIO SDK生成15分钟临时签名URL返回前端；
5. 前端使用临时签名链接渲染图片，过期后链接失效无法访问。

## 5.4 图片删除逻辑
1. 业务端仅执行MySQL逻辑删除 `del_flag=1`；
2. 本地演示环境不自动删除MinIO物理文件，如需清理手动登录MinIO后台删除对应文件；
3. 开启MinIO版本控制，误删文件可手动恢复。

# 6 数据库设计
## 6.1 表名：sys_upload_image
```sql
CREATE TABLE sys_upload_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '上传用户ID',
    original_name VARCHAR(256) COMMENT '用户原始上传文件名',
    file_key VARCHAR(512) NOT NULL COMMENT 'MinIO存储对象路径',
    cdn_url VARCHAR(512) NOT NULL COMMENT 'CDN完整访问地址',
    file_size BIGINT COMMENT '文件大小，单位字节',
    file_type VARCHAR(32) COMMENT '图片格式 jpg/png/webp',
    width INT COMMENT '原图宽度',
    height INT COMMENT '原图高度',
    is_private TINYINT DEFAULT 0 COMMENT '0=公开图片 1=私有私密图片',
    audit_status TINYINT DEFAULT 0 COMMENT '0待审核 1合规 2违规（本地人工审核）',
    create_time DATETIME DEFAULT NOW() COMMENT '上传时间',
    del_flag TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片上传元数据表';
```
## 6.2 设计说明
1. 不存储图片二进制文件，仅存储文件元数据；
2. `file_key` 关联MinIO存储路径，唯一标识一张图片；
3. `cdn_url` 前端页面直接渲染使用；
4. `is_private` 区分公私资源，控制访问权限；
5. `del_flag` 逻辑删除，保留图片记录便于溯源。

# 7 接口设计规范
## 7.1 图片上传接口
### 接口信息
- 请求方式：POST
- 接口地址：`/api/upload/image`
- 请求头：`Authorization: Bearer {JWT_TOKEN}`
- Content-Type：multipart/form-data
### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| file | File | 是 | 上传图片文件 |
| isPrivate | Integer | 是 | 0公开 1私有 |
### 返回VO
```json
{
  "code":200,
  "data":{
    "id":10001,
    "cdnUrl":"https://img.20sherry.com/upload/10001/20260713/xxx.jpg",
    "fileKey":"upload/10001/20260713/xxx.jpg"
  }
}
```

## 7.2 获取私有图片临时签名接口
### 接口信息
- 请求方式：GET
- 接口地址：`/api/image/private`
- 请求头：`Authorization: Bearer {JWT_TOKEN}`
### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| fileKey | String | 是 | MinIO文件唯一路径 |
### 返回示例
```json
{
  "code":200,
  "data":{
    "privateUrl":"https://img.20sherry.com/upload/private/xxx.jpg?X-Amz-Algorithm=xxx"
  }
}
```

# 8 安全设计规范
## 8.1 上传安全防护
1. 四层文件校验，杜绝图片马、脚本文件上传；
2. 图片重绘清洗，清除图片内嵌恶意代码；
3. 统一UUID文件名，消除路径穿越漏洞；
4. Gateway上传接口IP限流，防止恶意批量刷存储。

## 8.2 访问权限防护
1. 私有图片禁止直连访问，必须后端鉴权生成短期签名URL；
2. Nginx拦截`/upload/private/`路径直接返回403；
3. Cloudflare WAF全局防盗链，仅允许`*.20sherry.com`域名加载图片；
4. 双层Referer拦截（Nginx本地 + Cloudflare云端），杜绝外部网站盗图消耗带宽。

## 8.3 传输安全
1. 全站强制HTTPS，Cloudflare自动跳转HTTP请求；
2. 图片域名开启Cloudflare代理，隐藏本地服务器真实地址；
3. Cloudflare Tunnel加密隧道，外网无法直接访问本地端口。

# 9 性能优化设计
1. **多层缓存**
    - 第一层：Cloudflare全球CDN缓存静态图片，30天TTL；
    - 第二层：Nginx本地磁盘缓存，重复访问不读取MinIO；
2. **自动图片压缩**
   Nginx image_filter自动转WebP格式，同等清晰度体积减少60%；URL传参`?w=宽度`动态生成缩略图，前端列表无需加载大图；
3. **静态资源长缓存头**
   统一添加`Cache-Control: public,max-age=2592000`，延长缓存周期；
4. **分层目录存储**
   按用户ID+日期拆分目录，避免单目录文件过多导致磁盘读取卡顿。

# 10 部署与运行规范
## 10.1 本地部署流程
1. Docker Compose启动MinIO + Nginx镜像；
2. 进入MinIO容器初始化存储桶、开启版本控制；
3. 配置cloudflared隧道，绑定`img.20sherry.com`域名分流本地80端口Nginx；
4. 启动cloudflared隧道常驻后台；
5. 启动SpringBoot后端服务，加载MinIO配置；
6. 前端环境变量VITE_API_BASE指向`https://api.20sherry.com`，打包部署GitHub Pages。

## 10.2 Cloudflare配套配置规范
1. DNS记录：`img.20sherry.com` CNAME隧道地址，开启橙色代理；
2. 缓存规则：匹配`img.20sherry.com/*`，缓存全部静态资源，TTL30天；
3. 图像优化：开启自动WebP、图片压缩；
4. WAF防盗链：拦截非本站域名Referer访问；
5. 强制HTTPS，HTTP请求自动跳转。

# 11 维护规范（本地演示专用，无自动备份）
1. 磁盘监控：手动查看`minio-data`目录占用空间，硬盘容量不足手动清理无用图片；
2. 缓存清理：前端图片更新后，Cloudflare后台手动清除`img.20sherry.com`缓存；
3. 图片恢复：误删图片登录MinIO管理后台，通过版本控制/回收站手动恢复；
4. 日志排查：Nginx日志、cloudflared隧道日志、后端业务日志定位上传/访问异常；
5. 清理废弃资源：长期不用的图片，先修改数据库`del_flag=1`，再手动删除MinIO对应文件。

# 12 风险说明
1. 无异地备份：本地硬盘损坏会永久丢失图片，仅适合演示场景；
2. 带宽上限受本地Mac网络限制，高并发访问图片加载速度下降；
3. 无自动弹性扩容：硬盘存满需手动新增磁盘挂载MinIO；
4. 无云端内容审核：违规图片依靠人工后台审核标记，平台自行承担内容合规责任。