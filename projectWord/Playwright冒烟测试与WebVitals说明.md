# Playwright 冒烟测试与 Web Vitals 说明

> 更新时间：2026-09-23
> 适用范围：前端主链路自动验证、真实用户性能指标采集
> 目标：减少人工回归成本，并为后续性能优化提供 FCP/LCP/CLS 等真实数据

## 一、Playwright 冒烟测试

### 1. 是什么

Playwright 是浏览器自动化测试框架，可以启动真实浏览器并模拟用户操作：

- 输入账号密码。
- 点击按钮。
- 上传文件。
- 切换页面。
- 输入评论。
- 检查接口响应和页面内容。

“冒烟测试”不是覆盖全部功能的完整测试，而是只验证最关键的几条主链路，确认代码改动没有把核心流程整体打坏。

### 2. 当前覆盖范围

当前冒烟测试覆盖：

1. 用户登录。
2. 进入创建灵感页面。
3. 填写标题、分类和正文。
4. 上传一张测试图片。
5. 发布灵感。
6. 进入新灵感详情页。
7. 发表评论。
8. 确认评论出现在页面。
9. 测试结束后删除测试灵感。

测试不会：

- 调用 DeepSeek。
- 调用 AI 探索。
- 调用 AI 配图。
- 消耗大模型 token。

### 3. 实现文件

配置：

```text
frontend/playwright.config.js
```

测试用例：

```text
frontend/tests/e2e/smoke.spec.js
```

依赖：

```text
@playwright/test
```

package 命令：

```json
{
  "test:e2e": "playwright test",
  "test:e2e:headed": "playwright test --headed"
}
```

### 4. 浏览器选择

默认使用本机 Chrome：

```js
channel: 'chrome'
```

这样可以避免下载和维护 Playwright 自己的 Chromium。

如需要切换：

```bash
PLAYWRIGHT_CHANNEL=msedge npm run test:e2e
```

### 5. 运行方式

普通无头运行：

```bash
cd frontend
npm run test:e2e
```

显示浏览器运行：

```bash
cd frontend
npm run test:e2e:headed
```

指定环境地址：

```bash
E2E_BASE_URL=https://ai.20sherry.com npm run test:e2e
```

指定账号：

```bash
E2E_USER=user001 \
E2E_PASSWORD=112233 \
npm run test:e2e
```

如果前端服务已经启动，Playwright 会自动复用：

```js
reuseExistingServer: true
```

不启动 webServer：

```bash
E2E_SKIP_WEBSERVER=1 npm run test:e2e
```

### 6. 测试数据清理

测试创建的灵感会在 `finally` 中通过 API 删除：

```text
DELETE /api/inspire/{id}
```

即使断言失败，只要已经拿到灵感 ID，也会尝试清理。

### 7. 失败产物

测试失败时保留：

```text
test-results/
```

包含：

- 截图。
- trace 压缩包。
- 页面错误上下文。

查看 trace：

```bash
npx playwright show-trace test-results/.../trace.zip
```

### 8. 本次实际测试结果

执行：

```bash
npm run test:e2e
```

结果：

```text
1 passed
耗时：6.7s
```

测试过程中还发现并修复了一个真实问题：

```text
创建灵感接口的雪花 ID 原来以数字返回，前端会丢失精度。
```

现在 `InspireMain.id` 统一按字符串返回。

### 9. Token 成本

| Token 类型 | 是否消耗 |
| --- | --- |
| DeepSeek/大模型 token | 否 |
| AI 探索 token | 否，冒烟不进入 AI 流程 |
| JWT accessToken / refreshToken | 会生成，但属于登录凭证，不收费 |
| Codex 对话 token | 如果由 Codex 编写或调试测试会消耗 Codex 使用量 |

测试运行本身不调用大模型。

## 二、Web Vitals

### 1. 是什么

Web Vitals 是 Google 提供的真实用户性能指标采集方案。

当前接入：

| 指标 | 说明 |
| --- | --- |
| FCP | 首次内容绘制 |
| LCP | 最大内容绘制 |
| CLS | 布局偏移 |
| INP | 交互响应延迟 |
| TTFB | 首字节时间 |

