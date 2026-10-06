# 公共错误码包（种子代码）

与 `rules/api/status-codes.md` 的登记表一一对应的可直接引用实现。**错误码治理从文档变成代码**：新增错误码 = 先改规范登记表（PR）→ 再改这里的枚举。

```
seeds/common-error/
├── java/com/rd/common/error/
│   ├── ErrorCode.java               # 错误码枚举（全量）
│   ├── BusinessException.java       # 业务异常
│   ├── Result.java                  # 统一响应包 envelope
│   └── GlobalExceptionHandler.java  # Spring 全局异常处理（示例）
└── python/rd_common/
    ├── __init__.py
    ├── errors.py                    # ErrorCode 枚举 + BusinessError
    ├── responses.py                 # envelope 构造器
    └── handlers.py                  # FastAPI 异常处理器注册（示例）
```

## 接入方式

- **Java**：将 `com/rd/common/error` 拷入公共模块（改包名为 `com.<公司>.common.error`），Maven 坐标与版本规则遵循 `rules/java/java-backend-standard.md` §13；协议为 lowerCamelCase，与 Java 属性同名，无需任何 Jackson 命名策略配置。
- **Python**：将 `rd_common` 拷入项目 `src/` 或发内部包；FastAPI 项目在入口调用 `register_exception_handlers(app)`；业务模型的 pydantic 基类统一配置 `alias_generator=to_camel`，保证出参 lowerCamelCase。

## 新增错误码流程

1. 在 `rules/api/status-codes.md` §2 登记表加行（PR 评审查重）
2. 同步扩展 Java `ErrorCode` 与 Python `ErrorCode` 枚举
3. 语义只增不改：废弃旧码保留映射至少一个大版本
