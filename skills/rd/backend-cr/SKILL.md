---
name: backend-cr
description: 后端代码评审清单（Java / Python），CR 时逐项核对并按严重级别输出意见。Use when reviewing backend pull requests for Java or Python services.
metadata:
  origin: RD-Standards
---

# 后端代码评审（CR）清单

评审 Java / Python 后端 PR 时使用。规范依据：`rules/java/java-backend-standard.md`、`rules/python/python-backend-standard.md`；涉及 SQL 时联动 `sql-review` 技能，涉及表结构联动 `db-schema-review` 技能。

## When to Activate

- 评审后端 Pull Request（人审或 AI 审均可）
- 提交前自检

## 评审原则

1. 只报有把握的问题（>80% 确信是真问题），风格偏好不报（交给 ruff/Checkstyle/P3C）。
2. 每条意见必须能定位到文件与行号，说清触发条件与后果。
3. 同类问题合并成一条（"5 处缺空指针判断"而非 5 条意见）。
4. 按严重级别分级，BLOCKER 不修不合入。

## 严重级别定义

| 级别 | 含义 | 示例 |
|---|---|---|
| BLOCKER | 会造成资损/数据错误/安全漏洞/线上事故 | SQL 注入、并发丢更新、密钥硬编码 |
| HIGH | 高概率故障或严重性能问题 | 无超时的三方调用、事务内 RPC、OOM 风险 |
| MEDIUM | 违反【强制】规范但不直接出事故 | 魔法值、`select *`、缺注释 |
| LOW | 建议 | 命名可优化、可用更简洁写法 |

## 通用检查（两语言都查）

- [ ] 输入校验：对外入口（HTTP/MQ 消息/三方返回）全部校验后才进业务
- [ ] SQL 参数化绑定，无字符串拼接（Java `#{}`；Python 绑定参数）
- [ ] 无密钥/口令硬编码；日志脱敏；错误信息不泄内部细节
- [ ] 日志：SLF4J/标准 logging 占位符写法；error 只记系统错误；带 requestId
- [ ] 异常：不被吞（空 catch/except）；分类处理；最外层转用户可读信息
- [ ] 资源：流/连接关闭（try-with-resources / with）；线程池有界
- [ ] 测试：核心增量逻辑有单测；用例独立、可重复、无外部依赖
- [ ] 金额对外和落库按最小单位用整数（分）；需要小数精度的中间计算使用 `Decimal`；禁止 float/double 运算

## Java 专项（高频强制项）

- [ ] 线程池用 `ThreadPoolExecutor` 显式创建（禁 `Executors`），线程有意义名称
- [ ] POJO 属性包装类型；无 `is` 前缀布尔属性；POJO 有 `toString`
- [ ] 包装类比较用 `equals`；字符串比较 `"常量".equals(var)`
- [ ] `SimpleDateFormat` 未 static 共享（新代码用 `DateTimeFormatter`）
- [ ] foreach 内无 add/remove（用 `Iterator` / `removeIf`）
- [ ] 集合初始化指定容量；`subList`/`Arrays.asList` 陷阱未踩
- [ ] 锁内无 RPC；多资源加锁顺序一致；`@Override` 齐全；无过时 API 调用
- [ ] 命名：类 UpperCamelCase、方法/变量 lowerCamelCase、常量全大写；无拼音混用

## Python 专项（高频强制项）

- [ ] 导入路径稳定且无通配符；入口/跨顶层包用绝对导入，同包内部相对导入保持一致；不依赖运行目录或手工修改 `sys.path`
- [ ] 无裸 `except:`；`assert` 不用于校验外部参数（用 `raise ValueError`）
- [ ] 默认参数无可变对象（`b=[]` → `b=None`）
- [ ] 判 None 用 `is None`；容器判空用隐式假值（`if not users:`）
- [ ] 公开 API 有类型注释；mypy/ruff 通过
- [ ] 异步路径无阻塞调用；长任务进任务队列
- [ ] 无裸 `print`；无 f-string 拼 SQL；`shell=True` 拼用户输入

## 数据库联动检查

- [ ] mapper/py 中 SQL 变更 → 走 `sql-review` 技能清单
- [ ] migration/DDL 变更 → 走 `db-schema-review` 技能清单
- [ ] 更新语句只更新变更字段（无大而全 update）；同步维护 `gmt_modified`
- [ ] 新错误码已登记（`rules/api/status-codes.md`）

## 输出格式

```markdown
## CR 结论：<PR 标题>
结论：通过 / 修改后通过 / 不通过

### BLOCKER（必须修复）
- [文件:行号] 问题描述 → 触发条件与后果 → 修复建议

### HIGH
- ...

### MEDIUM / LOW
- ...

### 亮点（可选，1-2 条）
```

## 打回条件

- 存在任何 BLOCKER
- HIGH 超过 3 条且集中在同一模块（设计问题，退回重做）
- 无测试的核心业务变更
