# Control Protocol Spec v1

## 目标

定义 Flowy 的统一控制协议：

- CLI 可调用
- WebSocket 可直接发送
- 手机端 runtime 可统一接收
- 事件反馈可统一被 CLI / WS 消费

这份协议是 **控制面真源**，不是某个平台的临时接口。

---

## 一、核心原则

### 1. CLI 和 WebSocket 共享同一套协议模型

CLI 不是第二套协议。

CLI 的作用只是：

- 把人类输入
- 或自动化脚本输入
- 变成同一个 `CommandEnvelope`

而 WebSocket：

- 直接传输同一个 `CommandEnvelope`

---

### 2. 反馈统一成 `EventEnvelope`

手机端运行时不应该：

- 一套日志给 WS
- 一套输出给 CLI

必须统一为同一个事件模型：

- CLI 查看 / 订阅
- WS 推送 / 订阅

---

## 二、目录结构

建议固定为：

```text
packages/protocol/control/
├─ README.md
├─ command-envelope.example.json
├─ event-envelope.example.json
└─ route-map.md
```

当前阶段至少先落：

- `README.md`
- `command-envelope.example.json`
- `event-envelope.example.json`

---

## 三、统一命令模型

建议第一版命令信封：

```json
{
  "schemaVersion": "flowy-control-command-v1",
  "requestId": "req_20260422_0001",
  "sentAt": "2026-04-22T18:00:00+08:00",
  "source": {
    "kind": "cli",
    "clientId": "flowyctl-local"
  },
  "target": {
    "deviceId": "OP5DC1L1"
  },
  "command": {
    "kind": "workflow.step.run",
    "name": "open_xhs_search",
    "runId": "run_20260422_0001",
    "timeoutMs": 15000,
    "payload": {
      "workflowStepId": "open_xhs_search"
    }
  }
}
```

---

## 四、字段定义

### `schemaVersion`
- 命令协议版本

### `requestId`
- 全链路唯一请求号

### `sentAt`
- 请求发出时间

### `source`
- 谁发起的命令

建议 `source.kind`：

- `cli`
- `ws`
- `agent`
- `system`

### `target.deviceId`
- 目标手机 runtime

### `command.kind`

第一版建议支持：

- `operation.run`
- `workflow.step.run`
- `observer.capture`
- `upgrade.check`
- `upgrade.apply.runtime`
- `upgrade.apply.apk`
- `logs.fetch`

### `command.name`
- 具体命令名或 step 名

### `command.runId`
- 运行实例 id

### `command.timeoutMs`
- 执行超时

### `command.payload`
- 命令负载

---

## 五、统一事件模型

建议第一版事件信封：

```json
{
  "schemaVersion": "flowy-control-event-v1",
  "eventId": "evt_20260422_0001",
  "emittedAt": "2026-04-22T18:00:01+08:00",
  "requestId": "req_20260422_0001",
  "runId": "run_20260422_0001",
  "device": {
    "deviceId": "OP5DC1L1",
    "runtimeVersion": "0.1.0020"
  },
  "event": {
    "type": "workflow.step.succeeded",
    "status": "ok",
    "message": "step finished",
    "data": {
      "pageSignature": "e0644857b0d8"
    }
  },
  "artifacts": [
    {
      "kind": "page-context",
      "fileName": "page-context.json"
    }
  ],
  "error": null
}
```

---

## 六、事件类型

第一版建议保留这些事件：

- `runtime.connected`
- `runtime.disconnected`
- `command.received`
- `command.started`
- `command.finished`
- `page.observed`
- `filter.matched`
- `anchor.pre.checked`
- `operation.started`
- `operation.finished`
- `anchor.post.checked`
- `workflow.step.succeeded`
- `workflow.step.failed`
- `upgrade.available`
- `upgrade.applied`

---

## 七、CLI 与 WS 的关系

### CLI

CLI 做两件事：

1. 发送 `CommandEnvelope`
2. 查询 / 订阅 `EventEnvelope`

CLI 只是“人类友好入口”，不是协议真源。

---

### WebSocket

WebSocket 做三件事：

1. runtime connect / hello
2. 接收 `CommandEnvelope`
3. 推送 `EventEnvelope`

WS 是 transport，不是第二套模型。

---

## 八、最小路由模型

控制面至少有这些语义路由：

- `command.send`
- `events.stream`
- `events.list`
- `upgrade.check`
- `upgrade.apply`
- `logs.fetch`

CLI 与 WS 只是访问这些语义路由的两种入口。

---

## 九、错误模型

命令失败时，统一返回：

```json
{
  "code": "anchor_post_failed",
  "message": "post anchor not satisfied",
  "retryable": false
}
```

规则：

- 禁止静默失败
- CLI 与 WS 错误结构必须一致

---

## 十、当前接受的设计结论

1. CLI 与 WebSocket 共用 `CommandEnvelope` / `EventEnvelope`。
2. CLI 是控制面的包装入口，不是独立协议。
3. WebSocket 是 runtime 长连接通道，不是独立执行模型。
4. 控制协议先服务：
   - operation
   - workflow step
   - observer capture
   - upgrade
5. 任何控制面实现都必须先复用这套协议真源。
