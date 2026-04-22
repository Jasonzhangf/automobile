# exp01 JSON 真源规格

## 目标

定义 Experiment 01 当前唯一有效的 JSON 结构真源。

当前阶段：
- 只定义实验协议
- Phase A 已真机验证：`ping` / `fetch-logs`
- Phase B 已冻结规格：`capture-screenshot` / `dump-accessibility-tree`
- 真源仍放在文档中，不提前抽成正式 schema 工程

Experiment 01 代码实现必须与本文件保持一致。

## 真源边界

当前唯一真源文档：

```text
docs/experiments/exp01-json-truth-spec.md
```

未来只有在 Experiment 01 全部 gate 通过后，才允许迁移到：

```text
packages/protocol/
```

## 1. client_hello

### JSON
```json
{
  "type": "client_hello",
  "protocolVersion": "exp01",
  "deviceId": "android-local-01",
  "runtimeVersion": "0.1.0001",
  "appId": "com.flowy.explore",
  "capabilities": [
    "ping",
    "fetch-logs",
    "capture-screenshot",
    "dump-accessibility-tree"
  ],
  "sentAt": "2026-04-21T16:00:00+08:00"
}
```

### 字段要求
- `type`: 固定 `client_hello`
- `protocolVersion`: 固定 `exp01`
- `deviceId`: 本设备实验 id
- `runtimeVersion`: 当前运行版本
- `appId`: 当前实验 app id
- `capabilities`: 当前支持命令列表
- `sentAt`: 发送时间

### 能力声明规则
- `capabilities` 必须只声明当前真实已启用的命令
- Phase A 可以只声明 `ping` / `fetch-logs`
- Phase B 完成后，才允许声明 screenshot / accessibility 两个命令

## 2. command_envelope

### JSON
```json
{
  "protocolVersion": "exp01",
  "requestId": "req_20260421_161003_0001",
  "runId": "2026-04-21T16-10-03_ping_boot-check",
  "command": "ping",
  "sentAt": "2026-04-21T16:10:03+08:00",
  "timeoutMs": 10000,
  "payload": {}
}
```

### 字段要求
- `protocolVersion`: 固定 `exp01`
- `requestId`: 单次命令唯一 id
- `runId`: 单次实验运行 id
- `command`: 仅允许 `ping | fetch-logs | capture-screenshot | dump-accessibility-tree`
- `sentAt`: 发送时间
- `timeoutMs`: 超时时间
- `payload`: 命令参数对象

### 命令集合规则
- 协议保留命令集合：
  - `ping`
  - `fetch-logs`
  - `capture-screenshot`
  - `dump-accessibility-tree`
- 运行期是否真的允许，要同时受 `client_hello.capabilities` 约束
- 如果 Mac 对未声明 capability 的设备下发命令，必须显式报错

## 3. response_envelope

### JSON
```json
{
  "protocolVersion": "exp01",
  "requestId": "req_20260421_161003_0001",
  "runId": "2026-04-21T16-10-03_ping_boot-check",
  "command": "ping",
  "status": "ok",
  "startedAt": "2026-04-21T16:10:04+08:00",
  "finishedAt": "2026-04-21T16:10:04+08:00",
  "durationMs": 120,
  "device": {
    "deviceId": "android-local-01",
    "model": "Pixel",
    "androidVersion": "14"
  },
  "app": {
    "packageName": "com.flowy.explore",
    "runtimeVersion": "0.1.0001"
  },
  "artifacts": [],
  "error": null,
  "message": "pong"
}
```

### 字段要求
- `status`: `ok | error | timeout`
- `device`: 设备元数据
- `app`: app/runtime 元数据
- `artifacts`: 产物描述数组
- `error`: 成功时为 `null`，失败时必须为结构化对象
- `message`: 简短说明

### artifact 列表规则
- screenshot / accessibility 命令成功时，`artifacts` 不允许为空
- `artifacts` 必须准确覆盖当前 run 已上传并落盘的文件
- `response.json` 自己不计入 `artifacts` 数组，由 `manifest.json` 单独记录

## 4. error_object

### JSON
```json
{
  "code": "UNSUPPORTED_COMMAND",
  "message": "command capture-screenshot is not enabled on this device",
  "details": {
    "allowedCommands": ["ping", "fetch-logs"]
  }
}
```

### 字段要求
- `code`: 稳定错误码
- `message`: 人类可读说明
- `details`: 可选结构化上下文

