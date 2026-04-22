# Experiment 01 — Android Daemon 最小闭环

## 目标

验证 Android 最小 daemon runtime 是否能和 Mac daemon 形成一个真实、可回看的最小闭环：

```text
Mac command -> Android receive -> Android execute -> Android report -> Mac persist -> Mac inspect
```

本实验只证明链路成立，不证明正式产品架构已经成立。

## 实验边界

### 本实验要证明
1. Android 端能在后台长时运行
2. Android 端能接收 Mac 端命令
3. Android 端能执行最小感知任务
4. Android 端能回传结果给 Mac 端
5. Android 端能本地记录日志
6. Mac 端能把一次实验结果按 run id 落盘并人工检查

### 本实验明确不做
- 不做正式 APP 脚手架
- 不做复杂 UI
- 不做复杂操作执行
- 不做 partial upgrade
- 不做应用内 APK 升级
- 不做 page model 最终版
- 不做命令生成

## 最小命令集

Experiment 01 只允许 4 个命令：

1. `ping`
2. `capture-screenshot`
3. `dump-accessibility-tree`
4. `fetch-logs`

不在这 4 个命令里的能力，一律后置。

## 参与角色

### Mac daemon
职责：
- 下发命令
- 接收 Android 响应
- 保存 artifact
- 输出 run 目录
- 提供人工检查入口

### Android daemon
职责：
- 后台常驻
- 监听命令
- 执行最小能力
- 记录本地日志
- 上传结果

## 运行拓扑

优先顺序：

1. `adb reverse` / `adb forward`
2. 局域网 HTTP / WebSocket
3. 手工文件导出仅用于兜底，不作为主实验路径

## 目录落点

### 实验文档

```text
docs/experiments/
  experiment-01-daemon-min-loop.md
```

### 实验代码目标目录（仅保留路径）

```text
explore/
  android-daemon-lab/
services/
  mac-daemon/
```

### 实验产物目录

```text
artifacts/
  YYYY-MM-DD/
    <run-id>/
```

## Run ID 规则

格式：

```text
YYYY-MM-DDTHH-mm-ss_<command>_<short-purpose>
```

示例：

```text
2026-04-21T16-10-03_ping_boot-check
2026-04-21T16-15-48_capture-screenshot_home-screen
2026-04-21T16-18-22_dump-accessibility-tree_settings-page
```

## 单次实验的最小流程

### Flow A: ping

```text
Mac send ping
-> Android receive
-> Android log receive/start/end
-> Android return pong + device/runtime metadata
-> Mac persist response under run id
-> Human inspect response + logs
```

### Flow B: capture-screenshot

```text
Mac send capture-screenshot
-> Android receive
-> Android capture screenshot
-> Android persist local log
-> Android upload screenshot + response metadata
-> Mac persist screenshot + manifest + logs
-> Human inspect screenshot validity
```

### Flow C: dump-accessibility-tree

```text
Mac send dump-accessibility-tree
-> Android receive
-> Android fetch active window root
-> Android dump raw tree
-> Android persist local log
-> Android upload raw dump + response metadata
-> Mac persist artifacts
-> Human inspect raw tree availability
```

### Flow D: fetch-logs

```text
Mac send fetch-logs
-> Android receive
-> Android collect recent logs
-> Android upload log payload
-> Mac persist log artifact
-> Human inspect event sequence
```

## 协议 v0.1（实验态）

先用最窄协议，不提前泛化。

### Command Envelope

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

字段要求：
- `protocolVersion`: 当前实验协议版本，固定为 `exp01`
- `requestId`: 单次请求唯一 id
- `runId`: 单次实验运行 id
- `command`: 只能是 4 个最小命令之一
- `sentAt`: Mac 发起时间
- `timeoutMs`: 本次命令超时
- `payload`: 命令参数；无参数时为空对象

### Response Envelope

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

字段要求：
- `status`: `ok | error | timeout`
- `artifacts`: 本次命令关联产物列表
- `error`: 失败时必须有结构化错误；成功时为 `null`
- `message`: 简短结果说明

### Artifact Descriptor

```json
{
  "kind": "screenshot",
  "fileName": "screenshot.png",
  "contentType": "image/png",
  "sha256": "...",
  "sizeBytes": 123456
}
```

