# Experiment 01 / Phase A — 文件树与模块落点

## 目标

把 Phase A 的实现目录定清楚，保证后续开工时：

- 不会把实验代码和正式 APP 混写
- 不会把 Mac daemon 和 Android daemon 混写
- 不会把 foundation / blocks / flows 混写
- 不会把协议真源和运行时代码混写

本文件只定义 **实验阶段** 的文件树，不代表正式产品目录已经启用。

## Phase A 顶层落点

```text
flowy/
├─ docs/
│  └─ experiments/
│     ├─ experiment-01-daemon-min-loop.md
│     ├─ experiment-01-phase-a-implementation-spec.md
│     ├─ experiment-01-phase-a-file-layout.md
│     └─ exp01-json-truth-spec.md
├─ services/
│  └─ mac-daemon/
├─ explore/
│  └─ android-daemon-lab/
├─ scripts/
│  ├─ dev/
│  └─ verify/
└─ artifacts/
```

## 目录职责总览

### `services/mac-daemon/`
- 只放 Mac 本地 daemon 实验代码
- 负责 websocket 接入、命令下发、响应落盘、artifact 目录管理

### `explore/android-daemon-lab/`
- 只放 Android Phase A 实验代码
- 负责前台服务 runtime、websocket 出站连接、ping / fetch-logs、日志本地存储

### `docs/experiments/`
- 放当前实验真源规格
- 当前阶段的 exp01 JSON 真源也先定义在文档，不提前抽到正式 `packages/protocol/`

### `scripts/dev/`
- 放实验启动脚本入口

### `scripts/verify/`
- 放实验验证脚本入口

## Mac daemon 目标文件树（Phase A）

```text
services/
  mac-daemon/
    go.mod
    src/
      proto/
        types
      state/
        state
      foundation/
        json_codec
        ids
        time
        file_paths
      blocks/
        ws_accept
        send_command
        persist_response
        persist_manifest
        persist_logs_artifact
      flows/
        client_session_flow
        command_roundtrip_flow
        finalize_run_flow
      main
    tests/
      foundation/
      blocks/
      flows/
```

## Mac daemon 各文件职责


### `src/proto/`
- 放 exp01 协议结构体
- 只做数据结构，不放流程逻辑

### `src/state/`
- 放 daemon 运行态共享状态
- 如 client session、pending request、artifact root

### `src/foundation/`

#### `json_codec`
- command / response / hello 的 encode/decode
- 只做 JSON 结构处理

#### `ids`
- 生成 `requestId`
- 生成 `runId`
- 校验 id 格式

#### `time`
- RFC3339 时间格式
- duration 计算
- 按日期切 artifact 子目录

#### `file_paths`
- 生成 `artifacts/YYYY-MM-DD/<run-id>/...` 路径
- 不做 IO，纯路径拼装

### `src/blocks/`

#### `ws_accept`
- 接收 Android websocket 连接
- 维护连接映射

#### `send_command`
- 向指定 client 发送 exp01 command
- 不负责回包持久化

#### `persist_response`
- 将 response envelope 写为 `response.json`

#### `persist_manifest`
- 将 run manifest 写为 `manifest.json`

#### `persist_logs_artifact`
- 将日志 payload 写为 `logs.txt`

### `src/flows/`

#### `client_session_flow`
- 处理 client_hello
- 维护 client online/offline 生命周期

#### `command_roundtrip_flow`
- 发命令
- 等响应
- 计算成功/失败/超时

#### `finalize_run_flow`
- 汇总 response / logs
- 写 manifest
- 完成一次 run

### `src/main`
- Mac daemon 启动入口
- 只做装配，不做业务逻辑

## Android daemon-lab 目标文件树（Phase A）

