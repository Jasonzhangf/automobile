# packages/protocol/control

WebSocket 协议真源 (Go + Kotlin + 任何未来 client 共享)。

## 文件

- `*.schema.json` — JSON Schema 2020-12 真源。所有 client (Go daemon, Kotlin Android daemon) 必须消费同一份 schema。
- `*.example.json` — 示例 payload，供开发/测试参考。

## 用途

- `CommandEnvelope` (command-envelope.schema.json): Mac → Android WS 命令
- `ResponseEnvelope` (response-envelope.schema.json): Android → Mac WS 响应
- `ErrorObject` (error-object.schema.json): 错误结构
- `ArtifactDescriptor` (artifact-descriptor.schema.json): artifact 元数据
- `LogEntry` (log-entry.schema.json): 日志条目

## 验证

- Go: `services/mac-daemon/src/proto/validate.go::ValidateCommandEnvelope()`
- Kotlin: 通过第三方 JSON Schema validator (待实现)
- 任何 schema 变更必须同步更新 Go struct (`services/mac-daemon/src/proto/types.go`) + Android Kotlin client

## 升级

修改 schema 后必须:
1. 更新 Go struct
2. 更新 Kotlin data class
3. 增加或更新测试
4. 在 PR 中明确标注 BREAKING