### 推荐错误码
- `UNSUPPORTED_COMMAND`
- `TIMEOUT`
- `WS_NOT_CONNECTED`
- `LOG_READ_FAILED`
- `INVALID_PAYLOAD`
- `INTERNAL_ERROR`
- `SCREENSHOT_PERMISSION_NOT_READY`
- `SCREENSHOT_CAPTURE_FAILED`
- `ACCESSIBILITY_SERVICE_DISABLED`
- `ACCESSIBILITY_ROOT_UNAVAILABLE`
- `ACCESSIBILITY_SERIALIZE_FAILED`
- `ARTIFACT_UPLOAD_FAILED`

## 5. artifact_descriptor

### JSON
```json
{
  "kind": "screenshot",
  "fileName": "screenshot.png",
  "contentType": "image/png",
  "sha256": "...",
  "sizeBytes": 123456
}
```

### 字段要求
- `kind`: artifact 类型
- `fileName`: 文件名
- `contentType`: MIME type
- `sha256`: 文件摘要
- `sizeBytes`: 文件字节数

### 当前允许的 `kind`
- `logs`
- `response-json`
- `screenshot`
- `screenshot-meta`
- `accessibility-raw`
- `page-context`

## 6. log_entry

### JSON
```json
{
  "ts": "2026-04-21T16:10:04.123+08:00",
  "level": "info",
  "event": "command_received",
  "requestId": "req_20260421_161003_0001",
  "runId": "2026-04-21T16-10-03_ping_boot-check",
  "command": "ping",
  "message": "received ping command"
}
```

### 字段要求
- `ts`: 时间戳
- `level`: `debug | info | warn | error`
- `event`: 稳定事件名
- `requestId`: 可空，但命令期内必须有
- `runId`: 可空，但命令期内必须有
- `command`: 命令期内必须有
- `message`: 人类可读文本

### 推荐事件名
- `daemon_started`
- `daemon_stopped`
- `ws_connect_started`
- `ws_connect_succeeded`
- `ws_connect_failed`
- `ws_reconnect_scheduled`
- `command_received`
- `command_started`
- `command_finished`
- `command_failed`
- `logs_read_started`
- `logs_read_finished`
- `screenshot_capture_started`
- `screenshot_capture_finished`
- `screenshot_capture_failed`
- `accessibility_dump_started`
- `accessibility_dump_finished`
- `accessibility_dump_failed`
- `artifact_upload_started`
- `artifact_upload_finished`
- `artifact_upload_failed`

## 7. run_manifest

### JSON
```json
{
  "runId": "2026-04-21T16-10-03_ping_boot-check",
  "requestId": "req_20260421_161003_0001",
  "protocolVersion": "exp01",
  "command": "ping",
  "capturedAt": "2026-04-21T16:10:04+08:00",
  "status": "ok",
  "device": {
    "deviceId": "android-local-01",
    "model": "Pixel",
    "androidVersion": "14"
  },
  "app": {
    "packageName": "com.flowy.explore",
    "runtimeVersion": "0.1.0001"
  },
  "files": [
    "response.json",
    "logs.txt"
  ]
}
```

### 字段要求
- `runId`: 与 request 对应
- `requestId`: 与 request 对应
- `protocolVersion`: 固定 `exp01`
- `command`: 当前命令
- `capturedAt`: run 完成时间
- `status`: 结果状态
- `device`: 设备信息
- `app`: app/runtime 信息
- `files`: 当前 run 包含的文件列表

### Phase B 文件要求
- `capture-screenshot` 成功时，`files` 至少包含：
  - `response.json`
  - `logs.txt`
  - `page-context.json`
  - `screenshot.png`
  - `screenshot-meta.json`
- `dump-accessibility-tree` 成功时，`files` 至少包含：
  - `response.json`
  - `logs.txt`
  - `page-context.json`
  - `accessibility-raw.json`

## 8. 命令 payload 规范

### ping payload
```json
{}
```

### fetch-logs payload
```json
{
  "tail": 200
}
```

### `tail` 规则
- 必须为正整数
- 默认值可为 200
- 超出上限时必须截断并写日志

### capture-screenshot payload
```json
{
  "format": "png"
}
```

### capture-screenshot 规则
- `format` 当前只允许 `png`
- 若缺省，默认 `png`
- Phase B 不支持 JPEG / WebP / 局部裁剪

### dump-accessibility-tree payload
```json
{
  "includeInvisible": false
}
```

### dump-accessibility-tree 规则
- `includeInvisible` 默认为 `false`
- Phase B 以当前 active/root snapshot 为准，不做跨窗口聚合

## 9. page_context_json

