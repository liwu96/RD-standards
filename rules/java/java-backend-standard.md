# Java 后端开发规范

> 依据：《阿里巴巴 Java 开发手册》1.3.0（来源索引见 `references/README.md`；未授权全文不随库分发），结合团队工程实践整理。
> 约束级别：【强制】违反会被代码评审打回；【推荐】应当遵守，特殊情况可说明后豁免；【参考】供决策时选择。
> 本文件是手册的团队落地版，条目语义与手册保持一致；完整"说明/正例/反例"请查手册原文。

## 一、命名风格

1. 【强制】命名不得以下划线或美元符号开始/结束。反例：`_name` / `name$` / `Object$`。
2. 【强制】禁止拼音与英文混合，更不允许中文命名。国际通用名（alibaba、taobao、hangzhou）视同英文。
3. 【强制】类名 `UpperCamelCase`（DO/BO/DTO/VO/AO 例外，大写缩写放尾部）。正例：`UserDO` / `XmlService`；反例：`UserDo` / `XMLService`。
4. 【强制】方法名、参数名、成员变量、局部变量统一 `lowerCamelCase`。
5. 【强制】常量全大写下划线分隔，语义完整：`MAX_STOCK_COUNT`，反例 `MAX_COUNT`。
6. 【强制】抽象类 `Abstract`/`Base` 开头；异常类 `Exception` 结尾；测试类以 `XxxTest` 结尾。
7. 【强制】数组定义 `String[] args`，禁止 `String args[]`。
8. 【强制】POJO 布尔属性不加 `is` 前缀（数据库字段用 `is_xxx`，ORM 映射转换），否则部分框架反序列化会出错。
9. 【强制】包名全小写、单数形式，点分隔间有且仅有一个自然语义单词。
10. 【强制】杜绝不规范缩写（`AbsClass`、`condi`）。
11. 【推荐】命名完整表达语义（`PullCodeFromRemoteRepository`）；使用设计模式时在名中体现（`OrderFactory`、`LoginProxy`）。
12. 【推荐】接口方法不加修饰符（`void f();`）；Service/DAO 暴露接口，实现类以 `Impl` 结尾；能力型接口用形容词（`Translatable`）。
13. 【参考】枚举类带 `Enum` 后缀，成员全大写下划线（`ProcessStatusEnum.SUCCESS`）。
14. 【参考】分层方法命名：单个 `get`、多个 `list`、统计 `count`、插入 `save/insert`、删除 `remove/delete`、修改 `update`。
15. 【参考】领域模型：`xxxDO`（表名）、`xxxDTO`（业务领域）、`xxxVO`（展示层）；禁止 `xxxPOJO`。

## 二、常量定义

1. 【强制】禁止魔法值直接出现在代码中。反例：`String key = "Id#taobao_" + tradeId;`
2. 【强制】`long` 赋值用大写 `L`（`2L` 而非 `2l`）。
3. 【推荐】常量按功能归类（`CacheConsts` / `ConfigConsts`），不做一个大而全的常量类。
4. 【推荐】值只在固定范围内变化且带延伸属性时定义为枚举。
5. 【推荐】常量复用分五层（跨应用/应用内/子工程/包内/类内），同一语义的常量全局只定义一次。

## 三、代码格式

1. 【强制】大括号约定：空块 `{}`；非空块左括号前不换行、后换行，右括号前换行，右括号后跟 `else` 不换行、结束必换行。
2. 【强制】`if/for/while/switch/do` 与括号之间加空格；括号内侧不加空格。
3. 【强制】二目、三目运算符两侧各加一个空格。
4. 【强制】4 个空格缩进，禁止 tab。
5. 【强制】单行不超过 120 字符；换行时运算符/点号随下文，逗号后换行，括号前不换行。
6. 【强制】多参数逗号后加空格。
7. 【强制】文件编码 UTF-8，换行符 Unix 格式。
8. 【推荐】不对齐赋值、不用多余空行；不同业务逻辑块之间空一行。
9. 【推荐】团队统一使用 IDE 格式化模板 + Checkstyle/P3C 插件在 CI 中强制检查。

## 四、OOP 规约

