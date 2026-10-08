# TeamAI 接入

本仓库按 `teamai-template` 的远程团队资源库布局组织。已提交的角色清单把现有后端命名空间组合起来，根目录评审代理对所有成员共享。

## 成员接入

在需要接入规范的业务仓库根目录安装 `teamai-cli` 后执行一次：

```bash
npm install -g teamai-cli
teamai init https://github.com/liwu96/RD-standards.git --role backend --agent claude --agent codex
teamai pull
teamai doctor
```

如果团队把本仓库复制到自己的命名空间，把命令中的 URL 换成新地址。[teamai-hub/teamai-template](https://github.com/teamai-hub/teamai-template) 是上游参考模板。`--self` 只用于把资源直接提交到业务仓库 `.teamai/` 的场景，本仓库自身不采用该模式。机器数据、令牌、Provider 凭据和私有来源副本必须留在 Git 之外。

## 角色选择

`manifest/roles.yaml` 将 `backend` 角色映射到本仓库的 `rules/{common,api,database,java,python,third-party}`、`skills/rd` 和共享根代理。成员克隆后执行：

```bash
teamai roles list
teamai roles set backend
teamai pull
teamai doctor
```

角色清单变更时，在同一个 PR 中检查对应的命名空间目录。命名空间必须是单一路径段；不要放入斜杠、密钥或机器专属路径。

## 环境变量与密钥

团队仓库中的 `env/env.yaml` 会提交到主分支，只能放非敏感的团队默认值。密钥只声明名称，不要写值；具体值通过开发者 shell 或批准的密钥存储提供。不要把 `.env`、API token、密码、客户数据或 `references/` 本地副本加入团队仓库。

## 新增资源

- 在 `agents/<name>.yaml` 新增或修改 canonical agent，必须包含 `name`、`description`、`instructions`。
- OpenAPI 契约以 [`../templates/openapi-example.yaml`](../templates/openapi-example.yaml) 为基线，保留响应 envelope、每个写操作的幂等键和 server 环境变量。
- 经过评审的资源用 `teamai push` 发布；普通文档和角色清单走仓库正常 PR 流程。