```text
explore/
  android-daemon-lab/
    config/
      runtime-version.json
      dev-server.json
    app/
      src/
        main/
          AndroidManifest.xml
          java/.../
            foundation/
              VersionReader
              TimeHelper
              JsonCodec
              WsClientAdapter
              LogFormatter
              FileHelper
            blocks/
              StartDaemonBlock
              StopDaemonBlock
              ConnectWsBlock
              ReconnectWsBlock
              HandlePingBlock
              HandleFetchLogsBlock
              AppendLogBlock
              ReadLogTailBlock
            flows/
              DaemonStartupFlow
              WsSessionFlow
              PingResponseFlow
              FetchLogsFlow
              ReconnectFlow
            ui/
              DevPanelActivity
            runtime/
              DaemonForegroundService
              LocalLogStore
              DaemonNotification
        debug/
          java/.../
            debug/
              DebugOverrides
      src/
        test/
    gradle/
```

## Android 各目录职责

### `config/runtime-version.json`
- 实验期版本真源
- 编译前自动 bump
- 不提前迁入正式 shared packages

### `config/dev-server.json`
- 实验期 Mac daemon 地址真源
- 包含 host / port / ws path
- 只服务实验，不提前收敛到正式 server-profile

### `app/src/main/java/.../foundation/`
- 纯工具类
- 不引用 flow 级状态

### `app/src/main/java/.../blocks/`
- 单能力最小闭环
- 一次只做一个动作

### `app/src/main/java/.../flows/`
- 多个 block 串联
- 处理状态机与重连逻辑

### `app/src/main/java/.../ui/DevPanelActivity`
- 启动/停止 daemon
- 展示 server / version / daemon 状态 / last heartbeat

### `app/src/main/java/.../runtime/DaemonForegroundService`
- 前台服务 runtime
- websocket 连接管理
- 命令接收与分发
- ongoing notification

### `app/src/main/java/.../runtime/LocalLogStore`
- 本地日志追加
- 读取 tail(N)
- 为 `fetch-logs` 提供日志读取

### `app/src/debug/`
- 仅放开发覆盖项
- 不放核心业务逻辑

## Scripts 目标文件树（Phase A）

```text
scripts/
  dev/
    phase-a-start-mac-daemon.sh
    phase-a-build-android-lab.sh
    phase-a-install-android-lab.sh
    phase-a-run-loop.sh
  verify/
    check-file-lines.sh
    phase-a-regression.sh
    phase-a-verify-ping.sh
    phase-a-verify-fetch-logs.sh
```

## Scripts 职责

### `phase-a-start-mac-daemon`
- 启动 Mac daemon 实验服务
- 不做构建产物发布

### `phase-a-build-android-lab`
- bump 实验版本
- 编译 Android daemon-lab
- 自动调用 `phase-a-regression`

### `phase-a-install-android-lab`
- 安装 debug 构建到设备

### `phase-a-run-loop`
- 串联本地最小实验
- 启动 Mac daemon
- 编译安装 Android lab
- 提示人工打开 DevPanel 并观察

### `check-file-lines`
- 校验所有源码文件 <= 500 行

### `phase-a-regression`
- 运行 ping / fetch-logs 最小回归
- 校验版本格式
- 调用文件行数检查

### `phase-a-verify-ping`
- 单独验证 ping 闭环

### `phase-a-verify-fetch-logs`
- 单独验证 fetch-logs 闭环

## Artifact 目录

Phase A 仍使用统一 artifact 结构：

```text
artifacts/
  YYYY-MM-DD/
    <run-id>/
      manifest.json
      response.json
      logs.txt
```

若命令是 `fetch-logs`，`logs.txt` 是远端回收日志；
若命令是 `ping`，`logs.txt` 是对应 lifecycle 日志。

## 当前阶段禁止事项

在 Phase A 文件布局里，禁止提前加入：

- `packages/protocol/` 的正式 schema 文件
- `apps/android-agent/` 的正式 product module
- screenshot / accessibility / upgrade 相关实现目录
- 一个超大 `main` 文件包揽全部逻辑

## 文件数与拆分原则

优先让文件**少而清晰**，但必须遵守：
- 单文件 <= 500 行
- 单文件只承担一个清晰职责
- 避免把多个 blocks 或多个 flows 塞进一个文件

## 迁移原则

Phase A 成功后：
- `services/mac-daemon/` 的有效实验代码可迁入正式模块
- `explore/android-daemon-lab/` 中经验证的结构再选择性迁入 `apps/android-agent/`
- 协议真源再从实验文档迁入正式 `packages/protocol/`

在此之前，不做提前迁移。
