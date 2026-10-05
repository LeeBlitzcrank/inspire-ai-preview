#!/usr/bin/env node

/**
 * 为项目源码和配置文件批量补充文件级职责注释。
 *
 * 设计原则：
 * 1. 只添加“文件是什么、属于哪个模块、主要职责是什么”，不逐行解释显然代码。
 * 2. 使用 INSPIRE_FILE_HEADER 标记保证幂等，重复执行不会重复插入。
 * 3. 排除 node_modules、dist、target、构建产物、二进制和不可注释 JSON。
 */
import {existsSync, readdirSync, readFileSync, statSync, writeFileSync} from 'node:fs'
import {extname, join, relative} from 'node:path'
import {fileURLToPath} from 'node:url'

const ROOT = join(fileURLToPath(new URL('.', import.meta.url)), '..')
const MARKER = 'INSPIRE_FILE_HEADER'
const ROOTS = [
  'backend',
  'frontend/src',
  'frontend/tests',
  'frontend/public',
  'frontend/index.html',
  'frontend/detail.html',
  'frontend/playwright.config.js',
  'scripts',
  'deploy',
  '.github/workflows',
  'database',
  'docker-compose.yml'
]
const EXTENSIONS = new Set([
  '.java', '.js', '.mjs', '.vue', '.css', '.sql', '.sh',
  '.yml', '.yaml', '.xml', '.html', '.conf', '.properties', '.svg'
])
const EXCLUDED = [
  '/node_modules/',
  '/dist/',
  '/target/',
  '/out/',
  '/.git/'
]

function walk(path, files = []) {
  if (!existsSync(path)) return files
  const normalized = path.replaceAll('\\', '/')
  if (EXCLUDED.some(part => normalized.includes(part))) return files
  // Flyway 已执行迁移文件不可修改，修改内容会导致 checksum 校验失败。
  if (normalized.includes('/db/migration/')) return files
  const stat = statSync(path)
  if (stat.isDirectory()) {
    for (const entry of readdirSync(path)) walk(join(path, entry), files)
    return files
  }
  if (EXTENSIONS.has(extname(path)) || path.endsWith('Dockerfile') || path.endsWith('Dockerfile.local')) {
    files.push(path)
  }
  return files
}

function moduleDescription(rel) {
  const rules = [
    ['backend/inspire-auth', '用户认证模块，负责登录、令牌、会话、密码和登录风控'],
    ['backend/inspire-common', '公共基础模块，提供统一响应、异常、鉴权上下文和通用工具'],
    ['backend/inspire-gateway', 'API 网关模块，负责路由、CORS、限流、JWT 校验和可信身份透传'],
    ['backend/inspire-core', '核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知'],
    ['backend/inspire-ai', 'AI 服务模块，负责创作生成、AI 探索和世界种子'],
    ['backend/inspire-admin', '后台管理模块，负责管理员鉴权、内容审核和运营配置'],
    ['backend/inspire-search', '搜索服务模块，负责 MySQL/Elasticsearch 搜索及降级'],
    ['backend/inspire-rag', '多模态 RAG 模块，负责索引、向量检索、问答和同步'],
    ['backend/inspire-mq', '消息队列公共模块，负责生产者、消费者和积压指标'],
    ['backend/flink-jobs', '实时计算模块，负责热点聚合和用户行为计算'],
    ['frontend/src/pages/admin', '后台管理前端页面'],
    ['frontend/src/pages', '用户端页面和交互流程'],
    ['frontend/src/components', '可复用 Vue 组件'],
    ['frontend/src/composables', '跨页面复用的组合式逻辑'],
    ['frontend/src/api', '前端接口请求封装'],
    ['frontend/src/utils', '前端通用工具和基础能力'],
    ['frontend/src/stores', 'Pinia 全局状态'],
    ['frontend/src/router', '前端路由配置'],
    ['frontend/src/styles', '全局设计令牌和样式'],
    ['frontend/tests', 'Playwright 端到端测试'],
    ['deploy/cloudflare', 'Cloudflare Worker 和隧道部署配置'],
    ['deploy/minio', 'MinIO、Nginx 和对象存储策略'],
    ['deploy/docker', 'Docker 镜像构建配置'],
    ['deploy/monitoring', 'Prometheus 和 Grafana 监控配置'],
    ['database', '数据库初始化和结构说明'],
    ['.github/workflows', 'GitHub Actions 自动化流程'],
    ['scripts', '本地开发和运维脚本']
  ]
  return rules.find(([prefix]) => rel.startsWith(prefix))?.[1] || '项目工程配置'
}

