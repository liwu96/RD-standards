# Python 后端开发规范

> 依据：Google Python 风格指南·语言规范中文版（全文提取见 `references/google-python-styleguide-language-rules.md`），结合团队工程实践整理。
> 约束级别：【强制】/【推荐】/【参考】。语言规范条目忠实于 Google 指南的"决定"部分；工程实践部分为团队补充。

## 一、语言规范（源自 Google 风格指南）

### 1. Lint

- 【强制】代码必须通过 pylint（或团队统一以 ruff 承担 lint）检查；抑制告警必须用行注释并说明原因：

```python
def do_put(self):  # WSGI 接口名, 所以 pylint: disable=invalid-name
    ...
```

- 【强制】"参数未使用"通过在函数体开头 `del` 掉并注明"未使用"消除，或命名 `_`；不用 `unused_` 前缀方案。
- 【参考】禁止 `# fmt: off` 长期关闭格式化，需要时局部说明。

### 2. 导入（import）

- 【强制】只用 `import x` 导入包和模块，禁止 `from x import 函数或类`（以下例外）：
  - `from x import y`：x 是包前缀、y 是模块名（不含前缀）；
  - `from x import y as z`：解决重名或过长名称；
  - `import y as z`：仅限标准公认缩写（如 `np` 代表 numpy）。
- 【强制】禁止相对导入（`from . import x`），一律使用完整包名。
- 【参考】静态分析相关模块（typing、collections.abc、typing_extensions）允许例外。
- 【强制】导入顺序：标准库 → 三方库 → 本项目，组间空行（isort/ruff-isort 自动处理）。

### 3. 包

- 【强制】使用完整包路径导入每个模块，不依赖 `sys.path` 巧合：

```python
# 正确
from doctor.who import jodie
# 错误：导入结果取决于外部 sys.path
import jodie
```

### 4. 异常

- 【强制】允许但谨慎使用异常：
  - 优先内置异常类表达前置条件错误（参数非法 `raise ValueError`）；
  - **禁止用 `assert` 校验公开 API 的参数**——assert 用于内部正确性，意外情况用 `raise`；
  - 自定义异常继承已有异常类、以 `Error` 结尾、命名不重复（`foo.FooError`）；
- 【强制】禁止裸 `except:`；捕获 `Exception` 仅限：重新抛出，或作为隔离点记录并抑制（如线程最外层兜底）。
- 【强制】try 块内代码量最小化，避免掩盖真实错误。
- 【强制】资源清理放 `finally`，优先用 `with` 上下文管理器。

```python
# 正确
if minimum < 1024:
    raise ValueError(f'最小端口号至少为 1024，不能是 {minimum}.')
```

### 5. 全局变量

- 【强制】避免使用全局变量（可变的模块级变量/类属性）；确需时命名加 `_` 前缀并通过函数暴露访问。
- 【推荐】鼓励模块级常量（全大写下划线：`SIR_LANCELOTS_FAVORITE_COLOR = "blue"`）。

### 6. 嵌套/局部/内部类与函数

- 【参考】仅在需要捕获局部变量（除 self/cls）时使用嵌套函数/类；仅为隐藏而嵌套的，放模块级并加 `_` 前缀以便测试。

### 7. 推导式与生成式

- 【强制】仅用于简单场景：映射表达式、单个 for、过滤表达式各不超过一行；禁止多重 for 与多层过滤，复杂时用普通循环。

```python
# 正确
result = [mapping_expr for value in iterable if filter_expr]
# 错误：双重 for
result = [(x, y) for x in range(10) for y in range(5) if x * y > 10]
```

### 8. 默认迭代器与操作符

- 【强制】容器类型使用默认迭代器和操作符：`for key in adict`、`if obj in alist`、`for k, v in adict.items()`；不用 `adict.keys()`、`afile.readlines()`。

### 9. 生成器

- 【推荐】按需使用生成器；docstring 用 "Yields:" 而非 "Returns:"。
- 【推荐】占用大量资源的生成器用上下文管理器包裹确保清理（PEP-0533）。

### 10. Lambda

- 【推荐】仅用于单行函数（60~80 字符内）；常见操作用 `operator` 模块（`operator.mul` 优于 `lambda x, y: x * y`）；超长改为常规 def。

### 11. 条件表达式（三元）

- 【强制】真值分支、if 部分、else 部分各不超过一行，复杂逻辑用完整 if 语句。

### 12. 默认参数值

- 【强制】默认值禁止可变对象（list/dict/`time.time()`/flags.value）：

```python
# 正确
def foo(a, b=None):
    if b is None:
        b = []
# 错误
def foo(a, b=[]):
    ...
```

### 13. 特性（property）

- 【推荐】`@property` 仅用于简单计算/逻辑的读取设置，保持轻量、直白；只读写内部属性的不用 property（直接公开属性）；不用于子类可覆写的计算。

### 14. True/False 求值

- 【强制】用隐式假值（`if not users:` 优于 `if len(users) == 0:`）；
- 【强制】判断 None 必须 `is None` / `is not None`（默认值参数可能传入布尔语义为假的值）；
- 【强制】禁止用 `==` 比较布尔与 False（用 `if not x:`）；需区分 False 与 None 用 `if not x and x is not None:`；
- 【参考】整数与 0 显式比较（len() 返回值例外）；注意字符串 `'0'` 为真、numpy 数组用 `.size` 判空。

### 15. 词法作用域

- 【参考】允许使用；注意函数体内任何位置的赋值都会把该标识符变为局部变量（经典 for 循环变量遮蔽外层问题）。

