# Flowy Module Boundaries & Layer Contracts

> True source: 2026-07-01 architecture review
> Applies to: both Go (mac-daemon) and Kotlin (Android daemon-lab)

---

## 1. Layer Stack (必须遵守)

```
┌─────────────────────────────────────────┐
│       flows (编排)                       │
│  职责: 状态机推进、多 block 串联、run 生命周期 │
│  允许: 调 blocks + foundation + state    │
│  禁止: 直接构造 proto (makeCommand)       │
│       直接操作 session SendMu            │
│       重复实现 block 逻辑                 │
├─────────────────────────────────────────┤
│       blocks (能力块)                    │
│  职责: 单能力闭环，不承载长流程决策          │
│  允许: 调 foundation + transport interface │
│  禁止: 知道 session/app 细节              │
│       调 state 包 (除 transport 接口)     │
│       直接构造 proto.CommandEnvelope     │
├─────────────────────────────────────────┤
│       foundation (纯函数)                │
│  职责: 纯函数、数据变换、无状态             │
│  允许: 无                         │
│  禁止: 调 state / blocks / flows / proto  │
│       直接依赖 Android runtime 或 Go 运行时│
├─────────────────────────────────────────┤
│    ui / runtime (Android 专有)           │
│  职责: UI 渲染、系统服务交互               │
│  允许: 调 blocks + foundation            │
│  禁止: 承载业务流程逻辑                   │
├─────────────────────────────────────────┤
│    state (Go 专有)                      │
│  职责: 运行时会话 + pending map + active run│
│  允许: 被 blocks(通过 transport) / flows 调│
│  禁止: 调 foundation / proto / blocks    │
├─────────────────────────────────────────┤
│    proto (协议类型)                      │
│  职责: 共享 JSON schema 类型定义           │
│  允许: 被 blocks / flows / foundation 调  │
│  禁止: 调任何其他包，纯类型                 │
└─────────────────────────────────────────┘
```

## 2. 控制信号 vs 数据信号

### 定义

| 类别 | 定义 | 示例 |
|------|------|------|
| **控制信号** | 影响系统状态转移、流程决策、生命周期 | `requestId`, `runId`, `session lock`, `stopCh`, `pending ch`, `ActiveRun` |
| **数据信号** | 业务载荷、用户数据、设备状态 | `UiNode[]`, `FilterSpec`, `AnchorResult`, `LogEntry`, `Artifact` |
| **配置信号** | 运行时不变的参数化输入 | `CollectionConfig`, `CollectionProfile`, `AnchorConfig` |

### 流动规则

1. **控制信号不进入 payload**
   - `CommandEnvelope.Payload` 只能含数据信号
   - `ResponseEnvelope.InlineLogs` 只含数据日志
   - metadata / error / debug 永远走 side-channel carrier，不得混入 normal payload

2. **数据信号不附着控制语义**
   - `UiNode` 不携带 `requestId` / `runId`
   - `FilterSpec` 不携带 session 状态
   - `AnchorResult` 不携带超时/重试计数器

3. **配置信号不入 runtime 状态**
   - `CollectionConfig` 不在 `AppState` 中持久化
   - `CollectionProfile` 只用作 flows 输入参数

### 当前违反

| 位置 | 问题 | 修法 |
|------|------|------|
| `blocks/anchor_block.go:42` | 接收 `*state.ClientSession` — 控制信号泄漏到 blocks | 改为 `transport.Roundtrip(cmd) error` |
| `blocks/operation_block.go:31` | 接收 `*state.AppState` — 允许产生 `NextRequestSeq()` | 序列号由 transport 注入 |
| `blocks/recovery_block.go:39` | 接收 `*state.AppState` — 数据查询越界 | 改为 `transport.HealthCheck() bool` |
| `docs/function-map.md` 和 `docs/architecture/function-map.yml` | 无控制/数据信号标记 | 补充 `control_signal` / `data_signal` 字段 |

---

## 3. Transport Interface (Go)

