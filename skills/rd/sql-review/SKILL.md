---
name: sql-review
description: SQL 上线前评审流程：新 SQL、慢 SQL 优化、复杂查询改写时使用。包含规范静态检查与 explain 卡点。Use when reviewing SQL before release, optimizing slow queries, or rewriting complex queries.
metadata:
  origin: RD-Standards
---

# SQL 上线前评审

任何将进入生产的 SQL（新接口查询、报表、数据订正脚本）在上线前走本流程。规范依据：`rules/database/mysql-standard.md` §4 索引、§5 SQL 编写、§6 事务与锁。

## When to Activate

- 新增接口/DAO 方法中的 SQL 准备上线
- 慢查询优化、SQL 改写
- 评审他人 SQL（CR 中出现 mapper/py 文件里的 SQL 变更）
- 数据订正脚本（update/delete）发布前

## 步骤

### 1. 静态检查（逐条对照，违反【强制】项直接打回）

- [ ] 无 `select *`，字段明确列出
- [ ] `update`/`delete` 带 where，且 where 条件能走索引（否则锁全表）
- [ ] where 中无列上函数/运算（`month(t)=7`、`id+1=10` 改写掉）
- [ ] 无左模糊/全模糊 `like '%xx'`
- [ ] 无隐式转换：字符串条件带引号；关联两表字符集一致（utf8mb4）
- [ ] `in` 集合 ≤500；同一列多个 `or` 改 `in`
- [ ] join ≤3 张表，关联字段类型一致且有索引
- [ ] 统计用 `count(*)`；`sum` 包 `IFNULL` 防 NPE
- [ ] 无重复值场景用 `union all`；`group by` 不需排序时 `order by null`
- [ ] 禁 `order by rand()`、`truncate`、存储过程、外键

### 2. 索引核对

- [ ] where/order by/group by/join 字段有索引覆盖（对照 `show create table`）
- [ ] 联合索引顺序结合主要查询验证：等值列通常在范围列前，等值列之间按选择性/排序需求取舍，排序列仅在执行计划支持时放后
- [ ] 高频查询评估覆盖索引（免回表）
- [ ] 需要新建索引时：命名 `idx_字段`，说明查询收益与写入成本；无冗余（最左前缀可复用的不建），数量以实际 workload 与执行计划为准
- [ ] 长 varchar 使用前缀索引前算过区分度并记录取舍；短字段、唯一键和覆盖索引场景默认使用全字段索引

### 3. explain 卡点（在预发库执行）

```sql
EXPLAIN <sql>;
```

- [ ] 按查询目的和 SLO 检查 `type`/`key`/`rows`/`Extra`；不设统一 type 下限或必须命中 key，小表/全量查询可接受全表扫描
- [ ] 大表的选择性查询若意外出现 `ALL`/`index` 扫描，或 `rows` 超出预算，先优化或补充说明
- [ ] `rows` 预估与实际量级一致，偏离大先 `ANALYZE TABLE`
- [ ] `Using filesort` / `Using temporary` / `Block Nested Loop` 结合数据量和时延判断，不一律打回
- [ ] 深分页（offset > 1万）改延迟关联或游标分页

### 4. 事务与锁评估

- [ ] 该 SQL 是否在事务内？事务内是否混入 RPC/IO（打回）
- [ ] 批量写分批：单批 ≤500~1000 行循环提交，单事务 ≤1 万行
- [ ] 并发更新热点行：热行更新放事务最后；调用方有死锁重试
- [ ] 只读查询未显式开事务

### 5. 输出结论

```markdown
## SQL 评审结论
- SQL/位置：
- 静态检查：通过 / 不通过（违反项：...）
- explain：type=? key=? rows=? Extra=?
- 索引变更：无 / 新增 idx_xxx（DDL 走 db-schema-review）
- 风险与建议：
```

## 打回条件

- 违反任一【强制】静态检查项
- 大表的选择性查询出现未预期的全表扫描/索引全扫描，且执行计划或实测违反约定 SLO；小表或预期读取大部分数据的全量查询可接受全表扫描
- 大批量写未分批；事务内含 RPC