1. 【强制】静态成员通过类名访问，不通过对象引用。
2. 【强制】覆写方法必须加 `@Override`。
3. 【强制】可变参数类型一致、放参数列表最后，避免 `Object...`。
4. 【强制】对外接口/二方库依赖的接口不修改方法签名；过时接口加 `@Deprecated` 并说明替代方案。
5. 【强制】不使用过时的类或方法。
6. 【强制】`equals` 用常量或确定有值的对象调用：`"test".equals(object)`；推荐 `java.util.Objects#equals`。
7. 【强制】包装类对象比较一律用 `equals`（`Integer` 缓存区间 -128~127 之外 `==` 为坑）。
8. 【强制】POJO 属性、RPC 参数与返回值用包装类型；局部变量用基本类型。
9. 【强制】POJO 不设属性默认值。
10. 【强制】序列化兼容升级不改 `serialVersionUID`；不兼容升级必须改。
11. 【强制】构造方法禁止业务逻辑，初始化放 `init`。
12. 【强制】POJO 必须实现 `toString`（继承时含 `super.toString`）。
13. 【推荐】`split` 结果按索引访问前检查长度。
14. 【推荐】类内方法顺序：公有/保护方法 → 私有方法 → getter/setter。
15. 【推荐】getter/setter 不加业务逻辑。
16. 【推荐】循环内字符串拼接用 `StringBuilder.append`。
17. 【推荐】按需使用 `final`（不可变类/引用/方法/局部变量）。
18. 【推荐】慎用 `clone`（默认浅拷贝）。
19. 【推荐】访问控制从严：工具类无 public 构造器；仅类内使用的成员/方法一律 `private`。

## 五、集合处理

1. 【强制】重写 `equals` 必须重写 `hashCode`；`Set` 存储对象与自定义 `Map` 键必须同时重写两者。
2. 【强制】`subList` 不可强转 `ArrayList`（是视图）；原集合结构变更会导致子列表抛 `ConcurrentModificationException`。
3. 【强制】集合转数组用 `toArray(T[] array)`，数组大小取 `list.size()`。
4. 【强制】`Arrays.asList()` 返回对象不可 `add/remove/clear`；原数组修改会同步影响该列表。
5. 【强制】`<? extends T>` 不能 add、`<? super T>` 不能 get（PECS 原则）。
6. 【强制】foreach 内不做元素的 remove/add，用 `Iterator`（并发场景对 Iterator 加锁）。JDK8+ 可用 `removeIf`。
7. 【强制】`Comparator` 必须满足自反/传递/对称三性，相等情形必须返回 0。
8. 【推荐】集合初始化指定容量：`initialCapacity = 元素数 / 0.75 + 1`。
9. 【推荐】遍历 Map 用 `entrySet`（或 JDK8 `forEach`），不用 `keySet` 二次遍历。
10. 【推荐】注意各 Map 的 K/V 空值支持差异：`ConcurrentHashMap` K/V 均不允许 null。
11. 【参考】利用 `TreeSet`/`LinkedHashMap` 的有序性；用 `Set` 去重替代 `List.contains` 遍历。

## 六、并发处理

1. 【强制】单例对象及其方法保证线程安全。
2. 【强制】线程/线程池指定有意义的名称，便于回溯。
3. 【强制】线程必须由线程池提供，禁止应用中自行 `new Thread`。
4. 【强制】线程池用 `ThreadPoolExecutor` 显式创建，禁止 `Executors` 工厂方法：
   - `FixedThreadPool`/`SingleThreadPool`：队列 `Integer.MAX_VALUE`，堆积请求致 OOM；
   - `CachedThreadPool`/`ScheduledThreadPool`：最大线程数 `Integer.MAX_VALUE`，创建大量线程致 OOM。
5. 【强制】`SimpleDateFormat` 线程不安全，禁止 static 共享；JDK8+ 用 `DateTimeFormatter`（不可变、线程安全）。
6. 【强制】锁粒度最小化：能无锁不用锁、能锁区块不锁方法体、能对象锁不用类锁；锁内禁止 RPC 调用。
7. 【强制】多资源加锁保持一致顺序，防死锁。
8. 【强制】并发更新同一记录需加锁：应用层锁/缓存锁/数据库乐观锁（version），冲突概率低于 20% 用乐观锁，重试不少于 3 次。
9. 【强制】定时任务用 `ScheduledExecutorService`，不用 `Timer`（单任务异常会终止全部任务）。
10. 【推荐】`CountDownLatch` 的 `countDown` 必须在 finally 中确保执行。
11. 【推荐】多线程随机数用 `ThreadLocalRandom`。
12. 【推荐】双重检查锁的目标属性声明为 `volatile`。
13. 【参考】计数用 `LongAdder`（优于 `AtomicLong`）；`ThreadLocal` 建议 static 修饰且用完 `remove`（防内存泄漏与线程池串数据）。

