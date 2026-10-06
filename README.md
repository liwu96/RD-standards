# RD-Standards · 后端研发规范

互联网软件研发团队的**后端成员开发规范仓库**：覆盖 API 设计、数据库、三方依赖、Java 与 Python 两大技术栈。所有规范均从权威源材料提炼（阿里巴巴 Java 开发手册、Google Python 风格指南、阿里云/腾讯云数据库规范、《MySQL 实战 45 讲》），源材料完整保留在 [`references/`](./references/README.md) 供追溯。

## 快速索引

| 规范 | 文件 | 一句话说明 |
|---|---|---|
| API 命名 | [rules/api/api-naming.md](./rules/api/api-naming.md) | REST 风格、资源复数名词、版本入路径、跨层命名矩阵 |
| API 出入参 | [rules/api/api-request-response.md](./rules/api/api-request-response.md) | 统一响应包 envelope、分页结构、脱敏与兼容策略 |
| 状态码 | [rules/api/status-codes.md](./rules/api/status-codes.md) | HTTP 码 + 业务码（0/A/B/C 五位错误码）枚举与 Java/Python 实现 |
| MySQL 数据库 | [rules/database/mysql-standard.md](./rules/database/mysql-standard.md) | 命名/建表/字段/索引/SQL/事务锁/DDL/高可用/ORM 十一章 |
| 三方使用 | [rules/third-party/third-party-usage.md](./rules/third-party/third-party-usage.md) | 三方库治理、三方服务超时重试熔断、缓存/MQ/任务队列中间件 |
| Java | [rules/java/java-backend-standard.md](./rules/java/java-backend-standard.md) | 阿里手册十五个章节的团队落地版 + 现代 Java 补充 |
| Python | [rules/python/python-backend-standard.md](./rules/python/python-backend-standard.md) | Google 风格指南 20 条语言规则 + 工程实践补充 |

### 可执行技能（skills/）

rules 是"读的规范"，skills 是"反复执行的动作"，由 AI 或人在对应场景按步骤走查（格式兼容 teamai-cli）：

| 技能 | 触发场景 |
|---|---|
| [skills/rd/api-design](./skills/rd/api-design/SKILL.md) | 新增/修改 HTTP 接口的设计走查与错误码登记 |
| [skills/rd/tdd-workflow](./skills/rd/tdd-workflow/SKILL.md) | 实现功能/修 bug 前的 RED-GREEN-REFACTOR 强制循环（改编自 superpowers） |
| [skills/rd/sql-review](./skills/rd/sql-review/SKILL.md) | SQL 上线前评审：静态检查 + explain 卡点 + 事务锁评估 |
| [skills/rd/db-schema-review](./skills/rd/db-schema-review/SKILL.md) | 建表/改表/大表 DDL/数据订正四类变更评审 |
| [skills/rd/third-party-integration](./skills/rd/third-party-integration/SKILL.md) | 三方服务与三方库接入评审（五件套/密钥/对账/灰度） |
| [skills/rd/backend-cr](./skills/rd/backend-cr/SKILL.md) | Java/Python 后端 PR 评审清单与分级输出格式 |

### 通用规范（rules/common/）

与语言/领域无关的横向基线：

| 规范 | 一句话说明 |
|---|---|
| [git-workflow.md](./rules/common/git-workflow.md) | 分支模型、Conventional Commits、MR 卡点、版本标签 |
| [code-review.md](./rules/common/code-review.md) | CR 时机、严重级别、通用质量标准、评审礼仪 |
| [testing.md](./rules/common/testing.md) | 测试金字塔、覆盖率基线（70%/核心 100%）、BCDE、CI 卡点 |
| [security.md](./rules/common/security.md) | 提交前安全清单、密钥/依赖/接口/数据安全、事件响应 |
| [development-workflow.md](./rules/common/development-workflow.md) | 需求→方案→开发→CR→灰度发布→复盘的全流程卡点 |
| [ai-collaboration.md](./rules/common/ai-collaboration.md) | AI 编码工具的信任边界、验证门、提示注入防护 |

### 评审子代理（agents/）、模板（templates/）与种子代码（seeds/）

