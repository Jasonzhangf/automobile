# Experiment 01 / Phase B — Screenshot + Accessibility 最小实现规格

## 目标

Phase B 在 Phase A 已经验证通过的基础上，只新增两件事情：

1. `capture-screenshot`
2. `dump-accessibility-tree`

它要证明的不是“完整页面理解已经完成”，而是先证明：

```text
Android can capture visible screen
+ Android can dump current Accessibility tree
+ Mac can persist both artifacts under run id
+ Human can inspect the artifacts on Mac
```

如果这一层还不稳定，就不应该继续做 page model、命令生成、复杂自动化流程。

## Phase B 的工程结论

### 1. 主 runtime 仍然是 Foreground Service

Phase A 已经证明：
- 前台服务可见启动
- WebSocket transport 稳定
- ping / fetch-logs 真机闭环成立

所以 Phase B 不推翻这个选择：
- **命令编排主 runtime 仍是 `DaemonForegroundService`**
- screenshot / accessibility 只是作为新的感知能力挂进现有 runtime

### 2. Accessibility 感知要引入独立 `AccessibilityService`

原因：
- `AccessibilityNodeInfo` 的主入口天然属于 `AccessibilityService`
- 前台服务适合管 transport / lifecycle，不适合直接承担 Accessibility 事件源

因此 Phase B 推荐形态变为：

```text
DevPanelActivity
  -> 用户启动 DaemonForegroundService
  -> 用户显式启用 FlowyAccessibilityService
  -> DaemonForegroundService 通过共享 store / flow 获取最近 Accessibility root
  -> 收到 dump-accessibility-tree 命令后执行导出
```

这里的意思不是把主 runtime 换成 AccessibilityService，而是：
- **Foreground Service = command runtime**
- **AccessibilityService = sensor runtime**

### 3. Screenshot 采用 MediaProjection

Phase B 的 screenshot 路线优先选择：
- **MediaProjection**

原因：
- 它符合“可见、可交互、用户显式授权”的产品边界
- 它更接近后续真实产品能力
- 它不依赖 adb shell 侧通道，不会把实验做偏

因此 Phase B 的截图前提是：
- 用户在 `DevPanelActivity` 中显式点击“授权截图”
- App 获得本次运行期内可用的 MediaProjection session

### 4. 传输形态从“纯 WS”升级为“WS + HTTP artifact upload”

Phase A 只有小 JSON，可以全走 WebSocket。

Phase B 开始出现：
- PNG screenshot
- 较大的 accessibility raw JSON

因此推荐：
- **WebSocket**：命令、响应、小日志、状态
- **HTTP upload**：大 artifact 文件上传

最小流转：

```text
Mac -> WS command
Android -> local capture
Android -> HTTP upload artifact(s)
Android -> WS response with artifact descriptors
Mac -> persist manifest + response + files
```

## Phase B 范围

### 要实现
- `capture-screenshot`
- `dump-accessibility-tree`
- `page-context.json` 统一页面上下文产物
- Mac 侧 artifact 按 run id 落盘
- Android 侧本地日志补齐 capture / upload 事件

### 不实现
- 页面归一化 page model
- OCR
- overlay 悬浮球正式版
- 手势执行
- capture-page 组合命令
- partial runtime upgrade
- APK in-app update
- 正式 app scaffold

## 运行前提

### 1. screenshot 前提
必须满足：
- `DaemonForegroundService` 已启动
- MediaProjection 权限已由用户明确授予
- 当前 session 仍有效

否则：
- 返回 `SCREENSHOT_PERMISSION_NOT_READY`
- 不允许静默失败

### 2. accessibility 前提
必须满足：
- `FlowyAccessibilityService` 已启用
- 当前存在 active window root 或最近一次可用 root snapshot

否则：
- 返回 `ACCESSIBILITY_SERVICE_DISABLED` 或 `ACCESSIBILITY_ROOT_UNAVAILABLE`
- 不允许回空成功

## Mac daemon 最小新增接口

### 1. artifact upload endpoint

推荐新增：

```text
POST /exp01/artifacts
```

建议使用 `multipart/form-data`，至少包含两部分：

1. `meta`
   - JSON
   - 包含 `runId / requestId / deviceId / kind / fileName / contentType`
2. `file`
   - 实际 artifact 内容
   - 可为 PNG 或 JSON 文本

