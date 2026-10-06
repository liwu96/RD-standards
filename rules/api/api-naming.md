# API 命名规范

> 适用范围：团队所有对外/对内 HTTP API（REST 风格为主）、RPC 接口命名可参照执行。
> 约束级别定义：【强制】必须遵守，违反会被评审打回；【推荐】应当遵守；【参考】供选择时参考。

## 1. 总体风格

- 【强制】对外统一提供 REST 风格 HTTP 接口；个别操作语义复杂、无法映射到资源 CRUD 的场景，允许使用"动词子路径"的 RPC 风格（见 §4）。
- 【强制】URL 只使用小写字母、数字、连字符（`-`）和路径分隔符 `/`，禁止出现大写字母、下划线和中文。
- 【强制】接口必须携带版本号，版本号放在路径首位：`/api/v1/...`。不兼容变更必须升版本（v1 → v2），旧版本按团队公告的周期下线。
- 【强制】资源名使用英文复数名词，见名知义，禁止拼音与英文混用（国际通用名词如 alipay、taobao 可视同英文）。

```
正例：GET /api/v1/orders/{order_id}
反例：GET /api/v1/getOrderList          （动词 + 驼峰）
反例：GET /api/v1/dingdan                （拼音）
```

## 2. HTTP 方法语义

- 【强制】方法语义与下表一致，禁止用 GET 修改数据、用 POST 语义承担一切操作：

| 方法 | 语义 | 幂等 | 示例 |
|---|---|---|---|
| GET | 查询资源，无副作用 | 是 | `GET /api/v1/orders/{id}` |
| POST | 创建资源 / 触发不幂等的动作 | 否 | `POST /api/v1/orders` |
| PUT | 全量更新资源 | 是 | `PUT /api/v1/orders/{id}` |
| PATCH | 部分更新资源 | 是 | `PATCH /api/v1/orders/{id}` |
| DELETE | 删除资源（团队默认为逻辑删除） | 是 | `DELETE /api/v1/orders/{id}` |

- 【推荐】更新操作优先使用 PATCH（只传变更字段），与后端"禁止更新无改动字段"的数据库规范保持一致。

## 3. 路径与查询参数

- 【强制】路径参数用于定位唯一资源，命名为 `{xxxId}` 形式：`/api/v1/users/{userId}/orders`。
- 【强制】查询参数（query）仅用于 GET 的过滤、排序、分页，禁止在 GET 中传 body。
- 【强制】分页参数统一为 `page`（从 1 开始）与 `pageSize`（默认 20，最大 100）；排序参数统一为 `sort`，多个字段逗号分隔，字段名前加 `-` 表示倒序。
- 【推荐】超过 2 个查询条件的接口，条件命名与 JSON 字段保持同名（见 §6 命名矩阵）。

```
GET /api/v1/orders?status=PAID&sort=-createdAt&page=2&pageSize=20
```

- 【强制】URL 长度超过 2000 字节的批量/复杂查询改为 POST + body（`POST /api/v1/orders/batch-query`）。

## 4. 非 CRUD 动作（动词子路径）

- 【强制】无法用标准方法表达的动作，使用"资源 + 冒号或连字符动词"的子路径，动词用小写蛇形/连字符均可但全团队统一一种（本规范定为连字符）：

```
POST /api/v1/orders/{id}/cancel        取消订单
POST /api/v1/orders/{id}/submit        提交审核
POST /api/v1/users/batch-export        批量导出
```

- 【强制】动作接口本身必须是名词资源下的子路径，禁止出现 `/api/v1/cancelOrder` 这种"顶层动词"式 URL。

## 5. 接口（代码层）命名

- 【强制】Java 侧 Controller/Service/DAO 方法命名沿用《阿里巴巴 Java 开发手册》分层命名：
  - 获取单个对象 `getXxx`；获取多个对象 `listXxx`；统计值 `countXxx`；
  - 插入 `saveXxx`/`insertXxx`；删除 `removeXxx`/`deleteXxx`；修改 `updateXxx`。
- 【强制】Python 侧函数/方法命名使用 `snake_case`：`get_order_by_id`、`list_paid_orders`、`create_order`，与 §6 矩阵一致。
- 【参考】RPC 接口（Dubbo/gRPC）方法名 = 动词 + 宾语驼峰：`createOrder`、`queryUserOrders`；gRPC 按 proto 惯例可另行约定，但同一服务体系内必须统一。

## 6. 命名矩阵（重要）

【强制】各层命名风格按下表执行，跨层转换由框架统一完成，禁止手写不一致的映射：

| 层 | 风格 | 示例 |
|---|---|---|
| URL 路径 | 小写 + 连字符 / 复数名词 | `/api/v1/order-items` |
| JSON 字段（入参/出参）、query 参数 | `lowerCamelCase` | `orderStatus`, `createdAt`, `pageSize` |
| 枚举传输值 | 全大写下划线字符串 | `"PAID"`, `"PENDING_REVIEW"` |
| 布尔字段 | `is`/`has` 前缀 + 形容词 | `isDeleted`, `hasStock` |
| Java 代码属性 | `lowerCamelCase`（与协议同名，零转换） | `orderStatus` |
| Python 代码属性 | `snake_case`（pydantic `alias_generator=to_camel` + `populate_by_name=True` 负责协议转换） | `order_status` |
| 数据库字段 | `snake_case`（ORM 驼峰映射，如 MyBatis `mapUnderscoreToCamelCase`） | `order_status` |

> 协议层统一 `lowerCamelCase` 的原因：与前端 JS/TS 生态及主流 API 惯例一致，Java 侧天然同名无需任何序列化配置；Python 侧由 pydantic 别名机制一次配置转换；数据库侧由 ORM 驼峰映射处理。跨语言/跨层的名称转换全部由框架承担，任何人不得手写映射。

## 7. 其他约定

- 【强制】时间参数与返回一律使用 RFC 3339 / ISO 8601 带时区格式：`2026-10-05T12:30:00+08:00`；Unix 时间戳仅限内部性能场景，禁止对外混用两种格式。
- 【强制】金额单位统一为"分"（整数），字段名以 `Cents` 结尾（如 `amountCents`）或注释说明，避免浮点误差。
- 【强制】ID 对外统一为字符串，避免 JS 侧 long 精度丢失。
- 【参考】一次接口评审 checklist：方法语义正确？版本在路径？资源复数？命名矩阵符合？分页/排序参数规范？响应包 envelope 符合出入参规范？
