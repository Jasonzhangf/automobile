# Server Profile Spec v1

## 目标

把 Flowy 的控制面、升级面、协议版本统一到一份共享配置真源里。

这份真源必须同时服务于：

- 手机端 runtime
- 本地桌面 / Mac 调试端
- 远端 control daemon
- CLI 包装入口
- WebSocket 运行时连接

开发阶段默认：

- `controlMode = remote`

---

## 一、为什么必须先定 server-profile

现在已经确认了 4 条硬约束：

1. control daemon 支持多端驱动
2. 手机端 WebSocket 可连本地或远端控制端口
3. 手机端升级必须内建
4. CLI 与 WebSocket 必须共享同一套控制真源

因此：

- 地址不能分散写在平台代码里
- 升级地址不能另起一套真源
- CLI 和 WS 不能各自维护自己的 endpoint

必须统一进：

```text
packages/config/server-profile/
```

---

## 二、目录结构

建议固定为：

```text
packages/config/server-profile/
├─ README.md
├─ server-profile.example.json
├─ generated/
│  ├─ android/
│  ├─ desktop/
│  └─ remote/
└─ schemas/
```

当前阶段先至少落：

- `README.md`
- `server-profile.example.json`

---

## 三、配置职责

`server-profile` 负责定义：

### 1. control mode
- `on_device`
- `desktop`
- `remote`

### 2. control endpoints
- WebSocket 地址
- HTTP / API 基址
- CLI route/base

### 3. artifact / event endpoints
- artifact upload
- event collect
- command dispatch

### 4. upgrade endpoints
- runtime bundle update
- APK update
- version manifest

### 5. protocol version
- control protocol version
- artifact protocol version

---

## 四、最小字段模型

建议第一版结构：

```json
{
  "schemaVersion": "flowy-server-profile-v1",
  "profileId": "dev-remote-default",
  "profileName": "Remote Dev",
  "active": true,
  "controlMode": "remote",
  "protocol": {
    "control": "flowy-control-v1",
    "artifact": "exp01",
    "upgrade": "flowy-upgrade-v1"
  },
  "control": {
    "httpBaseUrl": "http://127.0.0.1:8787/flowy/control",
    "wsUrl": "ws://127.0.0.1:8787/flowy/control/ws",
    "cliRouteBase": "/flowy/control/cli",
    "commandRoute": "/flowy/control/command",
    "eventRoute": "/flowy/control/events"
  },
  "artifacts": {
    "uploadUrl": "http://127.0.0.1:8787/exp01/artifacts"
  },
  "upgrade": {
    "checkUrl": "http://127.0.0.1:8787/flowy/upgrade/check",
    "runtimeBundleUrl": "http://127.0.0.1:8787/flowy/upgrade/runtime",
    "apkManifestUrl": "http://127.0.0.1:8787/flowy/upgrade/apk"
  },
  "runtime": {
    "connectTimeoutMs": 8000,
    "requestTimeoutMs": 15000,
    "heartbeatIntervalMs": 10000
  }
}
```

---

## 五、字段定义

### `schemaVersion`

用途：

- 标记配置 schema 版本

规则：

- 必填
- 不和 control protocol version 混用

---

### `profileId`

用途：

- 唯一标识当前 profile

规则：

- 必填
- 用于日志、CLI、debug panel 展示

---

### `controlMode`

可选值：

- `on_device`
- `desktop`
- `remote`

规则：

- 必填
- Android / desktop / remote 都只读同一字段判断当前连接目标

---

### `protocol`

用途：

- 标记控制、artifact、upgrade 使用的协议版本

规则：

- 每个子字段独立
- 不允许平台侧写死

---

### `control.httpBaseUrl`

用途：

- CLI 包装入口与 HTTP 控制入口的共同基址

规则：

- CLI 通过它进行包装访问
- 不能单独再发明第二套 CLI 地址真源

---

### `control.wsUrl`

用途：

- 手机 runtime 的控制面长连接地址

规则：

- 手机端运行时直接使用
- 可以指向本地桌面或远端

---

### `control.cliRouteBase`

用途：

- 标记 CLI 包装所映射的路由前缀

规则：

- 这是控制面语义的一部分，不允许 CLI 自己另外约定

---

### `artifacts.uploadUrl`

用途：

- artifact 上传地址

规则：

- 与 control mode 同源
- 不允许在 Android / Mac 侧各写一份

---

### `upgrade.*`

用途：

- 手机端内建升级的唯一入口真源

规则：

- runtime update 与 APK update 必须都从这里派生

---

## 六、生成规则

平台代码不得直接维护多份配置。

必须采用：

```text
server-profile source
-> generate android config
-> generate desktop config
-> generate remote config
```

当前阶段还不急着做完整生成器，但结构必须先这么定。

---

## 七、校验规则

每次修改 `server-profile` 时至少校验：

1. `controlMode` 合法
2. `httpBaseUrl` / `wsUrl` / `upgrade` 字段齐全
3. CLI route 与 WS route 没有漂移
4. control / artifact / upgrade protocol version 存在

---

## 八、开发期默认 profile

当前开发默认 profile：

```text
profileId = dev-remote-default
controlMode = remote
```

但后续必须允许切换到：

- `desktop`
- `on_device`

且平台代码不能重写连接逻辑。

---

## 九、当前接受的设计结论

1. `server-profile` 是 control / CLI / WS / upgrade 的统一真源。
2. 开发阶段默认先走 `remote`。
3. CLI 与 WS 共用同一控制面配置，不允许各自维护 endpoint。
4. 手机端内建升级与 control daemon 通信链路共用同一套配置真源。