### 2. 上传成功后的最小职责
Mac daemon 必须：
- 校验 `runId / requestId / kind`
- 将文件写入 `artifacts/YYYY-MM-DD/<run-id>/`
- 返回落盘确认
- 失败时返回明确错误

## Android 侧最小流程

### Flow A: capture-screenshot

```text
receive command
-> validate MediaProjection session
-> capture screenshot bitmap
-> encode png
-> build screenshot-meta.json
-> build page-context.json
-> upload screenshot.png
-> upload screenshot-meta.json
-> upload page-context.json
-> log receive/start/upload/finish
-> return response envelope
```

### Flow B: dump-accessibility-tree

```text
receive command
-> validate AccessibilityService state
-> fetch active/root snapshot
-> serialize accessibility-raw.json
-> build page-context.json
-> upload accessibility-raw.json
-> upload page-context.json
-> log receive/start/upload/finish
-> return response envelope
```

## 最小 artifact 约束

### capture-screenshot run 必须包含
- `manifest.json`
- `response.json`
- `logs.txt`
- `page-context.json`
- `screenshot.png`
- `screenshot-meta.json`

### dump-accessibility-tree run 必须包含
- `manifest.json`
- `response.json`
- `logs.txt`
- `page-context.json`
- `accessibility-raw.json`

## page-context 的定位

Phase B 不做最终 page model，但必须先有一个足够稳定的页面上下文文件，给后续 screenshot/tree 对齐做锚点。

它的用途：
- 标记当前 package / window / screen 信息
- 标记本次 capture 是 screenshot 还是 accessibility
- 让后续 `capture-page` 组合命令可以直接复用

**注意**：
- `page-context.json` 只是 Phase B 的对齐锚点
- 它不是最终 page model

## 日志事件要求

Phase B 至少新增这些稳定事件名：
- `screenshot_capture_started`
- `screenshot_capture_finished`
- `screenshot_capture_failed`
- `accessibility_dump_started`
- `accessibility_dump_finished`
- `accessibility_dump_failed`
- `artifact_upload_started`
- `artifact_upload_finished`
- `artifact_upload_failed`

## Gate 定义

### Gate 1: screenshot permission gate
真机上必须能明确看到：
- 用户通过可见 UI 授权 MediaProjection
- daemon 侧状态显示 screenshot ready

### Gate 2: screenshot artifact gate
Mac 侧必须出现一个成功 run：
- `response.json` 为 `status=ok`
- `screenshot.png` 可人工打开
- `screenshot-meta.json` 与图片尺寸一致
- `page-context.json` 存在
- `logs.txt` 中存在 capture/upload 事件链

### Gate 3: accessibility artifact gate
Mac 侧必须出现一个成功 run：
- `response.json` 为 `status=ok`
- `accessibility-raw.json` 存在
- 至少包含 `rootNodeId` 与非空 `nodes`
- `page-context.json` 存在
- `logs.txt` 中存在 dump/upload 事件链

### Gate 4: inspectability gate
人工必须能在 Mac 上直接回答：
- 这张 screenshot 属于哪个 app/package
- 这份 accessibility raw 来自哪个 package
- 当前 screen size / rotation 是什么
- 当前 run 是否成功以及失败点在哪一段日志

不能回答这些问题，就说明 artifact 设计还不够好。

## 失败处理规则

### screenshot 失败时
必须返回结构化错误，例如：
- `SCREENSHOT_PERMISSION_NOT_READY`
- `SCREENSHOT_CAPTURE_FAILED`
- `ARTIFACT_UPLOAD_FAILED`

### accessibility 失败时
必须返回结构化错误，例如：
- `ACCESSIBILITY_SERVICE_DISABLED`
- `ACCESSIBILITY_ROOT_UNAVAILABLE`
- `ACCESSIBILITY_SERIALIZE_FAILED`
- `ARTIFACT_UPLOAD_FAILED`

## 与 Phase A 的关系

Phase B 不替换 Phase A，而是在其上加两条能力线：
- 保留现有 `ping`
- 保留现有 `fetch-logs`
- 新增 screenshot / accessibility 两条感知链路

因此到 Phase B 完成时，Experiment 01 的可用命令集变为：
- `ping`
- `fetch-logs`
- `capture-screenshot`
- `dump-accessibility-tree`

## 下一阶段入口

只有当 Phase B 的 4 个 gate 拿到真机证据后，才允许进入下一层：
- screenshot + accessibility 对齐
- 归一化 page-state
- command candidate 生成
- 操作自动化流程