- `agents/`：[code-reviewer](./agents/code-reviewer.md)、[database-reviewer](./agents/database-reviewer.md)、[security-reviewer](./agents/security-reviewer.md) —— 提示词直接引用本仓库 skills/rules，供 teamai-cli 等 AI 工具做第一轮评审
- `templates/`：[ADR 模板](./templates/adr-template.md)（架构决策记录）、[技术方案模板](./templates/tech-design-template.md)、[OpenAPI 契约样例](./templates/openapi-example.yaml)
- `seeds/`：[公共错误码包](./seeds/common-error/README.md) —— Java 四个类 + Python 包，与 `status-codes.md` 登记表一一对应；新项目直接拷入，错误码治理从文档变成代码

## 全局核心约定（新人必读 5 分钟版）

1. **协议层命名**：URL 小写连字符 + 复数名词（`/api/v1/order-items`）；JSON 字段、query 参数一律 `lowerCamelCase`；Java 代码同名零转换；Python 代码 `snake_case`（pydantic to_camel 别名转换）；数据库字段 `snake_case`（ORM 驼峰映射）。
2. **响应结构**：所有接口返回 `{code, message, data, requestId, serverTime}`；`code="0"` 成功（字符串统一类型）；HTTP 状态码与业务码按映射表配合使用，禁止 200 + 错误。
3. **错误码**：`A` 用户端 / `B` 系统端 / `C` 三方 + 4 位数字；集中定义在公共枚举，禁止魔法值。
4. **数据库**：InnoDB + utf8mb4 + `bigint unsigned` 自增主键 + 三必备字段；索引 `pk_/uk_/idx_`；禁止 `select *`、外键、存储过程、左模糊。
5. **事务**：短事务；事务内禁 RPC/IO；update/delete 必须走索引；并发冲突用乐观锁或 `for update`，死锁要重试。

## 仓库结构

```
RD-standards/
├── README.md                 # 本文件：索引与快速约定
├── CONTRIBUTING.md           # 本仓库的贡献流程与写作约定
├── rules/                    # 规范正文（读的文档）
│   ├── common/               # 通用基线：git-workflow / code-review / testing
│   │                         # security / development-workflow / ai-collaboration
│   ├── api/                  # api-naming / api-request-response / status-codes
│   ├── database/             # mysql-standard
│   ├── third-party/          # third-party-usage
│   ├── java/                 # java-backend-standard
│   └── python/               # python-backend-standard
├── skills/                   # 可执行技能（反复执行的工程动作）
│   └── rd/                   # api-design / tdd-workflow / sql-review
│                             # db-schema-review / third-party-integration / backend-cr
├── agents/                   # 评审子代理（code/database/security-reviewer）
├── templates/                # ADR 模板 / 技术方案模板 / OpenAPI 契约样例
├── seeds/                    # 种子代码（common-error：Java + Python 错误码包）
└── references/               # 源材料与消化笔记（见 references/README.md）
    ├── alibaba-java-manual-1.3.0.md
    ├── aliyun-mysql-standard.md
    ├── tencent-mysql-standard.md
    ├── google-python-styleguide-language-rules.md
    ├── mysql-45-lectures/    # 45 讲全文
    └── mysql-45-notes-*.md   # 四份主题消化笔记
```

结构参考 [teamai-template](https://github.com/affaan-m/everything-claude-code)（rules + skills + agents 的组织方式）；如团队使用 teamai-cli，可直接用本仓库初始化。

## 规范约束级别

沿用《阿里巴巴 Java 开发手册》的分级：

- 【强制】违反将被代码评审（CR）打回，CI 静态检查能覆盖的项已纳入流水线；
- 【推荐】应当遵守，特殊场景可在 CR 中说明后豁免；
- 【参考】供选择时的指引，不做强制。

## 修订与治理

- 规范修订走 PR：修改 `rules/` 必须在 PR 描述中注明依据（源材料条目或线上故障复盘编号）；
- 新增业务错误码必须同步更新 `rules/api/status-codes.md` 的登记表与公共枚举包；
- 每季度回顾一次规范与 CI 检查规则的差距，源材料更新（如 Java 手册新版本）时同步对齐 `rules/java`；
- 有争议的条目在 issue 中讨论，由技术负责人裁决后落文件。