## 七、控制语句

1. 【强制】`switch` 每个 `case` 用 `break/return` 结束（或注释说明贯穿），必须含 `default`。
2. 【强制】`if/else/for/while/do` 必须使用大括号，即使单行。
3. 【推荐】卫语句（guard clause）优先，`if-else` 不超过 3 层；更复杂的用策略/状态模式。
4. 【推荐】复杂条件提取为具名布尔变量。
5. 【推荐】循环体内避免定义对象、取数据库连接、不必要的 try-catch。
6. 【参考】对外接口、敏感权限入口、执行开销大的方法必须做参数校验；高频底层方法可省略并在文档注明前提。

## 八、注释规约

1. 【强制】类、属性、方法注释用 Javadoc（`/** */`），不用 `//`。
2. 【强制】抽象方法（含接口方法）必须 Javadoc 注释：说明做什么、实现要求、调用注意。
3. 【强制】类必须标注创建者与创建日期。
4. 【强制】方法内单行注释在被注释语句上方；枚举字段必须有注释说明用途。
5. 【推荐】中文注释说清问题，专有名词保持英文；代码修改同步更新注释。
6. 【参考】注释掉的代码如无用直接删除（git 保存历史）；TODO/FIXME 标注人与时间并定期清理。

## 九、异常与日志

### 异常

1. 【强制】可预检查的 `RuntimeException`（NPE、越界）用条件判断规避，不用 catch 处理。
2. 【强制】异常不做流程控制、条件控制。
3. 【强制】禁止大段 try-catch；区分稳定/非稳定代码，按异常类型分别处理。
4. 【强制】捕获异常必须处理，否则抛给调用者；最外层业务必须转为用户可理解的提示。
5. 【强制】事务代码 catch 后需回滚的，必须手动回滚。
6. 【强制】`finally` 关闭资源对象/流（JDK7+ 用 try-with-resources）；finally 中禁止 `return`。
7. 【强制】捕获与抛出的异常类型完全匹配或为其父类。
8. 【推荐】防止 NPE 是调用者责任；远程调用返回、`Map.get`、级联调用、自动拆箱均需判空；用 `Optional` 简化。
9. 【推荐】自定义异常带业务含义（`ServiceException` 等），禁止直接抛 `RuntimeException`/`Exception`/`Throwable`。
10. 【参考】对外 HTTP/API 开放接口用"错误码"（见 `rules/api/status-codes.md`）；应用内部抛异常；跨应用 RPC 用 `Result` 封装 `isSuccess()/code/message`。

### 日志

1. 【强制】使用 SLF4J 门面 API，不直接使用 Log4j/Logback API。
2. 【强制】日志文件至少保留 15 天。
3. 【强制】扩展日志命名 `appName_logType_logName.log`（如 stats/monitor/visit 分类），错误日志与业务日志分开。
4. 【强制】debug/info 级别用占位符 `{}` 或条件输出，避免无效字符串拼接。
5. 【强制】日志输出包含两类信息：案发现场参数 + 异常堆栈。正例：`logger.error("params={} msg={}", ctx, e.getMessage(), e);`
6. 【推荐】生产环境禁止 debug 日志；info 有选择输出；error 只记系统逻辑错误、异常等重大问题，参数错误用 warn。
7. 【推荐】日志需带 traceId/requestId（MDC），与出入参规范中的 `requestId` 贯通。

## 十、单元测试

1. 【强制】遵守 AIR 原则：Automatic（自动化）、Independent（独立性）、Repeatable（可重复）。
2. 【强制】单测全自动、非交互；用 assert 断言，禁止 `System.out` 人肉验证。
3. 【强制】用例之间不互相调用、不依赖执行顺序。
4. 【强制】不依赖外部环境（网络/服务/中间件），依赖用 DI + Mock/内存实现。
5. 【强制】测试粒度至多类级别、一般方法级别。
6. 【强制】核心业务、核心应用、核心模块的增量代码必须单测通过。
7. 【强制】单测代码位于 `src/test/java`。
8. 【推荐】语句覆盖率 ≥70%，核心模块语句+分支覆盖率 100%。
9. 【推荐】用例设计遵守 BCDE：Border 边界值、Correct 正确输入、Design 结合设计文档、Error 强制错误输入。
10. 【推荐】DB 相关单测数据用程序插入，设定自动回滚或加统一前后缀标识。
11. 【推荐】单测在提测前完成，不发布后补。

## 十一、安全规约

