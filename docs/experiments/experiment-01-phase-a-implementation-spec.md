# Experiment 01 / Phase A — 最小实现规格

## 目标

Phase A 只实现两件事：

1. `ping`
2. `fetch-logs`

它的目标不是证明感知已经可用，而是先证明：

```text
Android runtime can stay alive
+ Android can connect to Mac daemon
+ Mac can push one command
+ Android can respond
+ Android local logs can be fetched back
```

如果这一步不稳定，后面的截图和 accessibility 都不应该继续。

## 设计结论

### Android 侧运行形态（Phase A 推荐）

**推荐形态：可见启动入口 + Foreground Service runtime**

```text
DevPanelActivity
  -> start DaemonForegroundService
  -> DaemonForegroundService opens outbound WS to Mac daemon
  -> Mac pushes command over WS
  -> Android executes block/flow
  -> Android returns response over WS
```

### 为什么 Phase A 先不用 AccessibilityService 当主 runtime

原因不是它不能存在，而是当前最小目标是先把：
- 通信
- 长时运行
- 日志
- 命令/响应闭环

先做稳。

因此：
- **Phase A 主 runtime = Foreground Service**
- **Phase B 再引入 AccessibilityService** 处理 `dump-accessibility-tree` 和后续 screenshot / perception

这是一个工程上的拆分，不是否定 AccessibilityService 的核心地位。

## Android 官方约束（已纳入设计）

### 1. 没有“真正的 Linux 式 daemon”

Android 正规后台长时运行组件，当前实验应视为：

- started service / foreground service
- 由系统调度和限制
- 不是无限制常驻进程

### 2. Foreground Service 启动限制

对于目标 Android 12+ 的应用，**不能随便在后台启动 foreground service**；应当从**可见界面**启动。

所以本实验要求：
- 由 `DevPanelActivity` 或明确的可见操作启动 daemon
- 不允许把 Phase A 设计成“静默自启动”

### 3. Android 14+ 必须声明前台服务类型

Phase A 的 foreground service 必须显式声明 service type。

当前建议：
- **开发实验阶段先按 `dataSync` 方向建模**，因为它最接近：
  - 设备与 Mac 之间的数据上传/下载
  - run bundle / logs / command response 的传输

**注意**：这是一个**工程推断**，不是说最终产品一定固定为 `dataSync`。

### 4. Android 15+ 对 `dataSync` 有时长限制

如果目标 Android 15+，`dataSync` foreground service 在后台有 **24 小时内累计 6 小时** 的限制。

因此 Phase A 的真实含义应该是：
- 证明“可长时稳定运行一段时间”
- 不是承诺“无限期常驻”

### 5. Service 默认跑在主线程

Service 回调默认仍在 app 主线程。

因此：
- WebSocket I/O
- 文件写入
- 日志刷盘
- 命令执行

都必须下放到 worker/coroutine/executor，不能直接堵在 `onStartCommand()` 或主线程回调里。

## Phase A 范围

### 要实现
- DevPanelActivity 可启动/停止 daemon
- DaemonForegroundService 建立到 Mac daemon 的出站连接
- Mac daemon 可下发 `ping`
- Android 可返回 `pong`
- Mac daemon 可下发 `fetch-logs`
- Android 可回传最近日志
- Mac 侧能按 run id 落盘

### 不实现
- screenshot
- accessibility dump
- gesture/action
- runtime partial update
- APK update
- 正式 app scaffold
- 正式 shared package 抽象

## 运行通道

### 推荐：单 WebSocket 通道起步

Phase A 推荐只用一个通道：

```text
Android -> outbound WebSocket -> Mac daemon
```

理由：
- Android 主动连 Mac，和 `adb reverse` / LAN 都兼容
- ping / logs 都是小 payload
- 简化 Phase A，不提前引入 HTTP 上传复杂度

### 后续扩展

到 screenshot / accessibility dump 阶段，再引入：
- WebSocket：命令/响应/小日志
- HTTP upload：大 artifact 上传

## Mac daemon 最小接口规格

### 1. WebSocket endpoint

```text
ws://<host>:<port>/exp01/ws
```

用途：
- Android 注册连接
- Mac 向 Android 推送 command envelope
- Android 回送 response envelope
- 可选发送心跳

### 2. Mac daemon 内部职责

最小模块：

```text
services/mac-daemon/
  src/
    foundation/
    blocks/
    flows/
```

#### foundation
- json encode/decode
- request/run id helpers
- file path helpers
- wall-clock/time helpers

#### blocks
- ws accept block
- command queue block
- response persist block
- run manifest persist block
- log artifact persist block

#### flows
- client session flow
- send-command flow
- receive-response flow
- finalize-run flow

## Android runtime 最小接口规格

### 组件

#### 1. `DevPanelActivity`
职责：
- 显示当前 daemon 状态
- 用户点击启动 daemon
- 用户点击停止 daemon
- 显示当前 server address / runtime version / last heartbeat