### 16. 装饰器

- 【推荐】仅在显著优势时审慎使用；装饰器不得依赖文件/套接字/数据库连接等外部环境（导入时执行）；为装饰器编写单测。
- 【强制】禁用 `staticmethod`（改模块级函数）；`classmethod` 仅用于具名构造函数与修改进程内共享状态。

### 17. 线程

- 【强制】不依赖内置类型的原子性；线程间数据传递优先 `queue.Queue`，其次 `threading` 锁原语，优先 `threading.Condition` 替代低级锁。

### 18. 威力过大的功能

- 【强制】避开：自定义元类、字节码操作、运行时动态编译/动态继承、`__del__` 自定义清理、反射滥用（`getattr` 链）、修改系统内部状态。标准库内部使用这些机制的类（`abc.ABCMeta`、`dataclasses`、`enum`）可以正常使用。

### 19. from __future__ imports

- 【推荐】在需要平滑升级语法时使用 `from __future__ import ...`，逐文件启用；不再支持旧版本时删除。

### 20. 类型注释

- 【强制】公开 API（函数签名、类属性）必须带类型注释（PEP-484/526），新代码构建流程启用 mypy/pyright 静态检查。

```python
def func(a: int) -> list[int]: ...

name: str = some_func()
```

## 二、工程实践（团队补充）

### 21. 代码风格与命名

- 【强制】统一 ruff（lint + format，或 black + isort 组合）在 pre-commit 与 CI 强制执行；行宽 100（black 默认 88 可按项目覆盖，但同仓库一致）。
- 【强制】命名遵循 PEP-8：模块/包/函数/变量 `snake_case`、类 `PascalCase`、常量 `UPPER_SNAKE_CASE`、内部实现 `_` 前缀。Python 代码保持 snake_case；协议层 lowerCamelCase 由 pydantic `alias_generator=to_camel` + `populate_by_name=True` 统一转换（见《API 命名规范》§6 命名矩阵）。
- 【推荐】单文件不超过 800 行；按领域组织包，不按类型（不建 `utils/models/everything` 垃圾抽屉）。

### 22. 项目结构

- 【强制】采用 src 布局与 pyproject.toml 管理：

```
project/
├── pyproject.toml        # 元数据 + 依赖 + 工具配置
├── uv.lock / requirements.lock   # 锁定文件必须提交
├── src/
│   └── mypkg/
│       ├── __init__.py
│       ├── api/          # 路由/视图
│       ├── service/      # 业务逻辑
│       ├── repository/   # 数据访问（SQL 集中于此）
│       ├── models/       # pydantic Schema + ORM Model
│       └── core/         # 配置、日志、错误码
└── tests/                # pytest，目录结构与 src 对应
```

- 【强制】依赖用锁文件（uv/pip-tools/poetry）固化并提交；禁止提交 `.venv`、`__pycache__`；虚拟环境隔离项目依赖。
- 【强制】Python 版本跟随团队基线（当前 3.10+），`python_requires` 显式声明；新项目不引入对已 EOL 版本的兼容。

### 23. 数据校验与序列化

- 【强制】对外出入参统一 pydantic（FastAPI 天然集成），Schema 命名 `XxxCreateRequest` / `XxxResponse` / `XxxItem`。
- 【强制】所有外部输入（HTTP、MQ 消息、三方返回）先经 Schema 校验再进入业务；Schema 属性与 DB 字段同名 snake_case（禁止手工改名），对外序列化经 to_camel 别名输出 lowerCamelCase。

### 24. 异常与错误码

- 【强制】业务错误抛自定义异常（继承业务 `BaseError`，含 `ErrorCode` 枚举，见 `rules/api/status-codes.md`），由统一异常处理器转 Response envelope；禁止在业务代码中直接拼 dict 返回错误。
- 【强制】后台线程/任务最外层必须兜底捕获并记录日志，防止静默失败。

### 25. 日志

- 【强制】使用标准 logging（structlog 可选），禁止裸 `print` 调试代码入库。
- 【强制】日志携带 `requestId`（contextvars + middleware 注入，对应协议字段），与出入参规范贯通；日志级别使用规范与 Java 侧一致（error 仅系统错误、参数类问题 warn）。

### 26. 测试

- 【强制】测试框架 pytest；单测不依赖外部服务（数据库用事务回滚或 fixture 重建，三方用 mock/respx）。
- 【推荐】核心模块覆盖率 ≥70%（与 Java 侧一致）；用 pytest-cov 在 CI 中卡阈值。
- 【推荐】用例命名 `test_<被测>_<场景>_<预期>`，fixture 复用构造数据。

### 27. 异步与并发

- 【强制】异步框架（FastAPI + httpx.AsyncClient）中禁止在事件循环里执行阻塞调用（同步 IO、重计算），放入 threadpool（`run_in_executor`）或任务队列。
- 【强制】CPU 密集/长任务放 Celery/RQ 等任务队列，HTTP 请求内只做提交与查询。
- 【参考】进程内缓存注意多 worker 一致性，跨进程状态放 Redis。

### 28. 性能与安全

- 【强制】不信任任何外部输入：SQL 一律参数化（SQLAlchemy 绑定参数），禁止 f-string/format 拼 SQL（对应 Java 侧 `#{}` 强制项）；shell 调用禁止 `shell=True` 拼接用户输入。
- 【强制】密钥/口令不入代码库（环境变量或配置中心），日志与异常信息脱敏。
- 【推荐】热路径避免在循环内做重复正则编译（模块级 `re.compile`）、重复建连（连接池复用）。