1. 【强制】用户所属页面/功能必须做权限控制校验（防水平越权：不能查看/修改/删除他人数据）。
2. 【强制】敏感数据（手机号、身份证）展示必须脱敏。
3. 【强制】SQL 参数严格参数绑定（`#{}`），禁止字符串拼接 SQL。
4. 【强制】用户输入的任何参数必须做有效性验证（防 pageSize 溢出、恶意 order by 慢查询、SQL 注入、ReDoS）。
5. 【强制】输出到 HTML 的用户数据必须安全过滤/转义。
6. 【强制】表单、AJAX 提交执行 CSRF 过滤。
7. 【强制】短信、邮件、支付等平台资源必须有防重放：数量限制、频控、验证码。
8. 【推荐】UGC 场景实现防刷与违禁词过滤。

## 十二、工程结构与分层

1. 【推荐】分层：开放接口层 → Web 层 → Service 层 → Manager 层 → DAO 层。
   - Web 层：转发、基本参数校验、不复用的简单处理；
   - Service 层：具体业务逻辑；
   - Manager 层：三方平台封装（预处理结果、转化异常）、通用能力下沉（缓存方案、中间件）、多 DAO 组合复用；
   - DAO 层：与 MySQL/HBase 等的数据交互。
2. 【参考】分层异常处理：DAO 层 catch(Exception) 转 `DAOException`、不打日志；Service 层必须记日志带参数；Web 层不再上抛，转友好错误页/错误码。
3. 【参考】领域模型：DO（表一一对应）、DTO（Service 对外）、BO（业务对象）、AO（Web-Service 复用）、VO（展示）、Query（查询对象，超过 2 个参数禁止用 Map）。
4. 【推荐】新工程默认按 `controller / service / manager / dao(model) / client(对外 API) / common` 组织模块。

## 十三、二方库依赖（Maven）

1. 【强制】GAV 坐标：GroupId `com.{公司}.{业务线}[.子业务线]` 最多 4 级；ArtifactId `产品线-模块名`。
2. 【强制】版本号 `主.次.修订`：不兼容升级升主版本、兼容加功能升次版本、修 bug 升修订号；起始版本 `1.0.0`；正式版不允许覆盖发布。
3. 【强制】线上应用不依赖 SNAPSHOT（安全包除外）。
4. 【强制】二方库升级保持 jar 仲裁结果不变；变化时用 `dependency:tree` 对比并 `<excludes>` 排除。
5. 【强制】二方库参数可用枚举，但**返回值不允许**使用枚举或含枚举的 POJO。
6. 【强制】同 GroupId+ArtifactId 禁止出现多版本。
7. 【推荐】依赖声明在 `<dependencies>`，版本仲裁统一放父 pom `<dependencyManagement>`；同一框架组的版本用 `${spring.version}` 类变量统一。
8. 【推荐】二方库精简可控：只含 API/领域模型/工具类/常量；不携带配置项与日志实现。

## 十四、JVM 与服务器

1. 【推荐】高并发服务器调小 TCP time_wait（`net.ipv4.tcp_fin_timeout`）；调大最大文件句柄数。
2. 【推荐】JVM 加 `-XX:+HeapDumpOnOutOfMemoryError`。
3. 【推荐】生产环境 `Xms` 与 `Xmx` 设为相同值。
4. 【参考】服务器内部重定向用 forward；外部重定向用 URL 拼装工具类。

## 十五、现代 Java 补充（团队约定，手册 1.3.0 之后的新实践）

1. 【推荐】JDK8+ 时间处理统一 `java.time`（`LocalDateTime`/`Instant`/`DateTimeFormatter`），禁止新增 `Date`/`Calendar`/`SimpleDateFormat` 代码。
2. 【推荐】优先 Stream/Optional 表达清晰的管道与空值语义，但不允许在循环外滥用嵌套 Stream 导致可读性下降。
3. 【推荐】接口返回统一使用团队 Response envelope（见 `rules/api/api-request-response.md`），JSON 序列化默认 lowerCamelCase；普通属性可与协议同名，Java 布尔属性若采用协议的 `is`/`has` 前缀则必须显式配置序列化映射。
4. 【强制】新增代码必须通过团队 CI 中的静态检查（P3C-PMD / SpotBugs / Checkstyle），Blocker 级问题不允许合入。
5. 【推荐】框架选型跟随团队基线（Spring Boot 2.7+/3.x、MyBatis-Plus 或 JPA 按项目约定），新项目不得引入功能重复的同类框架。
