# Git 工作流规范

> 适用范围：团队所有代码仓库。语言/领域无关的通用规则，与各领域规范（`rules/api|database|java|python`）配合使用。
> 约束级别：【强制】/【推荐】/【参考】。

## 1. 分支模型

- 【强制】主分支：`main`（受保护，禁止直接 push，只接受 MR/PR 合入）。
- 【强制】开发分支从 `main` 拉出，命名：`feat/<issue-id>-<短描述>`、`fix/<issue-id>-<短描述>`、`refactor/<短描述>`、`docs/<短描述>`、`hotfix/<issue-id>-<短描述>`。示例：`feat/1024-order-export`。
- 【推荐】长期分支只保留 `main`；发布分支按需 `release/v<版本号>`，发布后合并回 `main` 并打 tag（`v1.2.0`）。
- 【强制】hotfix 从线上对应 tag 拉出，修复后同时合回 `main` 与发布分支。

## 2. Commit 规范（Conventional Commits）

【强制】提交信息格式：

```
<type>(<scope>): <subject>

<可选 body：为什么改、影响面>
<可选 footer: 关联 issue/BREAKING CHANGE>
```

- type 限定：`feat` / `fix` / `refactor` / `perf` / `test` / `docs` / `ci` / `chore` / `build`
- 【强制】subject 用中文或英文均可但仓库内统一，祈使语气、不超过 50 字符，不加句号
- 【推荐】一个提交只做一件事；依赖升级与业务改动不混提
- 【推荐】CI 接入 commitlint 校验格式

示例：

```
feat(order): 订单导出支持按时间范围筛选

- 新增 createdAt 范围参数（RFC 3339）
- 深分页改游标，导出上限 1 万行
Closes #1024
```

## 3. 合并请求（MR/PR）规范

- 【强制】合并前必须满足：CI 全绿（构建、静态检查、单测）、无冲突、至少 1 名评审人 Approve（核心模块 2 名）
- 【强制】MR 描述必须包含：改动概述、影响面、测试证据（截图/命令输出）、回滚方案
- 【强制】涉数据库变更的 MR 附加 `sql-review` / `db-schema-review` 技能的评审结论（见 `skills/rd/`）
- 【推荐】单个 MR 控制在 400 行 diff 以内（不含自动生成代码与锁文件），大改动拆分
- 【推荐】评审时效：24 小时内响应；阻塞他人时优先处理评审

## 4. 代码合入与历史

- 【强制】禁止 force push 到 `main`、`release/*` 与他人分支
- 【推荐】合入方式仓库统一：一般项目用 squash（保持 main 历史干净）；多人协作长分支用 merge commit
- 【推荐】rebase 自己的分支保持与 main 同步，避免大量冲突合并节点

## 5. 版本与标签

- 【强制】语义化版本 `主.次.修订`（与二方库版本规范一致，见 `rules/java/java-backend-standard.md` §13）
- 【强制】不兼容 API 变更升主版本；每次发布打 tag 并在 CHANGELOG（如有）登记

## 6. 敏感信息

- 【强制】密钥、口令、内网地址禁止提交；误提交后立即轮换密钥（删除提交不能消除泄露），并通知安全负责人
- 【推荐】开启仓库密钥扫描（git-secrets / 平台自带扫描）