允许的 `kind`：
- `screenshot`
- `accessibility-raw`
- `logs`
- `response-json`

## 命令定义

### 1. ping

#### 目的
验证通道、后台存活、最小响应。

#### 输入 payload
```json
{}
```

#### 成功输出要求
- 返回 `message = pong`
- 返回 device metadata
- 返回 runtime version
- 本地日志包含 receive/start/end

#### 失败条件
- 超时
- daemon 未响应
- response 缺少 requestId/runId/status

---

### 2. capture-screenshot

#### 目的
验证 Android 端最小截图能力和 Mac 落盘能力。

#### 输入 payload
```json
{
  "reason": "manual-debug"
}
```

#### 成功输出要求
- 返回 `status = ok`
- 上传一张可打开的图片
- 图片与 run id 对应
- 本地日志包含 capture start/end

#### 失败条件
- 无截图文件
- 截图损坏
- 元数据缺失
- Mac 未落盘

---

### 3. dump-accessibility-tree

#### 目的
验证 active window root 获取与 raw dump 回传。

#### 输入 payload
```json
{
  "includeBounds": true,
  "includeFlags": true
}
```

#### 成功输出要求
- 返回 `status = ok`
- 至少回传一份 raw json dump，或明确记录 root 不可用原因
- 日志中包含 root 获取结果

#### 失败条件
- 请求成功但无 raw dump 也无明确错误
- 返回空成功但没有证据
- 无日志

---

### 4. fetch-logs

#### 目的
验证本地日志体系和远端回收。

#### 输入 payload
```json
{
  "tail": 200
}
```

#### 成功输出要求
- 返回近期日志内容或日志文件
- 包含 lifecycle 与 command 事件
- 能与其它 run 对齐时间线

#### 失败条件
- 无日志
- 时间线缺失
- 日志无法和 requestId/runId 关联

## 本地日志最小格式

每条日志至少包含：

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

推荐事件名：
- `daemon_started`
- `daemon_stopped`
- `command_received`
- `command_started`
- `command_finished`
- `command_failed`
- `capture_started`
- `capture_finished`
- `dump_started`
- `dump_finished`
- `upload_started`
- `upload_finished`
- `network_error`

## Mac 落盘结构

单次 run 最小目录：

```text
artifacts/
  YYYY-MM-DD/
    <run-id>/
      manifest.json
      response.json
      logs.txt
      screenshot.png                # capture-screenshot 成功时
      accessibility-raw.json        # dump-accessibility-tree 成功时
```

### manifest.json 最小字段

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

## 实验成功判据

Experiment 01 只有在下面 4 组都成立时才算通过：

### Gate 1: ping gate
- Mac 能成功发送 `ping`
- Android 在后台返回 `pong`
- Mac 成功落盘 `manifest.json + response.json + logs.txt`

### Gate 2: screenshot gate
- Android 能执行 `capture-screenshot`
- Mac 能收到可打开的 `screenshot.png`
- 日志能对齐对应 requestId/runId

### Gate 3: accessibility gate
- Android 能执行 `dump-accessibility-tree`
- Mac 能收到 `accessibility-raw.json` 或明确失败原因
- 不允许“成功但没有证据”

### Gate 4: log retrieval gate
- `fetch-logs` 能取回近期日志
- 至少覆盖 daemon start + one command lifecycle
- 可以人工串起事件时间线

## 实验失败处理

若失败，必须记录到 `note.md`：
- 失败命令
- 失败时刻
- 失败证据
- 缺失的证据
- 下一步猜测

不允许只写“失败了，待看”。

## 实验退出条件

只有当 4 个 gate 全部通过，才允许进入：

- 正式脚手架讨论
- 正式 shared protocol 抽象
- 正式 Android app 容器搭建
- partial upgrade / APK upgrade 规格实现

## 推荐实现顺序

1. 先打通 `ping`
2. 再打通 `fetch-logs`
3. 再打通 `capture-screenshot`
4. 最后打通 `dump-accessibility-tree`

理由：
- `ping` 先证明通道
- `fetch-logs` 先证明排障能力
- `capture-screenshot` 先证明 artifact 链路
- `dump-accessibility-tree` 最后证明核心感知链路