理想情况下 blocks 只依赖 `Transport` 接口，而非 `*state.ClientSession + *state.AppState`：

```go
package blocks

type Transport interface {
    // Roundtrip sends a command and waits for the response.
    Roundtrip(cmd proto.CommandEnvelope) (*proto.ResponseEnvelope, error)
    // Send writes a message without waiting for response.
    Send(cmd proto.CommandEnvelope) error
    // Client returns whether a device is connected.
    Client(deviceID string) bool
    // NextSeq returns the next request sequence number.
    NextSeq() int64
}
```

当前 state: **未落地，需重构**。

---

## 4. Android 侧边界

Kotlin 侧已有较清晰边界:
- `blocks/` 只依赖 `foundation/` + `runtime/` (系统服务)
- `flows/` 只依赖 `blocks/` + `foundation/`
- `foundation/` 零依赖（纯函数）
- `ui/` 只依赖 `runtime/` + `foundation/`
- `runtime/` 系统接入层 (AccessibilityService, Service, Projection)

### 注意

- `blocks/FilterTargetsBlock.kt` 依赖 `foundation/AccessibilityTargeting.kt` ✅
- `blocks/TapBlock.kt` 依赖 `runtime/AccessibilitySnapshotStore` 和 `runtime/FlowyAccessibilityService` — **越界**，应改为依赖 `foundation/` 的抽象
- `blocks/blocks` 名称与 Go 侧 `services/mac-daemon/src/blocks` 重名但语义不同 — 是真实的"双侧独立实现"问题

---

## 5. 双边一致性要求

| 概念 | Go 侧 | Kotlin 侧 | 必须一致? |
|------|-------|-----------|-----------|
| CommandEnvelope Schema | `src/proto/types.go` | `WsClientAdapter.kt` | **必须** (shared JSON) |
| FilterSpec | `src/foundation/filter_spec.go` | `AccessibilityTargeting.kt` | **必须** (共享 schema) |
| AnchorSpec | `src/foundation/anchor_spec.go` | `EvaluateAnchorBlock.kt` | **必须** (共享 schema) |
| OperationBackend | `src/blocks/operation_block.go` | `foundation/OperationBackend.kt` | **必须** (共享语义) |
| 拓扑锁 | 无 | 无 | 先统一 |
| 错误链 | 无 | 无 | 先统一 |

## 6. 流水线节点编号

按 routecodex 的 `<Module><Phase><NN><Node>` 模板定义当前 pipeline：

### Go Side (Mac Daemon)

| 节点 | 数据流方向 | 说明 |
|------|----------|------|
| `ServerReq01RawHttp` | inbound | HTTP 请求入口 |
| `ServerReq02ParsedDispatch` | inbound | 已解析的 CommandDispatchRequest |
| `HubWs01CmdSent` | outbound | 命令通过 WebSocket 发出 |
| `HubWs02CmdExecuted` | inbound | 设备执行完毕 |
| `HubResp01ResultReceived` | inbound | 返回结果 |
| `HubResp02ArtifactPersisted` | inbound | artifact 文件落盘 |
| `ServerResp03JsonReturned` | inbound | HTTP 响应返回 |
| `ErrorErr01SourceRaised` | error | 错误入链 |
| `ErrorErr02ClientProjected` | error | 最终 HTTP 错误响应 |

### Kotlin Side (Android)

| 节点 | 数据流方向 | 说明 |
|------|----------|------|
| `DeviceReq01WsConnected` | inbound | 连接建立 |
| `DeviceReq02ClientHelloSent` | inbound | 注册握手 |
| `DeviceReq03CmdReceived` | inbound | 命令到达 |
| `DeviceExec01BlockInvoked` | execution | block 执行 |
| `DeviceExec02ResultReady` | execution | 执行完毕 |
| `DeviceResp01ArtifactUploaded` | outbound | artifact 上传 |
| `DeviceResp02ResponseSent` | outbound | 响应发送 |
