---
name: api-design
description: 新增或修改 HTTP API 接口时的设计走查流程。设计新端点、评审接口契约、注册新错误码、评审分页/排序/幂等设计时使用。Use when designing or reviewing HTTP API endpoints, request/response contracts, or registering new error codes.
metadata:
  origin: RD-Standards
---

# API 设计走查

新增/修改接口时按本流程执行，产出可直接评审的接口设计。规范依据：`rules/api/api-naming.md`、`rules/api/api-request-response.md`、`rules/api/status-codes.md`。

## When to Activate

- 设计新端点（新资源、新动作）
- 修改既有接口（加字段、改语义、废弃字段）
- 评审他人的接口设计 / OpenAPI 变更
- 需要新增业务错误码

## 步骤

### 1. 收集信息

先确认以下输入，缺一项就先问清，不要凭假设设计：

- [ ] 资源与操作是什么？调用方是谁（Web/App/服务间）？
- [ ] 读还是写？是否需要幂等（写操作默认需要）？
- [ ] 是否涉及敏感数据（手机号/身份证/金额）？
- [ ] 预估数据量级（决定分页方式）？

### 2. 定 URL 与方法

按 `rules/api/api-naming.md` 逐项核对：

- [ ] 资源复数名词、小写、连字符；版本在路径 `/api/v1/...`
- [ ] 方法语义正确（GET 无副作用；更新优先 PATCH）
- [ ] 非 CRUD 动作用动词子路径 `POST /api/v1/orders/{id}/cancel`
- [ ] 命名矩阵：JSON/query 为 `lowerCamelCase`；Java 遵循 Java Bean 布尔命名并在需要时显式映射；Python snake_case + pydantic to_camel 别名

### 3. 定义出入参

按 `rules/api/api-request-response.md`：

- [ ] 入参 Schema（Java Bean Validation / Python pydantic）逐字段标注：必填、类型、范围、示例
- [ ] 分页 `page`/`pageSize`（≤100）；排序 `sort` 白名单校验
- [ ] 写操作定义幂等策略：使用 `Idempotency-Key`（或请求体 `idempotencyKey`）并说明作用域、保存时长和重试行为；`requestId` 仅做链路追踪
- [ ] 出参用统一 envelope `{code, message, data, requestId, serverTime}`；列表用 `{total, page, pageSize, list}`
- [ ] 时间 RFC 3339、金额整数（分）、ID 字符串；协议布尔字段如 `isDeleted`/`hasStock`，Python/数据库用对应 snake_case，Java 布尔属性遵循本语言规范并显式映射
- [ ] 敏感字段出参脱敏（`158****9119`）

#### Python / FastAPI 走查

- [ ] 请求体和 GET 查询参数分别定义 Pydantic Schema：请求体绑定 `Body` 模型，过滤/排序/分页绑定 `Query` 模型；GET 不使用请求体承载查询条件。
- [ ] 同一 Schema 同时驱动运行时校验、OpenAPI 文档和序列化；禁止新增与 Schema 重复维护的手写字段字典或 marshalling 结构。
- [ ] 结构化接口在路由声明 `response_model`（或框架等价 Schema），声明的是最终统一 envelope；响应来自 ORM/领域对象时使用项目统一序列化 helper，并显式处理 `from_attributes`、JSON 模式和对外 alias。
- [ ] Schema 与 DB/领域对象默认使用 snake_case；有协议差异时使用显式 alias/mapping，禁止在业务代码散落手写字段转换。
- [ ] 服务层返回 `None` 时先映射为约定的 404/业务异常，再做响应 Schema 校验；采用 `204 No Content` 时不返回 envelope、字典或其他响应 body。

### 4. 选定错误码

按 `rules/api/status-codes.md`：

- [ ] 先查 §2 登记表，已有语义复用，禁止重复注册
- [ ] 需要新码时：`A/B/C 大类 + 4 位数字`，同步 PR 更新登记表与公共枚举（Java `ErrorCode` / Python `ErrorCode`）
- [ ] HTTP 码按映射表返回，禁止 200 + 业务错误
- [ ] message 面向用户可读；内部细节只进日志

### 5. 契约与文档

- [ ] 更新 OpenAPI/接口文档，与代码同步提交
- [ ] 不兼容变更（删字段/改类型/改语义）必须升版本 `/api/v2`；新增可选字段视为兼容

#### FastAPI / OpenAPI 生成验证

对 FastAPI 项目使用仓库现有的测试和启动入口生成 `app.openapi()`（命令按项目实际脚本调整），不得手工编辑生成的 OpenAPI 文件。至少检查：

- [ ] GET 参数均为 `in: query`；没有请求体的 GET 不出现 `requestBody`。
- [ ] 请求体只出现在代码明确声明 Body Schema 的接口；Schema 中的必填、范围、枚举与文档一致。
- [ ] 结构化响应引用预期的 envelope/`*Response` Schema，公开字段使用协议 lowerCamelCase，不泄露内部 snake_case 或 validation alias。
- [ ] 使用 `204 No Content` 的响应没有响应 body 或响应 Schema；默认 200 + envelope 的接口保持既有 `data` 结构。
- [ ] 生成的契约变更有对应的 Schema/路由测试；共享或下游文档引用生成结果时记录生成所依据的提交版本。

## 产出格式

```markdown
## 接口设计：<METHOD> <path>
- 调用方/场景：
- 请求 Schema：（字段表：名/类型/必填/约束/示例）
- 响应 Schema：（含 envelope）
- 错误码：A0100 / A0400 / [新增 XXXX，登记 PR #]
- 幂等：是/否，实现方式
- 安全：鉴权方式 + 敏感字段脱敏点
```

## 打回条件（出现任一即不通过）

- GET 修改数据；URL 含动词驼峰；JSON 字段非 lowerCamelCase
- 出入参无校验；分页无上限
- FastAPI 的 GET 把查询参数放入 request body；结构化响应缺少 `response_model` 或等价 Schema；204 响应仍返回 body
- 200 + 错误码组合；message 暴露堆栈/表名/SQL
- 新错误码未登记；敏感字段未脱敏