### 2. 前端实现

文件：

```text
frontend/src/utils/webVitals.js
```

入口：

```text
frontend/src/main.js
```

采集流程：

```text
浏览器产生指标
↓
web-vitals 回调
↓
按采样率判断是否上报
↓
navigator.sendBeacon
↓
POST /api/inspire/public/vitals
↓
写入 web_vital_metric
```

### 3. 采样

默认采样率：

```text
20%
```

可通过环境变量调整：

```bash
VITE_WEB_VITALS_SAMPLE_RATE=0.1 npm run build
```

如果某个环境不希望采集：

```js
sessionStorage.setItem('__disable_web_vitals__', '1')
```

Playwright 冒烟测试会自动关闭 web-vitals，避免测试流量污染指标。

### 4. 后端接口

接口：

```text
POST /api/inspire/public/vitals
```

Controller：

```text
backend/inspire-core/src/main/java/com/inspire/platform/core/controller/WebVitalsController.java
```

接受字段：

```json
{
  "name": "LCP",
  "value": 1234.5,
  "rating": "good",
  "delta": 1234.5,
  "navigationType": "navigate",
  "path": "/#/personal",
  "device": "desktop",
  "browser": "Chrome",
  "appVersion": "1.0.0"
}
```

只接受：

```text
FCP
LCP
CLS
INP
TTFB
```

### 5. 数据库

表：

```text
web_vital_metric
```

主要字段：

```sql
metric_name
metric_value
metric_rating
metric_delta
navigation_type
page_path
device_type
browser
app_version
user_id
create_time
```

索引：

```sql
KEY idx_vital_name_time (metric_name, create_time)
KEY idx_vital_path_time (page_path, create_time)
KEY idx_vital_user_time (user_id, create_time)
```

### 6. 查询示例

按页面查看 LCP 平均值：

```sql
SELECT
  page_path,
  COUNT(*) AS samples,
  AVG(metric_value) AS avg_lcp
FROM web_vital_metric
WHERE metric_name = 'LCP'
  AND create_time >= NOW() - INTERVAL 1 DAY
GROUP BY page_path
ORDER BY avg_lcp DESC;
```

查看不同设备的 CLS：

```sql
SELECT
  device_type,
  AVG(metric_value) AS avg_cls
FROM web_vital_metric
WHERE metric_name = 'CLS'
  AND create_time >= NOW() - INTERVAL 7 DAY
GROUP BY device_type;
```

查看最慢页面：

```sql
SELECT
  page_path,
  MAX(metric_value) AS max_lcp
FROM web_vital_metric
WHERE metric_name = 'LCP'
GROUP BY page_path
ORDER BY max_lcp DESC
LIMIT 20;
```

### 7. Web Vitals 不消耗大模型 token

Web Vitals 只会上报浏览器性能数据：

- 不调用 AI。
- 不读取用户正文。
- 不读取评论内容。
- 不消耗 DeepSeek token。

它只会产生很小的 HTTP 上报请求和数据库写入。

## 三、后续可以做什么

### 1. Playwright

- 加入 CI，每次提交自动跑冒烟。
- 增加移动端视口测试。
- 增加视频上传测试。
- 增加收藏夹和私信测试。
- 使用 API fixture 统一创建和清理测试数据。
- 在发布前只跑冒烟，在夜间跑完整回归。

### 2. Web Vitals

- 搭建性能趋势看板。
- 按页面、设备、版本对比 FCP/LCP/CLS。
- 设置 LCP、CLS、INP 告警阈值。
- 把慢页面关联到具体前端版本。
- 接入 Sentry 或自建 RUM 平台。
- 对 p75 而不是平均值做优化决策。

推荐阈值：

| 指标 | Good | Needs Improvement |
| --- | ---: | ---: |
| LCP | ≤ 2.5s | ≤ 4s |
| CLS | ≤ 0.1 | ≤ 0.25 |
| INP | ≤ 200ms | ≤ 500ms |
| FCP | ≤ 1.8s | ≤ 3s |
| TTFB | ≤ 800ms | ≤ 1.8s |