function responsibility(rel) {
  const name = rel.split('/').pop()
  const rules = [
    [/_test\.(js|mjs)$/i, 'Playwright 端到端测试，验证页面主流程和关键交互'],
    [/Test\.java$/, '自动化测试类，验证对应模块的边界行为和回归场景'],
    [/Application\.java$/, 'Spring Boot 应用启动入口，负责服务启动和组件扫描'],
    [/Controller\.java$/, 'HTTP 接口控制器，负责参数接收、权限上下文和响应返回'],
    [/ServiceImpl\.java$/, '业务服务实现，承载核心业务流程、事务和依赖编排'],
    [/Service\.java$/, '业务服务接口，定义模块对外能力'],
    [/Mapper\.java$/, '数据访问接口，负责数据库读写映射'],
    [/Config\.java$/, 'Spring 配置类，负责基础设施或框架能力装配'],
    [/Filter\.java$/, '请求过滤组件，处理进入业务前或响应后的通用逻辑'],
    [/Interceptor\.java$/, 'HTTP 拦截器，处理统一上下文和请求边界'],
    [/Util\.java$/, '通用工具类，提供可复用且无业务状态的静态能力'],
    [/(DTO|Request|Response|VO|Row)\.java$/, '接口或查询数据传输模型，定义字段结构'],
    [/Entity\.java$/, '数据库实体模型，对应持久化表结构'],
    [/\.vue$/, 'Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接'],
    [/composables\/use.*\.js$/, 'Vue Composable，集中管理页面状态、异步流程和生命周期'],
    [/api\/.*\.js$/, '前端 API 模块，统一封装后端接口调用'],
    [/utils\/.*\.js$/, '前端工具模块，提供可复用基础函数'],
    [/router\/.*\.js$/, '前端路由表，定义页面路径和访问守卫'],
    [/stores\/.*\.js$/, 'Pinia 状态模块，管理跨页面共享状态'],
    [/\.css$/, '样式文件，定义该页面或组件的视觉规则'],
    [/\.sql$/, '数据库脚本，定义表结构、索引、迁移或初始化数据'],
    [/\.sh$/, 'Shell 脚本，封装本地开发或运维命令'],
    [/\.ya?ml$/, 'YAML 配置，定义服务、运行参数或自动化流程'],
    [/\.xml$/, 'XML 配置，定义 Maven 依赖或构建参数'],
    [/\.html$/, 'HTML 页面或交互预览'],
    [/Dockerfile/, 'Docker 镜像构建文件，定义运行环境和启动方式'],
    [/docker-compose/, 'Docker Compose 编排文件，定义服务、网络和持久化卷'],
    [/\.svg$/, 'SVG 矢量资源'],
    [/\.conf$/, '服务配置文件']
  ]
  return rules.find(([pattern]) => pattern.test(name) || pattern.test(rel))?.[1] || '工程源码或配置文件'
}

function makeHeader(rel) {
  const module = moduleDescription(rel)
  const duty = responsibility(rel)
  return [
    `文件：${rel}`,
    `所属模块：${module}`,
    `主要职责：${duty}`,
    '维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。'
  ]
}

function commentBlock(rel, ext) {
  const lines = makeHeader(rel)
  if (ext === '.vue' || ext === '.html') {
    return `<!--\n${lines.map(line => `  ${line}`).join('\n')}\n  ${MARKER}\n-->\n`
  }
  if (ext === '.sql') {
    return `--\n${lines.map(line => `-- ${line}`).join('\n')}\n-- ${MARKER}\n`
  }
  if (['.yml', '.yaml', '.sh', '.conf', '.properties'].includes(ext) || rel.endsWith('Dockerfile') || rel.endsWith('Dockerfile.local')) {
    return `#\n${lines.map(line => `# ${line}`).join('\n')}\n# ${MARKER}\n`
  }
  if (ext === '.xml' || ext === '.svg') {
    return `<!--\n${lines.map(line => `  ${line}`).join('\n')}\n  ${MARKER}\n-->\n`
  }
  return `/**\n${lines.map(line => ` * ${line}`).join('\n')}\n * ${MARKER}\n */\n`
}

function insert(text, header, ext, rel) {
  if (ext === '.svg' && text.startsWith('#\n')) {
    const markerEnd = text.indexOf(`${MARKER}\n`)
    if (markerEnd >= 0) text = text.slice(markerEnd + MARKER.length + 1)
  }
  if (text.includes(MARKER)) return text
  if (ext === '.sh' && text.startsWith('#!')) {
    const newline = text.indexOf('\n')
    return `${text.slice(0, newline + 1)}${header}${text.slice(newline + 1)}`
  }
  if ((ext === '.xml' || ext === '.svg') && text.startsWith('<?xml')) {
    const newline = text.indexOf('\n')
    return `${text.slice(0, newline + 1)}${header}${text.slice(newline + 1)}`
  }
  if (ext === '.html' && /^<!DOCTYPE/i.test(text)) {
    const newline = text.indexOf('\n')
    return `${text.slice(0, newline + 1)}${header}${text.slice(newline + 1)}`
  }
  return `${header}${text}`
}

const files = ROOTS.flatMap(root => walk(join(ROOT, root)))
let changed = 0
for (const file of [...new Set(files)].sort()) {
  const rel = relative(ROOT, file).replaceAll('\\', '/')
  const ext = extname(file)
  const original = readFileSync(file, 'utf8')
  const updated = insert(original, commentBlock(rel, ext), ext, rel)
  if (updated !== original) {
    writeFileSync(file, updated)
    changed += 1
  }
}

console.log(`已补充文件级注释：${changed} 个文件`)
