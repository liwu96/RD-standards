# 贡献指南（CONTRIBUTING）

本仓库是团队的通用工程规范库：`rules/`（规范）、`skills/`（可执行技能）、`agents/`（评审子代理）、`templates/`（文档模板）、`references/`（源材料）。任何人都可以提 PR 修订。

## 修订流程

1. **提 issue 先行（争议性条目）**：修改现有【强制】条目或新增有取舍的规范，先开 issue 说明动机（线上故障复盘编号 / 新技术栈引入 / 条目失效），讨论收敛后再动文件
2. ** fork/branch → 修改 → PR**：分支命名与提交格式遵循本仓库 `rules/common/git-workflow.md`
3. **PR 描述必须包含**：
   - 修改了什么、为什么（依据来源：故障编号 / 源材料条目 / 业界实践）
   - 影响面：哪些语言/领域受影响，是否需要通知全员
   - 连带更新：新增错误码是否更新 `rules/api/status-codes.md` 登记表；是否需要同步修改引用它的 skill/agent
4. **评审与合入**：至少 1 名技术负责人 Approve；规范级变更合入后在团队群公告

## 写作约定

- 约束级别只用三种：【强制】/【推荐】/【参考】，语义与《阿里巴巴 Java 开发手册》一致
- 每条尽量带正例/反例或一行说明；多来源冲突时标注出处并写明取舍理由
- 条目编号在章节内递增，引用其他条目用"文件 §节"定位（如 `rules/database/mysql-standard.md` §6）
- 中文书写，专有名词与代码保持英文
- 单文件建议 ≤500 行，超限拆分（如数据库规范可拆子领域文件）

## 新增内容的归类判断

| 你要加的东西 | 放哪里 | 判断标准 |
|---|---|---|
| "应该怎么做"的知识/约束 | `rules/<领域>/` | 长期有效的规则，读的 |
| "反复执行的动作+步骤+卡点" | `skills/rd/<name>/SKILL.md` | 有流程、有通过/打回标准 |
| 评审角色（给 AI 子代理或专人） | `agents/<name>.yaml` | TeamAI canonical agent，含 name/description/instructions |
| 文档骨架（ADR/方案/OpenAPI） | `templates/` | 填空即用的模板 |
| 可直接引用的基线代码 | `seeds/` | 新项目拷入即用的种子实现 |
| 支撑规范的原始材料 | `references/` | 溯源用，只读 |

## 源材料与许可

`references/` 的来源与许可说明见 [`references/README.md`](./references/README.md) 和 [`ATTRIBUTION.md`](./ATTRIBUTION.md)。仓库组织结构参考 [teamai-hub/teamai-template](https://github.com/teamai-hub/teamai-template)；本仓库只保留已核对许可的改编内容。`skills/rd/tdd-workflow` 改编自 [obra/superpowers](https://github.com/obra/superpowers) 的 test-driven-development skill（MIT）；部分评审素材参考 [everything-claude-code](https://github.com/affaan-m/everything-claude-code)（MIT）。引用的外部材料版权归原作者所有，未经许可的全文不应提交到 `references/`。

## TeamAI 资源约定

- 代理定义使用 TeamAI canonical YAML：`agents/<name>.yaml` 必须包含 `name`、`description`、`instructions`；修改代理时同步检查 `manifest/roles.yaml` 的命名空间映射。
- `teamai init <repo-url> --role backend`、`teamai pull` 和 `teamai doctor` 是本仓库的最小引导流程；真实环境变量与密钥不得写入 Git。
- OpenAPI 模板中的响应 envelope、写操作幂等键和环境 server 变量是可复制的基线；例外必须在 PR 描述中说明。
- 修改 `references/` 前先核对来源、再分发许可和是否包含敏感内容；优先提交索引、摘要或可公开链接。

## 季度治理

- 每季度由技术负责人组织一次回顾：清理失效条目、合并重复、登记待办
- CI 静态检查规则与本仓库条目的对应关系同步维护（新增【强制】项时评估是否可工具化卡点）
