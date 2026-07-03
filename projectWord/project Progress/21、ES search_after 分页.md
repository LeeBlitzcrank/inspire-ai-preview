# 灵思集 — ES search_after 游标分页

> 版本: v1.0 | 更新日期: 2026-07-02

---

## 一、背景

ES 默认的 `from + size` 分页在深分页时存在性能问题：

```
"from": 10000, "size": 20   # ES 仍需排序前 10020 条再截断
```

`search_after` 通过**游标**直接定位上次结果的最后一条，跳过已检索文档的排序开销：

```
"search_after": [13, "198324536624549889"],
"sort": [{"heat": "desc"}, {"id": "desc"}]
```

---

## 二、改动

### 后端

| 文件 | 改动 |
|------|------|
| `SearchResultVO.java` | 新增 `sortValue` 字段，前端用于游标回传 |
| `SearchService.java` | 新增 `searchAfter` 参数 |
| `EsSearchService.java` | `buildQuery()` 支持 `search_after`；`sort` 增加 `id` 作为 tiebreaker；响应中提取 `sort` 值写入 VO |
| `MysqlSearchService.java` | 新增 `searchAfter` 参数（MySQL 不使用） |
| `SearchServiceManager.java` | 透传 `searchAfter` 参数 |
| `SearchController.java` | 新增 `searchAfter` 参数 |

### 前端

| 文件 | 改动 |
|------|------|
| `Search.vue` | 游标翻页：首次搜索不加 `searchAfter`，后续追加 |

---

## 三、游标格式

`searchAfter` = `"{heat}_{id}"`，如 `"13_198324536624549889"`

- `heat` — 主排序字段（降序）
- `id` — 唯一 tiebreaker，防止同热度结果丢失/重复

---

## 四、部署

无需数据库变更。重启 `inspire-search` 服务即可。

```bash
cd ~/Desktop/Project/Study\ Project/Codex/inspire-ai-preview/backend
mvn spring-boot:run -pl inspire-search
```


ES 没在跑，所以搜索会自动降级到 MySQL。不过两者的测试方式一样。

---

## 测试步骤

### 1. 确认搜索服务正常

```bash
# 重启 search 服务
kill -9 $(lsof -ti :8086 2>/dev/null)
cd ~/Desktop/Project/Study\ Project/Codex/inspire-ai-preview/backend
nohup mvn spring-boot:run -pl inspire-search > /tmp/search.log 2>&1 &

sleep 8
tail -3 /tmp/search.log | grep "Started"
```

确认看到 `Started InspireSearchApplication`。

### 2. 测试搜索接口

```bash
# 发起搜索（无游标 = 第一页）
curl "http://localhost:8080/api/search/public?keyword=美食&size=3" | python3 -m json.tool | head -40
```

看返回的每一项是否有 `sortValue` 字段，如：
```json
{
    "id": 198275883717365761,
    "title": "...",
    "heat": 13,
    "sortValue": "13_198275883717365761",
    ...
}
```

### 3. 测试游标翻页（模拟前端行为）

```bash
# 第一页
curl -s "http://localhost:8080/api/search/public?keyword=美食&size=2" > page1.json

# 取最后一条的 sortValue
SORT=$(cat page1.json | python3 -c "import sys,json; items=json.load(sys.stdin)['data']; print(items[-1]['sortValue'] if items else '')")
echo "游标: $SORT"

# 第二页（传入游标）
curl "http://localhost:8080/api/search/public?keyword=美食&size=2&searchAfter=$SORT" | python3 -m json.tool | head -20
```

如果返回了数据且没有重复第一条，说明游标翻页正常。

### 4. 对比 with/without 游标

```bash
# 不带游标 = 从头查
curl -s "http://localhost:8080/api/search/public?keyword=美食&size=2" | python3 -c "import sys,json; print([i['id'] for i in json.load(sys.stdin)['data']])"

# 带游标 = 从游标之后查（结果不同说明生效）
SORT=$(curl -s "http://localhost:8080/api/search/public?keyword=美食&size=2" | python3 -c "import sys,json; items=json.load(sys.stdin)['data']; print(items[-1]['sortValue'])")
curl -s "http://localhost:8080/api/search/public?keyword=美食&size=2&searchAfter=$SORT" | python3 -c "import sys,json; print([i['id'] for i in json.load(sys.stdin)['data']])"
```

两组 ID 应该**不同**（第二组是紧接第一组之后的结果）。

### 5. 前端验证

浏览器打开搜索页 → 输入关键词搜索 → 滚动翻页 → F12 Network 看第二次请求是否带了 `searchAfter` 参数。

---

如果 ES 以后启动，`buildQuery()` 会自动用 `search_after` 替代 `from`，不需要额外改动。
