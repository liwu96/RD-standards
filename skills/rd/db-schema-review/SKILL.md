---
name: db-schema-review
description: MySQL 建表、改表、索引变更、大表 DDL 与数据订正的评审流程。Use when creating tables, altering schema, planning DDL on large tables, or preparing data-fix scripts.
metadata:
  origin: RD-Standards
---

# 建表与变更评审

覆盖四类场景：新表设计、表结构变更（DDL）、大表变更、数据订正。规范依据：`rules/database/mysql-standard.md` §1-3 建表/字段、§7 变更与大表 DDL。

## When to Activate

- 新建业务表
- 加/删字段、加/改索引
- 百万行以上大表的任何 DDL
- update/delete 数据订正脚本

## 场景 A：新表设计

逐项核对（全部为【强制】项）：

- [ ] `id bigint unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY`
- [ ] 必备 `id` / `gmt_create` / `gmt_modified`（datetime）
- [ ] InnoDB + utf8mb4；表名小写下划线、单数、见名知义 ≤32 字符、无保留字
- [ ] 所有表和字段有 comment；"是否"字段 `is_xxx unsigned tinyint`
- [ ] 类型：货币金额按最小单位用整数（如 `amount_cents BIGINT`）；确需小数精度的非货币量用 `DECIMAL`；字符串定长 `char`；varchar ≤5000，超长 text 拆扩展表；无 ENUM、无外键、无预留字段、无明文密码
- [ ] 预计 3 年内数据量；>500 万行的规划归档或分表方案（不用分区表）
- [ ] 索引：命名 `pk_/uk_/idx_`；业务唯一字段建 `uk_`；逐条说明查询/唯一性用途、写入成本和无冗余，数量以实际 workload 与执行计划为准

产出建表 SQL 模板：

```sql
CREATE TABLE `trade_order` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`      VARCHAR(32)  NOT NULL COMMENT '业务单号',
  `order_status`  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态:0待支付,1已支付,2已取消',
  `amount_cents`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '金额(分)',
  `is_deleted`    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否,1是',
  `gmt_create`    DATETIME NOT NULL COMMENT '创建时间',
  `gmt_modified`  DATETIME NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易订单表';
```

## 场景 B：表结构变更（DDL 三步走）

1. **查长事务**（DDL 被 MDL 阻塞后，会连带阻塞该表后续所有查询）：

```sql
SELECT * FROM information_schema.innodb_trx ORDER BY trx_started;
```

   发现长事务先确认连接归属、业务影响和是否可安全结束，优先等待自然结束或调整窗口。禁止应用或 AI 直接 `KILL` 会话；确需中止语句时优先由 DBA 按变更流程审批后执行 `KILL QUERY <thread_id>`，关闭连接（`KILL CONNECTION`）还会触发事务回滚，必须单独评估并记录会话 ID，再复查锁等待。
2. **低峰执行**：与 DBA 确认窗口。
3. **设等待上限**：`ALTER TABLE ... NOWAIT` / `WAIT n`，拿不到 MDL 立即放弃重试。

另核对：

- [ ] 加字段：可空或带默认值（禁止对已有表加 NOT NULL 无默认）
- [ ] 改列名/删列：先发代码（双读写或去引用）→ 再发 DDL（expand-contract）
- [ ] 新索引走 `sql-review` 技能的索引核对项

## 场景 C：大表变更（>100 万行）

- [ ] 使用 gh-ost / pt-online-schema-change，禁止直接 ALTER
- [ ] 评估主从延迟与磁盘空间（gh-ost 需要约一倍表空间）
- [ ] 删表流程：先改名加 `_to_be_deleted` 后缀观察 ≥7 天 → 管理系统删除（drop/truncate 无法闪回）

## 场景 D：数据订正

- [ ] 脚本先 `select` 输出影响范围（行数 + 抽样），人工确认后再执行
- [ ] 分批执行：循环 `update/delete ... limit 500`，批间 sleep
- [ ] where 走索引；核对 `sql_safe_updates=on` 不误伤
- [ ] 备份将被修改的行（先导出到备份表 `bak_前缀_日期`）
- [ ] 读写分离环境确认在主库执行；执行后核对从库延迟

## 产出格式

```markdown
## 变更评审：<表名/变更内容>
- 场景：A 建表 / B DDL / C 大表 / D 订正
- 影响行数/表大小：
- 长事务检查：innodb_trx 结果
- 执行窗口与方式：低峰 / gh-ost / 分批脚本
- 回滚方案：
```

## 打回条件

- 建表违反任一【强制】项（无主键、无注释、utf8 非 mb4、float 金额等）
- 大表直接 ALTER；删表未先改名
- 订正脚本无 select 确认、无备份、未分批