### JSON
```json
{
  "requestId": "req_20260421_161500_0002",
  "runId": "2026-04-21T16-15-48_capture-screenshot_home-screen",
  "command": "capture-screenshot",
  "capturedAt": "2026-04-21T16:15:48+08:00",
  "app": {
    "packageName": "com.android.settings",
    "windowTitle": "Settings"
  },
  "screen": {
    "widthPx": 1080,
    "heightPx": 2400,
    "rotation": 0,
    "densityDpi": 420
  },
  "runtime": {
    "projectionReady": true,
    "accessibilityReady": true
  }
}
```

### 字段要求
- `requestId`: 当前请求 id
- `runId`: 当前 run id
- `command`: 当前命令
- `capturedAt`: 产物生成时间
- `app.packageName`: 当前前台包名；拿不到时必须写 `null`
- `app.windowTitle`: 可选窗口标题
- `screen`: 当前屏幕几何信息
- `runtime`: 当前感知能力是否 ready

## 10. screenshot_meta_json

### JSON
```json
{
  "requestId": "req_20260421_161500_0002",
  "runId": "2026-04-21T16-15-48_capture-screenshot_home-screen",
  "fileName": "screenshot.png",
  "format": "png",
  "widthPx": 1080,
  "heightPx": 2400,
  "captureMethod": "media-projection",
  "displayId": 0
}
```

### 字段要求
- `fileName`: 当前截图文件名，固定 `screenshot.png`
- `format`: 当前固定 `png`
- `widthPx` / `heightPx`: 最终图片尺寸
- `captureMethod`: 当前固定 `media-projection`
- `displayId`: 当前 display id

## 11. accessibility_raw_json

### JSON
```json
{
  "requestId": "req_20260421_161822_0003",
  "runId": "2026-04-21T16-18-22_dump-accessibility-tree_settings-page",
  "capturedAt": "2026-04-21T16:18:22+08:00",
  "packageName": "com.android.settings",
  "rootNodeId": "n0",
  "nodeCount": 3,
  "nodes": [
    {
      "nodeId": "n0",
      "parentId": null,
      "childIds": ["n1", "n2"],
      "className": "android.widget.FrameLayout",
      "packageName": "com.android.settings",
      "viewIdResourceName": null,
      "text": null,
      "contentDescription": null,
      "hintText": null,
      "paneTitle": null,
      "boundsInScreen": {
        "left": 0,
        "top": 0,
        "right": 1080,
        "bottom": 2400
      },
      "flags": {
        "enabled": true,
        "clickable": false,
        "longClickable": false,
        "focusable": false,
        "focused": false,
        "checkable": false,
        "checked": false,
        "selected": false,
        "scrollable": false,
        "editable": false,
        "visibleToUser": true,
        "password": false
      }
    }
  ]
}
```

### 字段要求
- `requestId`: 当前请求 id
- `runId`: 当前 run id
- `capturedAt`: tree 导出时间
- `packageName`: 当前 root package
- `rootNodeId`: 根节点 id
- `nodeCount`: 节点数量
- `nodes`: 扁平数组；保留 parent/child 关系

### 节点字段最小要求
每个 node 至少包含：
- `nodeId`
- `parentId`
- `childIds`
- `className`
- `packageName`
- `viewIdResourceName`
- `text`
- `contentDescription`
- `hintText`
- `paneTitle`
- `boundsInScreen`
- `flags`

## 12. 兼容性规则

### 严格规则
- `protocolVersion != exp01` 时必须显式报错
- 缺少 `requestId` / `runId` / `command` 时必须显式报错
- 不允许吞掉未知字段错误后继续假装成功

### 宽容规则
- 可允许增加新字段，但不能删除当前必需字段
- Mac / Android 双端都必须忽略**已知之外但不冲突**的附加字段

## 13. 当前实现要求

### Phase A 已验证要求
- `ping` 的 request / response 完整符合本文件
- `fetch-logs` 的 request / response 完整符合本文件
- `logs.txt` 中的每条 log entry 都能映射到 `log_entry` 结构
- `manifest.json`、`response.json` 与 runId/requestId 全程一致

### Phase B 冻结要求
- `capture-screenshot` 的 payload / response / artifacts 必须符合本文件
- `dump-accessibility-tree` 的 payload / response / artifacts 必须符合本文件
- `page-context.json`、`screenshot-meta.json`、`accessibility-raw.json` 的字段必须优先服从本文件

## 14. 迁移规则

当且仅当 Experiment 01 全部 gate 通过后：
- 允许将本文件内容迁移到正式 `packages/protocol/`
- 允许用代码 schema / validator 替代文档真源

在此之前：
- 本文档就是唯一真源
- 代码要服从文档，不反过来修改文档
