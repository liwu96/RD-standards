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
- [ ] 命名矩阵：JSON/query 为 `lowerCamelCase`，Java 属性同名零转换，Python snake_case + pydantic to_camel 别名

### 3. 定义出入参

按 `rules/api/api-request-response.md`：

- [ ] 入参 Schema（Java Bean Validation / Python pydantic）逐字段标注：必填、类型、范围、示例
- [ ] 分页 `page`/`pageSize`（≤100）；排序 `sort` 白名单校验
- [ ] 写操作带 `idempotencyKey` 或 `requestId` 去重
- [ ] 出参用统一 envelope `{code, message, data, requestId, serverTime}`；列表用 `{total, page, pageSize, list}`
- [ ] 时间 RFC 3339、金额整数（分）、ID 字符串、布尔 `is_`/`has_` 前缀
- [ ] 敏感字段出参脱敏（`158****9119`）

### 4. 选定错误码

按 `rules/api/status-codes.md`：

- [ ] 先查 §2 登记表，已有语义复用，禁止重复注册
- [ ] 需要新码时：`A/B/C 大类 + 4 位数字`，同步 PR 更新登记表与公共枚举（Java `ErrorCode` / Python `ErrorCode`）
- [ ] HTTP 码按映射表返回，禁止 200 + 业务错误
- [ ] message 面向用户可读；内部细节只进日志

### 5. 契约与文档

- [ ] 更新 OpenAPI/接口文档，与代码同步提交
- [ ] 不兼容变更（删字段/改类型/改语义）必须升版本 `/api/v2`；新增可选字段视为兼容

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
- 200 + 错误码组合；message 暴露堆栈/表名/SQL
- 新错误码未登记；敏感字段未脱敏