#### 2. `DaemonForegroundService`
职责：
- 建立与 Mac daemon 的 WebSocket 连接
- 接收命令
- 将命令分发给 blocks/flows
- 保持 ongoing notification
- 管理 heartbeat / reconnect
- 输出本地日志

#### 3. `LocalLogStore`
职责：
- 追加本地日志
- 提供 tail(N) 能力
- 为 `fetch-logs` 返回最近日志

## Android 侧模块分层

### foundation
只放纯工具：
- version reader
- time helper
- json helper
- ws client adapter
- log formatter
- file helper

### blocks
单能力闭环：
- start-daemon block
- stop-daemon block
- connect-ws block
- reconnect-ws block
- handle-ping block
- handle-fetch-logs block
- append-log block
- read-log-tail block

### flows
流程编排：
- daemon-startup flow
- ws-session flow
- ping-response flow
- fetch-logs flow
- reconnect flow

## 命令处理规则

### 1. `ping`

#### 输入
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

#### Android 行为
1. 记录 `command_received`
2. 记录 `command_started`
3. 返回 `pong`
4. 记录 `command_finished`

#### 输出
- response envelope
- 本地日志追加 3~4 条事件
- Mac 端落盘 `manifest.json + response.json + logs.txt`

### 2. `fetch-logs`

#### 输入
```json
{
  "protocolVersion": "exp01",
  "requestId": "req_20260421_162000_0002",
  "runId": "2026-04-21T16-20-00_fetch-logs_tail-200",
  "command": "fetch-logs",
  "sentAt": "2026-04-21T16:20:00+08:00",
  "timeoutMs": 10000,
  "payload": {
    "tail": 200
  }
}
```

#### Android 行为
1. 记录 `command_received`
2. 读取最近 N 条日志
3. 返回日志内容或单独 artifact 引用
4. 记录 `command_finished`

#### 输出
- response envelope
- `logs.txt` 或 response 内嵌 logs payload
- Mac 端落盘 run 目录

## 连接与重连规则

Phase A 不做复杂网络层，只定最小规则：

1. daemon 启动后立即尝试连接 Mac daemon
2. 成功后发送 `client_hello`
3. 断线后退避重连
4. 重连间隔：例如 `1s -> 2s -> 5s -> 10s`，上限固定
5. 每次重连都写本地日志

### client_hello 最小字段

```json
{
  "type": "client_hello",
  "deviceId": "android-local-01",
  "runtimeVersion": "0.1.0001",
  "appId": "com.flowy.explore",
  "capabilities": ["ping", "fetch-logs"]
}
```

## 版本规则（Phase A 落地方式）

由于当前仍处于实验期，版本真源先放在**实验目录内部**，不要提前抽到正式 shared config。

推荐路径：

```text
explore/android-daemon-lab/config/runtime-version.json
```

最小格式：

```json
{
  "versionName": "0.1.0001",
  "majorMinor": "0.1",
  "buildNumber": 1
}
```

规则：
- 每次编译前自动 bump `buildNumber`
- `versionName = majorMinor + '.' + buildNumber.padStart(4, '0')`
- Phase A 只定义规则，不要求现在就做正式全仓版本系统

## Build / Verify 入口（Phase A）

仍然遵守全仓硬规则，但只给 Phase A 最小入口：

```text
scripts/dev/
  phase-a-start-mac-daemon.*
  phase-a-start-android-dev.*

scripts/verify/
  check-file-lines.*
  phase-a-regression.*
```

### `phase-a-regression` 最少包含
1. ping flow regression
2. fetch-logs flow regression
3. 版本格式校验：必须是 `0.1.0001` 这种 4 位 build number
4. 文件行数校验

## Phase A 成功判据

### Gate A1: startup gate
- 用户可从 DevPanel 启动 daemon
- daemon 有 ongoing notification
- daemon 状态可见

### Gate A2: transport gate
- Android 能连上 Mac daemon
- Mac 能识别 Android client_hello
- 断线后能重连

### Gate A3: ping gate
- `ping` 真实往返成功
- Mac 落盘 run artifacts
- Android 本地日志可见完整 lifecycle

### Gate A4: log gate
- `fetch-logs` 能真实取回日志
- 日志能覆盖 startup + command lifecycle
- Mac 能人工检查时间线

只有 Gate A1~A4 全过，才进入 Phase B。

## Phase B 入口（仅占位，不展开）

Phase A 通过后，Phase B 再增加：
- `AccessibilityService`
- `dump-accessibility-tree`
- `capture-screenshot`

### Phase B 的额外官方约束
- `AccessibilityService` 需要显式启用
- 如要使用 `takeScreenshot()`，需要声明 screenshot capability
- 如要获取多窗口/交互窗口，需要启用相应 accessibility flags

## 当前推荐决定

**推荐决定：**

- Experiment 01 / Phase A 用 **Foreground Service + WebSocket** 起步
- `ping` / `fetch-logs` 先跑通
- Accessibility 感知留到 Phase B
- 版本号先在实验目录本地管理
- 不提前做正式 shared scaffold
