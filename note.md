# note.md

> 说明：本文件记录探索过程，不是真源事实；稳定结论才提升到 `AGENTS.md`。

## 2026-04-21

### 本轮已确认方向
- 产品方向：Android 上可见、可交互、可中断的自动化 Agent hand。
- 当前优先级：先做感知链路，不先做复杂操作。
- 主感知框架：AccessibilityService / AccessibilityNodeInfo。
- 补充感知：截图，后续再接 OCR / 视觉理解。
- 开发方式：Mac 本地 daemon + 手机端 Agent，把手机上的识别结果和截图拉回到 Mac 侧调试。

### 本轮新增决策
1. 工作区采用 **双 daemon + shared packages** 的 monorepo 结构。
2. `services/mac-daemon/` 与 `apps/android-agent/` 分开建模块，禁止混写。
3. `packages/` 负责协议、页面模型、服务地址、回归样本的唯一真源。
4. 升级分两段：
   - partial runtime upgrade：通过文件传输，不动 APK
   - APK in-app update：需要安装包更新
5. Android 必须支持内建开发模式，能够直接访问同一套 server 地址进行检测与升级。
6. 每个源码文件 <= 500 行，任何 build 都必须自动跑回归测试。

### 当前核心假设
1. 手机端可以把一次页面捕获整理成统一 run bundle，并传给 Mac daemon。
2. 只要 run bundle 包含截图、包名/Activity、raw Accessibility dump、归一化 page-state、日志，就足够支持第一阶段调试。
3. `adb reverse` / `adb forward` 应该是第一优先的本地调试传输方案；如果不顺，再切 LAN WebSocket / HTTP。
4. server 地址、升级地址、协议版本可以从单一 `server-profile` 真源生成给两个 daemon 使用。

### 当前待验证项
1. Mac daemon 最小技术栈选什么最顺手，同时便于本地调试与回放。
2. Android debug build 如何最稳地内建 dev panel、server profile、runtime sync。
3. partial runtime upgrade 的最小 manifest 长什么样，怎样做 checksum/rollback。
4. 回归测试样本应该先覆盖哪些页面和协议场景。

### 已收敛的目录原则
- 详细结构见：`docs/architecture/workspace-layout.md`
- 脚本统一放 `scripts/dev`、`scripts/verify`、`scripts/release`
- 详细架构不要堆进 `AGENTS.md`

### 建议的下一步实现顺序
1. 先做 `services/mac-daemon/` 的 Phase A 最小 websocket 接收与 run 落盘。
2. 再做 `explore/android-daemon-lab/` 的 Phase A 最小 runtime（Foreground Service + logs）。
3. 先打通 `ping`，再打通 `fetch-logs`。
4. Phase A 全部 gate 通过后，再进入 screenshot / accessibility / 正式协议抽象。

### 本轮新增硬规则
- 先做实验闭环，再做基础脚手架。
- 先做实验闭环，再做基础功能。
- 当前目录结构以保留路径和设计目标为主，不代表正式模块已经开工。

### 本轮新增规格
- Android 最小实验对象是 long-running daemon runtime。
- Android 最小闭环：接收命令 -> 执行 -> 反馈 -> 本地日志。
- 探索目录与正式 APP 目录分离。
- 代码三层：foundation / blocks / flows。
- 版本从 `0.1.0001` 起，每次编译自动 bump 四位 build number。

### Experiment 01 已定义
- 文档：`docs/experiments/experiment-01-daemon-min-loop.md`
- 目标：验证 Android daemon 与 Mac daemon 的最小闭环。
- 最小命令：`ping`、`capture-screenshot`、`dump-accessibility-tree`、`fetch-logs`。
- Gate：ping / screenshot / accessibility / log retrieval 四个 gate 全过才算实验完成。

### Experiment 01 / Phase A 已定义
- 文档：`docs/experiments/experiment-01-phase-a-implementation-spec.md`
- Android 运行形态：`DevPanelActivity + DaemonForegroundService + LocalLogStore`
- 只做：`ping` + `fetch-logs`
- 通道：Android outbound WebSocket -> Mac daemon
- Gate：startup / transport / ping / log 四个 gate 全过再进入 Phase B。

### Phase A 文件树与 JSON 真源已定义
- 文档：`docs/experiments/experiment-01-phase-a-file-layout.md`
- 文档：`docs/experiments/exp01-json-truth-spec.md`
- 作用：定死 Phase A 的 Mac / Android 文件树与 exp01 JSON 结构。
- 约束：当前 JSON 真源仍放实验文档，不提前迁到 `packages/protocol/`。

### Mac daemon Phase A 最小骨架已实现
- 语言：Go（本机 Rust 默认 toolchain 当时未就绪，为保持实验推进先用 Go 落地）。
- 已实现：websocket 接入、client_hello 注册、命令下发、response/manifest/logs 落盘。
- 已补脚本：`scripts/verify/check-file-lines.sh`、`scripts/verify/phase-a-regression.sh`、`scripts/dev/phase-a-start-mac-daemon.sh`。
- 已验证：`go test ./...`、`go build`、`/health` smoke 成功。

### Android daemon-lab Phase A 最小骨架已实现
- 目录：`explore/android-daemon-lab/`
- 已实现：`DevPanelActivity`、`DaemonForegroundService`、`LocalLogStore`、WS client、ping/fetch-logs 路由骨架。
- 已实现构建门禁：编译前自动 bump `config/runtime-version.json`；构建自动跑 `testDebugUnitTest`；行数检查进入构建。
- 已解决构建问题：使用本机缓存 Gradle 8.3；使用本机 SDK `aapt2` override，绕过远端 jar 下载中断。
- 已验证：`scripts/dev/phase-a-build-android-lab.sh` 成功，`app-debug.apk` 已产出；`scripts/verify/phase-a-verify-android-lab.sh` 成功。
- 当前未验证：设备安装与真实运行（当前 `adb devices` 为空）。

### Phase A 真机闭环脚本已补齐
- 已补脚本：`scripts/dev/phase-a-adb-reverse.sh`、`phase-a-list-clients.sh`、`phase-a-send-command.sh`、`phase-a-send-ping.sh`、`phase-a-send-fetch-logs.sh`、`phase-a-real-device-loop.sh`。
- 已验证：无设备场景下，`phase-a-list-clients.sh` 返回 `[]`，`phase-a-send-ping.sh` 会明确报 `no connected exp01 client`。
- 当前缺口：还没有连上真实 Android 设备，因此 `client_hello` / `ping` / `fetch-logs` 真闭环仍未验证。

## 2026-04-22

### Phase A 真机闭环已打通
- 设备：`PLZ110`
- adb 连接：`100.127.23.27:36711`
- Mac daemon 健康检查：`GET /health -> {"status":"ok"}`
- Android lab 当前版本：`0.1.0006`

### 今日先发现的真机阻塞点
- 现象：前台服务可以启动，但 Mac 侧一直没有 `client_hello`。
- 真机日志结论：`CLEARTEXT communication to 127.0.0.1 not permitted by network security policy`
- 根因：Android 默认 cleartext policy 拦截了 `ws://127.0.0.1:8787/exp01/ws`
- 修复：
  - `AndroidManifest.xml` 增加：
    - `android:networkSecurityConfig="@xml/network_security_config"`
    - `android:usesCleartextTraffic="true"`
  - 新增：`app/src/main/res/xml/network_security_config.xml`
  - 放行：`127.0.0.1`、`localhost`

### 真机成功证据
- `adb reverse tcp:8787 tcp:8787` 成功。
- 打开 `DevPanelActivity` 后，通过真实 UI 点击 `START DAEMON` 成功启动前台服务。
- `GET /exp01/clients` 返回：
  - `deviceId: OP64DDL1`
  - `runtimeVersion: 0.1.0006`
  - `capabilities: ["ping", "fetch-logs"]`
- `ping` 真机回包成功：
  - artifact：`artifacts/2026-04-22/2026-04-22T09-50-25_ping_boot-check/response.json`
  - 结果：`status=ok`，`message=pong`
- `fetch-logs` 真机回包成功：
  - artifact：`artifacts/2026-04-22/2026-04-22T09-50-25_fetch-logs_tail-120/`
  - 文件：
    - `manifest.json`
    - `response.json`
    - `logs.txt`
  - 结果：`status=ok`，`inlineLogs` 返回 33 条，能同时看到：
    - 历史 cleartext 失败日志
    - 修复后的 `ws_connect_succeeded`
    - 本次 `command_received` / `command_started` / `command_finished`

### 今日新增脚本兼容性修复
- 真机联调时发现 `scripts/dev/phase-a-send-command.sh` 在 macOS 自带 Bash 3.2 下不兼容：
  1. `${var@Q}` 不支持，导致脚本直接报 `bad substitution`
  2. `payload_json=\"${3:-{}}\"` 会被 Bash 3.2 解析出多余 `}`，导致 Python `json.loads` 报 `Extra data`
- 已修复：
  - 改为通过环境变量把参数传给内嵌 Python
  - 改为先读 `${3-}`，为空时再显式设置 `payload_json='{}'`
- 已验证：
  - `bash -n scripts/dev/phase-a-send-command.sh`
  - `./scripts/verify/check-file-lines.sh`
  - `phase-a-send-ping.sh` / `phase-a-send-fetch-logs.sh` 真机成功

### 当前判断
- Experiment 01 / Phase A 的四个 gate 已拿到真机证据：
  1. startup
  2. transport
  3. ping
  4. log
- 因此可以开始进入下一阶段：`screenshot` / `accessibility tree` 的最小实验设计与实现。

### 下一步建议
1. 把 Phase B 的最小 gate 明确成：
   - `capture-screenshot`
   - `dump-accessibility-tree`
   - Mac 侧 artifact 同 runId 对齐落盘
2. 先补一个统一 run bundle 目录结构，避免 screenshot/tree 两条线分别散落。
3. 再决定是否把实验 JSON 真源从文档提升到 `packages/`。

### Phase B 规格已冻结（未实现）
- 新增文档：
  - `docs/experiments/experiment-01-phase-b-implementation-spec.md`
  - `docs/experiments/experiment-01-phase-b-file-layout.md`
- 更新文档：
  - `docs/experiments/exp01-json-truth-spec.md`
- 当前收敛结论：
  1. 主 runtime 仍然是 `DaemonForegroundService`
  2. Accessibility 感知通过独立 `FlowyAccessibilityService`
  3. Screenshot 通过 `MediaProjection`
  4. 传输形态从 `WS only` 升级为 `WS + HTTP artifact upload`
  5. `page-context.json` 作为 screenshot/tree 的最小对齐锚点独立落盘
- 当前 Phase B run 目录要求：
  - screenshot 成功 run：
    - `manifest.json`
    - `response.json`
    - `logs.txt`
    - `page-context.json`
    - `screenshot.png`
    - `screenshot-meta.json`
  - accessibility 成功 run：
    - `manifest.json`
    - `response.json`
    - `logs.txt`
    - `page-context.json`
    - `accessibility-raw.json`
- 当前未验证项：
  - MediaProjection 真机授权与后台 session 持有
  - AccessibilityService snapshot 获取时机
  - 大 artifact 上传接口的 Go 端落盘实现

### Phase B 第一刀实现：Mac artifact upload 底座已完成
- 已新增 Mac daemon 路由：
  - `POST /exp01/artifacts`
- 当前实现能力：
  1. 接收 `multipart/form-data`
  2. 读取 `meta` + `file`
  3. 校验 `protocolVersion/requestId/runId/deviceId/kind/fileName/contentType`
  4. 按 `runId` 落盘到：
     - `artifacts/YYYY-MM-DD/<run-id>/<fileName>`
  5. 返回 `artifact_descriptor`（含 `sha256`、`sizeBytes`）
- 同步补齐：
  - `FinalizeRun` 现在会把 `response.artifacts[].fileName` 写入 `manifest.json`
  - run 目录按 `runId` 日期前缀归档，避免后续上传与 response 落到不同目录

### 本轮新增测试与验证
- 新增测试：
  - `services/mac-daemon/src/flows/artifact_upload_flow_test.go`
  - `services/mac-daemon/src/flows/finalize_run_flow_test.go`
- 已验证：
  - `./scripts/verify/check-file-lines.sh`
  - `cd services/mac-daemon && go test ./...`
  - `cd services/mac-daemon && go build -o ./.tmp/flowy-mac-daemon ./src`
  - 本地 smoke：
    - `POST /exp01/artifacts` 成功
    - `page-context.json` 已写入 `/tmp/flowy-phase-b-smoke/2026-04-22/2026-04-22T10-09-00_capture-screenshot_smoke/`

### 当前下一步
1. Android 侧补 `MediaProjection` 授权入口与 session holder
2. 接 `capture-screenshot` command flow
3. 用真机把第一个 `screenshot.png + page-context.json + response.json` run 打出来

### Phase B / screenshot 真机后台闭环已打通
- 目标：不是前台 Activity 自截图，而是：
  - `DevPanelActivity` 只负责用户显式授权
  - `DaemonForegroundService` 在后台常驻
  - 切到别的 APP 后，由 service 响应 Mac 命令完成截图并回传
- 真机过程：
  1. 在 `Flowy Daemon Lab` 内授权 `MediaProjection`
  2. 录屏范围切为 **整个屏幕**（不是单个应用）
  3. 启动 `DaemonForegroundService`
  4. 切到系统设置页 `com.android.settings`
  5. Mac 下发 `capture-screenshot`
  6. Android 后台截图并通过 `POST /exp01/artifacts` 上传
  7. Mac 落盘 screenshot artifacts

### 本轮遇到并解决的 3 个 Android 真约束
1. **FGS type 必须包含 `mediaProjection`**
   - 失败证据：`Media projections require a foreground service of type ... MEDIA_PROJECTION`
   - 修复：
     - manifest 增加 `FOREGROUND_SERVICE_MEDIA_PROJECTION`
     - service type 改为 `dataSync|mediaProjection`
     - `startForeground()` type 位图加上 `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`
2. **后台 service 不能用 `context.display`**
   - 失败证据：`Tried to obtain display from a Context not associated with one`
   - 修复：改为 `DisplayManager.getDisplay(Display.DEFAULT_DISPLAY)` 读取默认屏
3. **MediaProjection 开始 capture 前必须注册 callback**
   - 失败证据：`Must register a callback before starting capture`
   - 修复：capture 前 `registerCallback()`，结束时 `unregisterCallback()`

### 后台 screenshot 成功证据
- client 能力声明已包含：
  - `capture-screenshot`
- 真机成功 run：
  - `artifacts/2026-04-22/2026-04-22T10-30-44_capture-screenshot_settings-screen/`
- 成功文件：
  - `response.json`
  - `manifest.json`
  - `screenshot.png`
  - `screenshot-meta.json`
  - `page-context.json`
- `response.json` 结果：
  - `status = ok`
  - 3 个 artifact descriptor 均有 `sha256` 与 `sizeBytes`
- 人工查看 `screenshot.png`：
  - 内容确认为系统设置里的“无线调试”页面
  - 说明截图不是停留在 Flowy 自己界面，而是后台截到了别的 APP

### 当前剩余缺口
- `page-context.json.app.packageName` 仍是 `null`
  - 说明 screenshot 链路已经通，但“当前前台 app/package”还没有并入 page context
  - 这正是后续 AccessibilityService / page-context 对齐要补的内容
- `manifest.json.app.packageName` 仍是 `com.flowy.explore`
  - 当前代表 runtime app，不代表被截取的前台目标 app

### 当前判断
- 你要求的这条链路已拿到真机证据：
  - **安卓端后台常驻**
  - **切到别的 APP 后截图**
  - **回传到 Mac 并落盘**
- 所以下一步应该进入：
  1. `dump-accessibility-tree`
  2. page-context 中补前台 package / window 信息
  3. screenshot + tree 同 runId 对齐

### Accessibility 链路实现已落地，当前卡在系统开关未启用
- 已实现：
  - `FlowyAccessibilityService`
  - `AccessibilitySnapshotStore`
  - `AccessibilityTreeSerializer`
  - `dump-accessibility-tree` command flow
  - `page-context.json` 现在可复用 accessibility snapshot 的 `packageName/windowTitle`
  - DevPanel 已新增：
    - `Accessibility: enabled|disabled`
    - `Open Accessibility Settings`
- 已验证：
  - Android build / verify 通过
  - manifest 中 Accessibility service 已注册
- 当前真机阻塞证据：
  - `adb shell settings get secure enabled_accessibility_services` 返回空
  - `adb shell settings get secure accessibility_enabled` 返回 `0`
  - 说明系统辅助功能里的 Flowy Accessibility 还未开启
- 结论：
  - 代码已到可验证状态
  - 真机 `dump-accessibility-tree` 还差用户把 Accessibility 服务启用

### 新设备 `100.120.173.56:46389` 上的 crash 根因已定位并修复
- 设备：
  - serial: `100.120.173.56:46389`
  - deviceId: `OP5DC1L1`
- 新发现的 2 个真约束：
  1. **AccessibilityService 不能在内容变化风暴里同步全量序列化**
     - 现象：旧实现监听 `TYPE_WINDOW_CONTENT_CHANGED`，在主线程频繁递归序列化节点树，曾触发 Activity / service ANR。
     - 当前修正：
       - service 事件收敛为 `TYPE_WINDOW_STATE_CHANGED | TYPE_WINDOWS_CHANGED`
       - 增加 debounce + 最小 capture 间隔

## 2026-04-26

### `0.1.0063` 真机闭环：后台截图修复完成
- 设备：`100.127.23.27:1234`
- 设备 ID：`OP64DDL1`
- 最终安装版本：`0.1.0063`

### 本轮根因定位过程
1. 旧问题不是单一问题，实际上连续踩了 3 类约束：
   - `android:project_media` / `FOREGROUND_SERVICE_MEDIA_PROJECTION` 时序约束
   - `Must register a callback before starting capture`
   - `Don't re-use the resultData ... / don't createVirtualDisplay multiple times`
2. 原 `MediaProjectionSessionHolder.store()` 静默吞异常，导致面板只显示 `Projection: not-ready`，根因不可见。
3. 加上 logcat 后拿到真实异常，确认第二个阻塞点是：
   - `java.lang.IllegalStateException: Must register a callback before starting capture`
4. 修正后再验证，确认当前稳定做法是：
   - 用户授权
   - 前台服务提升到 `mediaProjection`
   - `registerCallback()`
   - 创建并保留单个 `MediaProjection + VirtualDisplay + ImageReader`
   - 后续截图复用同一会话，不再重复 `getMediaProjection()` / `createVirtualDisplay()`

### 本轮代码调整
- `MediaProjectionSessionHolder`
  - 改成 **grant pending + activate** 两段式
  - 补 `lastError` / logcat 输出，去掉静默吞异常
  - 激活顺序调整为先 `registerCallback()` 再 `createVirtualDisplay()`
- `DaemonForegroundService`
  - `foregroundServiceType()` 以 `hasGrant()` 而不是 `isReady()` 判定是否带 `mediaProjection`
  - `promoteProjectionIfReady()` 在刷新前台服务后主动触发 `activate()`

### 真机成功证据
1. 授权成功后状态：
   - run: `artifacts/2026-04-26/2026-04-26T10-24-43_dump-accessibility-tree_v63-after-grant/`
   - DevPanel 状态文本：`Projection: ready @ 2026-04-26T10:24:30.999236+08:00`
   - logcat 关键证据：`FlowyProjection: projection session activated`
2. 前台截图成功：
   - run: `artifacts/2026-04-26/2026-04-26T10-25-04_capture-screenshot_v63-foreground-after-grant/`
   - `response.json.status = ok`
   - 人工查看 `screenshot.png`，内容为 Flowy DevPanel
3. Home 后后台截图成功：
   - run: `artifacts/2026-04-26/2026-04-26T10-25-19_capture-screenshot_v63-background-after-home/`
   - `response.json.status = ok`
   - `page-context.json.app.packageName = com.android.launcher`
   - 人工查看 `screenshot.png`，内容为 launcher 首页，不是 Flowy 自身页面

### 当前结论
- Flowy 自身截图链路已重新拿到真机证据：
  - 前台截图成功
  - Home 后后台截图成功
  - screenshot artifact 正常回传到 Mac
  - page-context 能对齐前台包名（如 `com.android.launcher`）

### 下一步建议
1. 停止再用 adb 参与业务流程控制，只保留安装/启动/日志类用途。
2. 开始进入 **小红书流程探索**：
   - 打开小红书
   - 搜索 `deepseek v4`
   - 记录页面流转、可提取锚点、可执行操作、采集字段
3. 把这套流程抽成 app-collection workflow 的第一个真实 profile。

### 基础能力已抽成标准框架模块（`0.1.0066`）
- 本轮目标：不再继续散着做实验块，而是把已验证能力收口成标准骨架模块。
- 已新增基础骨架块：
  - `ObservePageBlock`
  - `FilterTargetsBlock`
  - `EvaluateAnchorBlock`
  - `ExecuteOperationBlock`
  - `EmitEventBlock`
- 已新增基础真值/工具：
  - `BlockResultFactory`
  - `ObservedPageState`
  - `PageSignatureBuilder`
- 已完成接线：
  - `ScreenshotCaptureFlow` 改走 `ObservePageBlock`
  - `AccessibilityDumpFlow` 改走 `ObservePageBlock`
  - `OperationRunFlow` 改走 `ExecuteOperationBlock`

### 本轮验证证据
- 构建门禁：
  - `./scripts/dev/build-android-lab.sh`
  - 结果：通过，版本 bump 到 `0.1.0066`
- 真机安装：
  - `versionName=0.1.0066`
- 真机 smoke：
  - `dump-accessibility-tree` 成功：
    - `artifacts/2026-04-26/2026-04-26T11-07-47_dump-accessibility-tree_framework-smoke/`
  - 重新授权后 `capture-screenshot` 成功：
    - `artifacts/2026-04-26/2026-04-26T11-08-56_capture-screenshot_framework-after-grant/`

### 当前判断
- 基础能力已经从“散装命令”进入“可复用框架模块”阶段。
- 下一步可以不再补底层操作，直接开始：
  - 业务页面流转
  - filter 设计
  - anchor 设计
  - app profile / field mapping
     - 结果：新设备上不再复现之前的 ANR crash。
  2. **daemon 启动时不能无条件以 `mediaProjection` FGS type 起前台服务**
     - 失败证据：
       - `java.lang.SecurityException: Starting FGS with type mediaProjection ... requires ... [android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION] anyOf [android.permission.CAPTURE_VIDEO_OUTPUT, android:project_media]`
     - 根因：
       - 在还没拿到当前 projection session 前，就用 `DATA_SYNC | MEDIA_PROJECTION` 调 `startForeground()`
     - 当前修正：
       - daemon 初始只按 `DATA_SYNC` 起
       - 只有 `MediaProjectionSessionHolder.isReady()` 时，才升级前台 service type，截图前也会再 promote 一次

### Accessibility status 真源修正
- 旧现象：
  - 系统 `dumpsys accessibility` 已显示 Flowy service enabled/bound
  - 但 DevPanel 仍显示 `Accessibility: disabled`
- 修正：
  - `AccessibilityStatusReader` 先读 `AccessibilityManager.getEnabledAccessibilityServiceList()`
  - `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` 作为 fallback
  - 同时兼容 `flattenToString()` / `flattenToShortString()`
- 新证据：
  - 当前 DevPanel UI dump 已显示：
    - `Accessibility: enabled`

### 新设备上的 daemon / accessibility / screenshot 真机回归已通过
- daemon 启动成功证据：
  - `GET /exp01/clients` 返回：
    - `deviceId: OP5DC1L1`
    - `runtimeVersion: 0.1.0020`
    - `capabilities: ["ping","fetch-logs","capture-screenshot","dump-accessibility-tree"]`
- accessibility dump 成功 run：
  - `artifacts/2026-04-22/2026-04-22T12-36-31_dump-accessibility-tree_settings-tree/`
  - 文件：
    - `accessibility-raw.json`
    - `page-context.json`
    - `response.json`
    - `manifest.json`
  - `page-context.json` 证据：
    - `app.packageName = "com.android.settings"`
    - `runtime.accessibilityReady = true`
- screenshot 成功 run：
  - `artifacts/2026-04-22/2026-04-22T12-39-16_capture-screenshot_settings-screen/`
  - 文件：
    - `screenshot.png`
    - `screenshot-meta.json`
    - `page-context.json`
    - `response.json`
    - `manifest.json`
  - `page-context.json` 证据：
    - `app.packageName = "com.android.settings"`
    - `runtime.projectionReady = true`
    - `runtime.accessibilityReady = true`
  - 人工查看 `screenshot.png`：
    - 内容为系统设置“无线调试”页面
    - 说明 daemon 在切到别的 APP 后仍能后台截图并回传

### 当前新的判断
- 你要的这条更完整链路，现在在新设备上已经有证据：
  1. Android daemon 可后台常驻
  2. Accessibility 可识别前台页面并回传控件树
  3. 拿到 projection 授权后，可在后台截图并回传
  4. `page-context.json` 已能对齐到前台目标 app
- 下一步更值得做的是：
  1. 把 `page-context/windowTitle` 再补稳定
  2. 开始定义 raw tree -> stable page model 的归一化
  3. 再进入“页面命令化 / 流程编排”

### Phase C / D 当前收敛
- 文档：
  - `docs/experiments/experiment-01-phase-c-page-state-min-spec.md`
  - `docs/experiments/experiment-01-phase-d-command-candidate-min-spec.md`
- 脚本：
  - `scripts/dev/phase-c-extract-page-state.py`
  - `scripts/dev/phase-d-extract-command-candidates.py`
  - `scripts/verify/phase-c-page-state-smoke.sh`
  - `scripts/verify/phase-d-command-candidate-smoke.sh`
- 已验证：
  - `python3 -m py_compile scripts/dev/phase-c-extract-page-state.py`
  - `python3 -m py_compile scripts/dev/phase-d-extract-command-candidates.py`
  - `./scripts/verify/check-file-lines.sh`
  - `./scripts/verify/phase-c-page-state-smoke.sh`
  - `./scripts/verify/phase-d-command-candidate-smoke.sh`
- settings smoke：
  - `title = "无线调试"`
  - `signature = "b40efef1dff7"`
  - `elementCount = 12`
  - `commandCount = 14`
- 当前判断：`accessibility-raw -> page-state -> command-candidates` 链路已通，下一步重点是过滤规则，不是扩 planner。

### 多页面观测测试目标（本轮）
- `settings_page`：验证 `button / row-card / scroll`
- `quick_settings_page`：验证 `tile / toggle / header-action`
- `notification_panel_page`：验证 `notification-card / group / action`
- `content_feed_page(微博/小红书)`：先看 Accessibility 能否拿到 `feed-card / action-bar / input / search`
- 顺序固定：**Accessibility -> screenshot/vision -> OCR**

### 多页面实测结果（2026-04-22）
1. `settings_page`
   - artifact：`artifacts/2026-04-22/2026-04-22T12-36-31_dump-accessibility-tree_settings-tree/`
   - screenshot：`artifacts/2026-04-22/2026-04-22T12-39-16_capture-screenshot_settings-screen/`
   - 结果：`package=com.android.settings`、`nodeCount=40`、`clickable=11`、`scrollable=1`
   - PageState：`title=无线调试`、`elements=12`、`commands=14`
   - 可提取：返回按钮、列表滚动、设置项 row-card、长按项、文本锚点
   - 判断：`settings/list page` 直接以 Accessibility 为主真源
2. `quick_settings_page`
   - artifact：`artifacts/2026-04-22/2026-04-22T13-11-14_dump-accessibility-tree_quick-settings-tree/`
   - 结果：`package=com.android.systemui`、`nodeCount=170`、`clickable=46`
   - PageState：`title=控制中心`、`elements=62`、`commands=45`
   - 可提取：`tile`、`switch/toggle`、header action、设置按钮、隐私入口
   - 判断：系统快捷设置也可直接长命令；当前 screenshot 在该页失败，见 `artifacts/2026-04-22/2026-04-22T13-11-29_capture-screenshot_quick-settings-screen/response.json`
3. `xhs_feed_page`
   - artifact：`artifacts/2026-04-22/2026-04-22T13-29-46_dump-accessibility-tree_xhs-feed-real/`
   - 结果：`package=com.xingin.xhs`、`nodeCount=64`、`clickable=10`、`scrollable=3`
   - PageState：`title=更新`、`elements=19`、`commands=13`
   - 可提取：顶部 tabs（关注/发现/深圳）、搜索、底部导航、消息未读数、feed 卡片标题+作者+赞数
   - 关键观察：feed 卡片 bounds 很清楚，但当前暴露出来主要是 `long-click`，`viewId` 还是混淆 id，不能只靠 viewId 做稳定定位
   - 判断：小红书 feed 已能提取结构和卡片文本；**卡片内图片区/更细粒度动作** 仍需要 screenshot/vision 补洞
4. `xhs_search_page`
   - artifact：`artifacts/2026-04-22/2026-04-22T13-32-03_dump-accessibility-tree_xhs-search-real/`
   - 进入方式：从 feed 页点击搜索按钮（`bounds=[994,107]-[1111,224]`）
   - 结果：`package=com.xingin.xhs`、`nodeCount=43`、`clickable=20`、`editable=1`
   - PageState：`title=问ai`、`elements=20`、`commands=15`
   - 可提取：返回、搜索输入框、搜索按钮、问AI、拍照搜索、历史记录、猜你想搜、底部语音提问入口
   - 判断：输入框在小红书搜索页是可见且可定位的，`input` 类 filter 已有真实样本
5. `weibo_feed_page`
   - artifact：`artifacts/2026-04-22/2026-04-22T13-29-52_dump-accessibility-tree_weibo-feed-real/`
   - 结果：`package=com.sina.weibo`、`nodeCount=95`、`clickable=22`、`scrollable=5`
   - PageState：`title=@Transformer-周 :分享图片`、`elements=27`、`commands=35`
   - 可提取：推荐/关注 tab、底部导航、feed 列表、卡片正文、转发/评论/喜欢 action bar、"热门微博已更新" toast
   - 关键观察：微博的 action bar 暴露比小红书更强，且有较稳定的 `viewId`（如 `leftButton/midButton/rightButton`）；但也存在多个无标签 clickable 节点
   - 判断：微博 feed 的可操作性强于小红书；需要对多层 scroll container 和无标签点击节点做去重/降权
6. `jd_launch_ad_page`
   - artifact：`artifacts/2026-04-22/2026-04-22T13-29-58_dump-accessibility-tree_jd-home-real/`
   - 结果：实际拿到的是启动广告，不是首页；`title=跳过`、`elements=3`
   - 可提取：广告容器、跳过按钮
   - 判断：启动广告/开屏拦截页这类 blocker 页面，Accessibility 很适合先做专门 filter

### screenshot 当前状态
- 在真实 app 前台页再次测试：
  - `artifacts/2026-04-22/2026-04-22T13-30-38_capture-screenshot_xhs-feed-real/response.json`
  - `artifacts/2026-04-22/2026-04-22T13-30-43_capture-screenshot_weibo-feed-real/response.json`
- 两次都失败，错误一致：`FOREGROUND_SERVICE_MEDIA_PROJECTION` 权限/type 约束
- 当前结论：**Accessibility 线可继续推进；screenshot 线存在回归/权限门槛，暂时不能作为这轮 feed 测试主证据**

### 当前设计收敛
- 页面能力先分 5 类：
  1. `settings/list page`
  2. `quick-settings/tile page`
  3. `notification/card page`
  4. `feed/card page`
  5. `search/input page`
- filter 第一版建议：
  1. `actionable`：button / action / switch / checkable
  2. `scrollable`：列表、容器、信息流；优先保留**最深且文本最丰富**的 scroll container
  3. `input`：搜索框、评论框、编辑框
  4. `card-like`：大 bounds + 多文本 + 位于 scroll feed 内的卡片
  5. `image-like`：封面、头像、纯图片按钮；优先走 screenshot/vision
  6. `blocker`：开屏广告、升级弹窗、权限弹窗、通知卡片
- 设计原则：
  1. **Accessibility 先给可定位目标**：bounds / role / action / text / viewId
  2. **feed 页不要只信 viewId**：像小红书会混淆 id，要靠 `role + text anchor + bounds + page signature`
  3. **微博/小红书要区分“语义目标”和“执行目标”**：语义上能识别卡片，不代表系统直接给了 click action；必要时可基于卡片 bounds 合成 tap target
  4. **screenshot/vision 只补洞**：图片区、自绘控件、无标签 clickable、卡片内部细粒度区域
  5. **OCR 最后上**：只有 Accessibility 与 vision 标签都不足时再介入
- 当前下一步：
  1. 做 feed 页 filter 去噪：去重多层 scroll container、降低无标签 clickable 权重
  2. 给 `card-like` 目标补一层合成 tap candidate
  3. 等 screenshot 权限线修复后，再补微博/小红书卡片图片区的视觉定位验证

### 本轮最终收敛（2026-04-22）
- 用户决策：**实验阶段到此为止，先做基座设计，不再继续扩实验。**
- 当前接受的正式抽象：
  - `operation`：定义手机端原子操作能力
  - `observer`：提取页面并按 filter 返回关注目标
  - `anchor`：执行前后校验目标和状态转换
  - `workflow`：`observe -> filter -> pre-anchor -> operation -> observe -> post-anchor`
- 当前新真源：`docs/architecture/operation-observer-anchor-workflow.md`
- 当前下一步：固化 `operation schema`、`observer/filter interface`、`anchor spec`，再落单步 workflow engine

### 悬浮球真机不可见问题已定位并修正（2026-04-22 16:xx）
- 触发背景：
  - 用户反馈“看不到悬浮球”
  - 已知当时：
    - overlay permission = granted
    - `WorkbenchOverlayService` 已启动
    - `dumpsys window` 已存在 `TYPE_APPLICATION_OVERLAY`
- 根因判断：
  1. `WorkbenchOverlayService` 使用 `Gravity.END | Gravity.CENTER`
  2. 但拖拽 / 初始定位逻辑把 `LayoutParams.x/y` 当成**左上角绝对坐标**
  3. 导致 WindowManager 中窗口虽然存在，但视觉落点不符合预期，容易表现为“窗口存在但球不可见/不好找”
  4. `DevPanelActivity` 只读静态内存态，不能稳定反映 overlay 当前 showing 状态
- 本次修正：
  - overlay 改成 `Gravity.TOP | Gravity.START`
  - 气泡与面板都改成绝对坐标布局
  - 新增：
    - 气泡 `x/y` clamp
    - 左右贴边 snap
    - 面板按“向屏幕内展开”重新计算 `panelX`
  - 新增 `WorkbenchOverlayRuntimeStore`
    - 用 shared preferences 持久化 `showing / expanded`
    - DevPanel 不再依赖单纯静态变量
  - DevPanel 打开/关闭 overlay 后，延迟刷新一次 runtime 状态
- 真机证据：
  - build：
    - `./scripts/dev/build-android-lab.sh`
    - 版本从 `0.1.0026 -> 0.1.0027 -> 0.1.0028`
    - 过程中自动执行：
      - `blocks-spec-unit`
      - `blocks-spec-coverage`
      - `:app:testDebugUnitTest`
  - 安装：
    - `adb -s 100.120.173.56:46389 install -r .../app-debug.apk`
  - DevPanel 状态：
    - 打开 overlay 后 UI dump 显示：
      - `Overlay: granted / showing=true / expanded=false`
  - 可见性截图：
    - `.tmp/overlay_devpanel_v28.png`
      - 在 DevPanel 页面右侧可直接看到灰色半透明悬浮球，文字 `idle`
    - `.tmp/overlay_xhs.png`
      - 在小红书 `com.xingin.xhs` 前台页同样可见
  - WindowManager 证据（修复前后都能证明 service/window 存在）：
    - overlay window frame 现已稳定出现在右侧可见区域，例如：
      - `Rect(906, 627 - 1140, 861)`
- 当前结论：
  - **悬浮球现在在真机上已可见，且切到小红书后仍可见**
  - 当前 overlay v1 已满足继续接入“捕获模式 / 目标标注 / alias / test / save”的骨架前提
- 下一步更值得做：
  1. 接 `capture mode` 的长按选中 + 就地 alias 输入框
  2. 加 `smoke / real click` 测试菜单
  3. 保存到本地 + 发给 daemon，形成第一条可回放的 target 记录

### 悬浮球视觉第一轮修正（2026-04-22 16:32）
- 用户反馈：
  - “这个悬浮球你好歹做成一个球吧，现在这也太土了”
- 这次不再继续扩 capture mode，先修正悬浮球本体视觉。
- 修正内容：
  - 新增 `BubbleView`
  - 原先矩形 `TextView` 改成固定尺寸圆形球体
  - 风格改成：
    - 深灰底
    - 蓝灰描边
    - 中心短状态文字
    - 上方小状态点
  - 保持当前项目已确认的黑白灰主色 + 蓝灰点缀
- 相关文件：
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/workbench/BubbleView.kt`
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/workbench/WorkbenchViewFactory.kt`
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/workbench/WorkbenchOverlayService.kt`
- 验证：
  - `./scripts/dev/build-android-lab.sh`
  - `adb -s 100.120.173.56:46389 install -r .../app-debug.apk`
  - runtime version：`0.1.0029`
  - 自动回归仍通过：
    - `blocks-spec-unit`
    - `blocks-spec-coverage`
    - `:app:testDebugUnitTest`
- 真机截图证据：
  - `.tmp/overlay_ball_devpanel.png`
  - `.tmp/overlay_ball_xhs.png`
- 当前结果：
  - 悬浮球现在已是明确的圆形，不再是矩形块
  - 小红书前台页也能看到新的圆形球体
  - 视觉上已经更接近“真正的悬浮球”，可继续进入 capture mode 交互闭环

### logo + 悬浮球图标已切到 globe-in-hand（2026-04-22 17:xx）
- 用户提供新的图形方向：
  - 以 `globe in hand` 作为 APP logo 与悬浮球 icon
  - 状态不再靠中间文字，改为**不同颜色的粗边框**
- 本轮实现：
  - 新增资源：
    - `drawable-nodpi/flowy_mark_light.png`
    - `drawable-nodpi/flowy_mark_dark.png`
    - `drawable-nodpi/flowy_logo_tile.png`
  - `BubbleView` 改成：
    - 中间使用 `flowy_mark_light`
    - 外圈使用粗描边表达状态
    - 当前空闲/连接态截图可见为蓝灰粗边框
  - APP icon 改成：
    - 直接在 manifest 使用 `@drawable/flowy_logo_tile`
    - 避免 OEM 对 adaptive icon 的异常裁切
- 中间一次失败记录：
  - 初版 launcher 走 adaptive icon
  - 在真机系统“应用详情”页里图标被 OEM 裁坏，不能接受
  - 后续改成预合成 tile 后恢复正常
- 真实验证：
  - build/version：
    - `0.1.0030`
    - `0.1.0031`
    - `0.1.0032`
  - 每次 build 都自动通过：
    - `blocks-spec-unit`
    - `blocks-spec-coverage`
    - `:app:testDebugUnitTest`
  - 真机截图证据：
    - `.tmp/overlay_logo_ball_ok2.png`
      - DevPanel 页已显示 globe-in-hand 悬浮球
    - `.tmp/overlay_logo_ball_xhs_ok2.png`
      - 小红书前台页已显示 globe-in-hand 悬浮球
    - `.tmp/flowy_app_info_ok3.png`
      - 系统“应用详情”页已显示新的 APP logo
- 当前结论：
  - globe-in-hand 现在已经同时用于：
    1. APP logo
    2. 悬浮球 icon
  - 粗边框状态方案已落到 bubble 本体
  - 当前只对 `idle/connected` 可见态做了真机截图验证；其他状态色已接线，但还未逐个 smoke

### 非 adb 远端控制口识别失败，当前拿到的是 adb TLS 口（2026-04-22 22:xx）
- 触发背景：
  - 用户要求：**不能借助 adb，也要能远端控制 / 截屏 / 升级**
  - 最新给定地址：`100.120.173.56:43325`
- 本轮探测：
  1. 直接 HTTP 探测：
     - `GET /health`
     - `GET /exp01/clients`
     - 都没有表现出当前仓库 `services/mac-daemon` 的 `exp01` HTTP 行为
  2. 原始 TCP 探测：
     - 空连接：端口可连，但无任何 `exp01` banner
     - 随机 JSON / HTTP upgrade：不是当前 Flowy control surface
  3. ADB protocol 探测：
     - 新增脚本：`scripts/dev/probe-control-endpoint.py`
     - 对 `100.120.173.56:43325` 发送最小 ADB `CNXN` 握手后，收到：
       - `command = "STLS"`
- 结论：
  - `100.120.173.56:43325` **不是** 当前项目实现的 Flowy `exp01` 控制端口
  - 它更像 **Android Wireless Debugging / adb TLS transport** 端口
  - 因此：**当前还没有拿到“非 adb 可直接控制 Flowy runtime”的真实 daemon 地址**
- 当前证据：
  - 运行：
    - `python3 scripts/dev/probe-control-endpoint.py 100.120.173.56:43325`
  - 关键输出：
    - `classification = "adb-wireless-debugging-tls"`
    - `adb.command = "STLS"`
- 当前影响：
  - 不能把这个端口当成：
    - 远端 CLI 控制面
    - WebSocket control endpoint
    - screenshot / upgrade endpoint
- 下一步收敛：
  1. 需要一个**真实的 Flowy daemon 地址**（HTTP/WS control surface）
  2. 或者我们把 Android 端切到“主动连远端 daemon”的模式后，再由 CLI 打那个 daemon
  3. 在这之前，不能宣称已经具备“完全脱离 adb 的远控闭环”

### 悬浮菜单输入法抬顶 / 输入框失焦问题已修正一轮（2026-04-22 23:xx）
- 用户反馈：
  - 输入法弹出时，悬浮菜单像是被往上顶到贴边
  - 紧接着输入法又消失
  - 同时配置输入区布局难看、尺寸也不对
- 本轮根因判断：
  1. `WorkbenchOverlayService` 每秒 `renderState()`
  2. `renderState()` 之前会无条件 `renderSection()`
  3. `renderSection()` 会 `removeAllViews()` 然后重建当前 section
  4. 所以用户一旦在 `EditText` 中输入，下一次刷新就可能把输入框整个销毁，导致：
     - 焦点丢失
     - 输入法关闭
     - 叠加 window 的 IME 调整表现，体感就像“窗口被顶上去然后键盘消失”
- 本轮修正：
  - 新增：
    - `OverlaySectionRenderKey.kt`
  - 当前 section 只有在以下情况才重建：
    - section 切换
    - 当前 section 的关键状态变化
  - 对 panel window 增加：
    - `softInputMode = SOFT_INPUT_ADJUST_NOTHING`
    - 避免 IME 弹出时把悬浮层整体往上推
  - 输入区 UI 重排：
    - daemon 地址 / 端口改成更清晰的字段卡片
    - host 宽、port 窄
    - 输入框改成圆角描边、统一高度、黑灰蓝配色
    - 模式切换改成更像 segmented control 的样式
- 代码：
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/workbench/WorkbenchOverlayService.kt`
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/workbench/WorkbenchViewFactory.kt`
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/workbench/OverlaySectionRenderKey.kt`
  - `explore/android-daemon-lab/app/src/test/kotlin/com/flowy/explore/ui/workbench/OverlaySectionRenderKeyTest.kt`
- 构建证据：
  - `./scripts/dev/build-android-lab.sh`
  - 自动 gates：
    - `check-file-lines`
    - `blocks-spec-unit`
    - `blocks-spec-coverage`
    - `:app:testDebugUnitTest`
  - build 成功
  - runtime version：
    - `0.1.0033 -> 0.1.0034`
- 当前边界：
  - **已完成代码级修正 + 编译/单测验证**
  - **还没有新的真机输入法 smoke 证据**

### APK 自动升级最小闭环已接通（2026-04-22 22:xx）
- 用户要求：
  - 安装先由远端 adb 完成
  - 后续不再依赖手工装包，而是进入自动升级
- 本轮实现：
  1. Mac daemon 新增升级接口：
     - `GET /flowy/upgrade/check`
     - `GET /flowy/upgrade/apk`
     - `GET /flowy/upgrade/apk/download`
  2. daemon 从本地真源读取：
     - `explore/android-daemon-lab/config/runtime-version.json`
     - `explore/android-daemon-lab/app/build/outputs/apk/debug/app-debug.apk`
  3. Android 悬浮菜单“检查升级”已接真实流程：
     - 检查版本
     - 拉取 APK manifest
     - 下载 APK 到 cache
     - 通过 `FileProvider` 拉起系统安装器
     - 若系统未允许“安装未知应用”，自动跳到对应设置页
- Android 侧新增：
  - `CheckUpgradeBlock.kt`
  - `DownloadUpgradeApkBlock.kt`
  - `PromptInstallApkBlock.kt`
  - `UpgradeCheckFlow.kt`
  - `VersionComparator.kt`
  - `res/xml/file_provider_paths.xml`
- Mac daemon 侧新增：
  - `services/mac-daemon/src/flows/upgrade_apk_manifest_flow.go`
  - `services/mac-daemon/src/foundation/version_compare.go`
- 构建 / 测试证据：
  - `go test ./...`（`services/mac-daemon`）
  - `./scripts/dev/build-android-lab.sh`
  - 自动 gates：
    - `check-file-lines`
    - `blocks-spec-unit`
    - `blocks-spec-coverage`
    - `:app:testDebugUnitTest`
  - build 成功
  - runtime version：
    - `0.1.0034 -> 0.1.0035`
- daemon 真机可访问证据：
  - 本机：
    - `curl 'http://127.0.0.1:8787/flowy/upgrade/check?currentVersion=0.1.0034'`
    - 返回 `latestVersion=0.1.0035, available=true`
  - Tailscale 地址：
    - `curl 'http://100.86.84.63:8787/flowy/upgrade/check?currentVersion=0.1.0034'`
    - 返回 `manifestUrl=http://100.86.84.63:8787/flowy/upgrade/apk`
  - 下载口：
    - `curl -I http://127.0.0.1:8787/flowy/upgrade/apk/download`
    - 返回 `200 OK`
- 设备当前事实：
  - `adb -s 100.120.173.56:43325 shell dumpsys package com.flowy.explore`
  - 当前可见：
    - `versionName=0.1.0033`
- 当前结论：
  - **自动升级代码链路已接通**
  - **daemon 升级接口已可用**
  - **手机当前版本落后于 daemon 最新包，具备真实升级测试条件**
- 当前未完成：
  - 还没有补到“用户在手机上点击检查升级 -> 系统安装器弹出 -> 完成安装”的最终真机证据

### 版本 bump 与实际 APK 版本错位问题已修正（2026-04-22 22:xx）
- 新发现：
  - 之前 `build-android-lab.sh` 显示版本已 bump
  - 但设备实际安装后仍落后一版
- 根因：
  1. `app/build.gradle` 在 **Gradle 配置阶段** 就读取了 `runtime-version.json`
  2. 原先的 `bumpRuntimeVersion` 在 `preBuild` 才执行
  3. 导致同一次构建里：
     - JSON 文件先被 bump
     - 但 APK 仍使用旧的 `versionCode/versionName`
- 修正：
  - 版本 bump 前移到：
    - `scripts/dev/build-android-lab.sh`
  - Gradle 改为优先读取：
    - `FLOWY_RUNTIME_VERSION_NAME`
    - `FLOWY_RUNTIME_BUILD_NUMBER`
  - `preBuild` 不再依赖旧的 `bumpRuntimeVersion`
- 验证证据：
  - `runtime-version.json`：
    - 当前 `0.1.0037`
  - `aapt dump badging app-debug.apk`：
    - `versionCode='37'`
    - `versionName='0.1.0037'`
- 当前真机状态：
  - 已通过远端 adb 安装：
    - `0.1.0036`
  - 证据：
    - `adb -s 100.120.173.56:43325 shell dumpsys package com.flowy.explore`
    - `versionName=0.1.0036`
- 当前升级测试条件：
  - 手机：`0.1.0036`
  - daemon 最新 APK：`0.1.0037`
  - `GET http://100.86.84.63:8787/flowy/upgrade/check?currentVersion=0.1.0036`
    - 返回 `available=true`

### 悬浮菜单压缩 + 输入迁到次级配置页（2026-04-25）
- 用户新要求：
  - 悬浮窗展开后，在狭窄空间内尽量提高有效信息密度
  - 去掉没意义的显示
  - 字体变小
  - 不容易输入的内容移到次级菜单
  - 需要考虑输入法遮挡
- 本轮实现：
  1. 状态区压缩
     - `OverlayStatusFormatter.statusSummary()` 改成紧凑短句：
       - `连接正常/连接中/连接异常`
       - `无障碍开/关`
       - `截图开/关`
       - `捕获开/关`
  2. overlay 主菜单压缩
     - 标题/正文/按钮字号整体下调
     - section padding / margin 收紧
     - system section 去掉重复的升级入口（一级菜单已直接有“检查升级”）
  3. `Agent 自身控制` 收口
     - 主菜单里不再直接内嵌 host/port 输入框
     - 只保留：
       - 模式切换
       - 当前连接目标摘要
       - `连接配置` 按钮
  4. 新增次级配置页
     - `DaemonConfigActivity.kt`
     - `activity_daemon_config.xml`
     - 通过正常 Activity 承载 host / port 输入，绕开 overlay 内联输入法问题
     - 点击保存后：
       - 写入 `DevServerOverrideStore`
       - 自动 stop/start daemon 重连
- 代码：
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/workbench/OverlayStatusFormatter.kt`
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/workbench/WorkbenchViewFactory.kt`
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/workbench/WorkbenchOverlayService.kt`
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/DaemonConfigActivity.kt`
  - `explore/android-daemon-lab/app/src/main/res/layout/activity_daemon_config.xml`
  - `explore/android-daemon-lab/app/src/main/AndroidManifest.xml`
  - `explore/android-daemon-lab/app/src/main/res/values/themes.xml`
- 构建证据：
  - `./scripts/dev/build-android-lab.sh`
  - 自动通过：
    - `check-file-lines`
    - `blocks-spec-unit`
    - `blocks-spec-coverage`
    - `:app:testDebugUnitTest`
  - 版本：
    - `0.1.0044`
- 当前边界：
  - 代码和回归已通过
  - 本轮装机时，旧地址 `100.120.173.56:40791` 已经 `connection refused`
  - 因此还没有拿到这版的最新真机截图证据

### 小红书搜索采集 flow 探索（2026-04-25）
- 当前目标收敛：
  - **搜索关键词**
  - **进入帖子详情**
  - **采集图片 / 正文 / 评论**
  - **回到结果列表继续下一个**
- 当前要求：
  - 控制面必须走 **Flowy 自己的 command / workflow**，不能再用 adb 充当操作执行面。
- 当前已知控制面状态：
  - 已有 command：
    - `ping`
    - `fetch-logs`
    - `capture-screenshot`
    - `dump-accessibility-tree`
  - 还缺最小 operate command：
    - `tap`
    - `input-text`
    - `press-key`
    - `scroll`
    - `back`
- 基于已有 XHS feed/search 观测结果，当前建议把页面拆成 6 类：
  1. `xhs_home_feed_page`
     - 识别信号：
       - 顶部 tabs：`关注 / 发现 / 视频`
       - 右上 `搜索`
     - 页面目标：
       - 找到搜索入口
     - 必要操作：
       - `tap(search_entry)`
  2. `xhs_search_input_page`
     - 识别信号：
       - 搜索输入框
       - 搜索按钮 / 键盘搜索键
       - 历史记录 / 猜你想搜 / 问AI
     - 页面目标：
       - 输入关键词
       - 提交搜索
     - 必要操作：
       - `tap(search_input)`
       - `input-text(keyword)`
       - `press-key(search)` 或 `tap(search_submit)`
  3. `xhs_search_result_page`
     - 识别信号：
       - 当前关键词可见
       - 结果列表 scroll container
       - 帖子卡片 / 作者 / 摘要 / 封面
     - 页面目标：
       - 维护“未访问结果卡片队列”
       - 点击进入下一条帖子
       - 列表不足时继续滚动
     - 必要操作：
       - `tap(result_card)`
       - `scroll(result_list)`
  4. `xhs_post_detail_page`
     - 识别信号：
       - 帖子正文区域
       - 图片区域 / 图片计数
       - 作者信息
       - 评论入口或评论列表
     - 页面目标：
       - 采集正文
       - 采集图片素材
       - 采集评论首屏
       - 继续向下滚动评论并追加采集
     - 必要操作：
       - `capture-screenshot`
       - `dump-accessibility-tree`
       - `scroll(detail_or_comment_list)`
  5. `xhs_comment_expanded_page`（若评论区与正文区拆成独立滚动）
     - 识别信号：
       - 评论标题 / 评论输入框 / 评论列表
     - 页面目标：
       - 分页采集评论
       - 判定“没有更多评论”
     - 必要操作：
       - `scroll(comment_list)`
       - `back`
  6. `xhs_blocker_page`
     - 识别信号：
       - 登录弹窗
       - 权限弹窗
       - 升级弹窗
       - 开屏广告 / 浮层遮挡
     - 页面目标：
       - 先消障，再回主流程
     - 必要操作：
       - `tap(close/skip/deny/later)`
       - 若无法消除，则 `emit-event` 标记人工接管
- 当前建议的采集结果结构：
  - `search.keyword`
  - `result.rank`
  - `post.title`
  - `post.author`
  - `post.body_text`
  - `post.images[]`
  - `post.comments[]`
  - `post.source_page_signature`
  - `artifacts.screenshot[]`
  - `artifacts.accessibility[]`
- 当前判断：
  - 小红书这条链路不能只停在“搜索并点开”
  - 真正的 workflow 必须覆盖：
    - `搜索 -> 结果列表 -> 帖子详情 -> 评论采集 -> 返回结果列表 -> 去重继续`
- 当前下一步：
  1. 先把这条链路沉成正式 flow 文档
  2. 再补 Flowy operate commands（tap / input / press-key / scroll / back）
  3. 再按 flow 真机执行

### 跨 APP 内容采集骨架抽象（2026-04-25）
- 用户新增要求：
  - 不只做小红书单一路径
  - 要抽象成跨 APP 可复用框架
  - 同一套骨架里要同时容纳：
    - 操作行为
    - 目标锚点
    - 采集字段
    - reply / like / comment 等交互入口点
  - Mac / remote daemon 需要能完整下发操作并拿回结构化结果
- 当前抽象判断：
  - 通用层不应该写“这是小红书搜索按钮 / 这是微博评论按钮”
  - 通用层应该只定义：
    1. workflow skeleton
    2. operation set
    3. target anchor model
    4. content extraction contract
    5. daemon command/event contract
  - APP 差异应该单独落在 `app profile`
  - 字段差异应该单独落在 `field mapping / extractor spec`
- 当前准备沉淀为正式真源：
  - `docs/architecture/app-collection-workflow-abstraction.md`

### operate blocks / control surface 第一轮接线（2026-04-25）
- 本轮新增 Android operate blocks：
  - `TapBlock`
  - `ScrollBlock`
  - `InputTextBlock`
  - `BackBlock`
  - `PressKeyBlock`
- 本轮新增运行 flow：
  - `OperationRunFlow`
- 本轮接线结果：
  - `WsSessionFlow` 已开始支持 command：
    - `tap`
    - `scroll`
    - `input-text`
    - `back`
    - `press-key`
  - `DaemonStartupFlow.sendHello()` 已把这些 capabilities 发到 Flowy control surface
  - 当前 `tap/scroll` 走 AccessibilityService `dispatchGesture`
  - 当前 `input-text` 走 Accessibility editable node + `ACTION_SET_TEXT`
  - 当前 `back` 走 `GLOBAL_ACTION_BACK`
  - 当前 `press-key` 第一版只支持：
    - `back`
    - `home`
    - `recents`
    - `notifications`
    - `quick_settings`
- 当前代码门禁证据：
  - `./scripts/dev/build-android-lab.sh`
  - 通过：
    - `check-file-lines`
    - `blocks-spec-unit`
    - `blocks-spec-coverage`
    - `:app:testDebugUnitTest`
  - 版本：
    - `0.1.0049`
- 本轮装机证据：
  - `adb -s 100.127.23.27:45949 install -r .../app-debug.apk`
  - 安装成功
- 当前真实 blocker：
  - 设备目前处于**密码锁屏界面**
  - 本地 Mac daemon 已拉起：`:8787`
  - 但因为手机未解锁，Flowy runtime 还没有进入前台并连回 daemon
  - 所以还不能继续做“只用 Flowy 控制小红书”的真机链路验证
- 当前下一步：
  1. 设备解锁
  2. 启动 Flowy runtime
  3. 用 `exp01/clients` 确认新 operate capabilities
  4. 再只走 Flowy 跑 XHS `deepseek v4`

### 升级交互改为“检查 + 手动升级”（2026-04-25）
- 用户新要求：
  - 需要新增**手动升级按钮**
  - 启动检查升级后先**提示有升级**
  - 再走真机闭环验证
- 本轮实现：
  1. `UpgradeCheckFlow` 改成两段：
     - `check()`：只检查并缓存 pending upgrade
     - `installPending()`：手动触发下载 APK + 拉起安装器
  2. 新增：
     - `UpgradeStateStore`
     - 保存 `pendingVersion / pendingManifestUrl`
  3. UI 接线：
     - overlay 一级菜单新增 `手动升级`
     - DevPanel 新增：
       - `Check Upgrade`
       - `Manual Upgrade`
  4. 检查到新版本后的提示：
     - overlay: `发现新版本 x，请点手动升级`
     - DevPanel: `发现新版本 x，请点 Manual Upgrade`
- 构建证据：
  - `./scripts/dev/build-android-lab.sh`
  - 通过：
    - `check-file-lines`
    - `blocks-spec-unit`
    - `blocks-spec-coverage`
    - `:app:testDebugUnitTest`
  - 当前版本：
    - daemon / APK 构建产物：`0.1.0050`
- 真实升级条件证据：
  - 设备当前已安装：
    - `0.1.0049`
  - daemon 升级检查：
    - `GET /flowy/upgrade/check?currentVersion=0.1.0049`
    - 返回：
      - `latestVersion=0.1.0050`
      - `available=true`
  - daemon APK manifest：
    - `GET /flowy/upgrade/apk`
    - 返回：
      - `versionName=0.1.0050`
      - `downloadUrl=/flowy/upgrade/apk/download`
- 当前 blocker：
  - 真机当前仍停在系统锁屏 / 密码输入界面
  - 所以还不能完成：
    - 打开 Flowy
    - 点击 `Check Upgrade`
    - 提示升级
    - 点击 `Manual Upgrade`
    - 拉起安装器
- 当前下一步：
  1. 解锁手机
  2. 打开 Flowy DevPanel
  3. 点 `Check Upgrade`
  4. 看到升级提示
  5. 点 `Manual Upgrade`
  6. 验证系统安装器弹出

### 升级闭环真机修复与验证（2026-04-25）
- 本轮先抓到的真根因 1：
  - `CHECK UPGRADE` 失败
  - `daemon.log` 证据：
    - `upgrade_check_failed`
    - `CLEARTEXT communication to 100.86.84.63 not permitted by network security policy`
- 对应修复：
  - `app/src/main/res/xml/network_security_config.xml`
  - 从只放行 `127.0.0.1 / localhost` 改为 dev-lab 全局 `base-config cleartextTrafficPermitted=true`
- 修复后真机证据：
  - 当前已装版本：`0.1.0052`
  - daemon 最新版本：`0.1.0053`
  - 点击 `CHECK UPGRADE` 后：
    - `upgrade_available`
    - `pending_version=0.1.0053`
    - `pending_manifest_url=http://100.86.84.63:8787/flowy/upgrade/apk`

- 本轮再抓到的真根因 2：
  - `MANUAL UPGRADE` 第二步失败
  - `daemon.log` 证据：
    - `upgrade_install_failed`
    - `Name must not be empty`
- 对应修复：
  - `app/src/main/res/xml/file_provider_paths.xml`
  - `FileProvider` path 配置从 `android:name / android:path` 改成无 namespace 的 `name / path`
- 根因判断：
  - AndroidX `FileProvider` 解析 `<paths>` 时读取的是非 namespace 属性
  - 所以会把 `android:name` 读成空字符串，导致 `Name must not be empty`

- 真机安装权限链路证据：
  - 首次点 `MANUAL UPGRADE` 会进入：
    - 系统页：`安装未知应用`
    - 开关：`允许从此来源安装应用 = 关闭`
  - 手动打开该开关后再次执行

- 真机最终闭环证据：
  - 安装前设备版本：`0.1.0054`
  - daemon 最新版本：`0.1.0055`
  - `CHECK UPGRADE` 后：
    - `upgrade_available 0.1.0055`
  - `MANUAL UPGRADE` 后：
    - `upgrade_download_started downloading 0.1.0055`
    - `upgrade_install_prompted installer-opened`
    - `flowy_upgrade_state` 被清空
  - 最终设备包信息：
    - `versionCode=55`
    - `versionName=0.1.0055`

- 本机型额外现象：
  - `installer-opened` 后界面不一定停留在标准安装确认页
  - 当前 Oplus ROM 上会回桌面或切走当前界面
  - 但安装仍然已经完成
- 当前结论：
  - 升级闭环的验收不能只看“安装器 UI 是否停留”
  - 必须以：
    1. `daemon.log` 的 `upgrade_install_prompted`
    2. `flowy_upgrade_state` 是否清空
    3. `dumpsys package` 最终版本号
    作为真正确认依据

### 调试证据路径纠偏（2026-04-26）
- 用户新增硬规则：
  - 本项目里**截屏证据不能再用 adb**
  - 页面探索、业务流程、调试截图都必须优先走 **Flowy 自己的 screenshot 能力**
- 本轮反思：
  - 我在业务链路探索时仍用了 `adb screencap` 做页面判断，这和当前项目目标不一致
  - 既然 Flowy 已有 `capture-screenshot` 能力，后续要先把 `Projection: ready` 补齐，再继续页面探索
- 当前后续执行约束：
  1. 页面/流程证据截图只认 Flowy screenshot artifact
  2. `adb` 最多保留给安装、设备连通、bootstrap 类 plumbing
  3. 业务页面探索的“观察 → 定位 → 证据”必须切回 Flowy command surface

### DevPanel 自动启动 daemon 真机验证（2026-04-26）
- 变更：
  - `explore/android-daemon-lab/app/src/main/kotlin/com/flowy/explore/ui/DevPanelActivity.kt`
  - 在 `onCreate` 内新增 `ensureDaemonRunning()`
  - 当前规则：打开 DevPanel 时，若 daemon 状态为 `idle/stopped`，自动执行 `StartDaemonBlock`
- 构建证据：
  - `./scripts/dev/build-android-lab.sh`
  - build + tests 通过
  - 版本从 `0.1.0057` bump 到 `0.1.0058`
- 真机验证步骤：
  1. 安装 `0.1.0058`
  2. `am force-stop com.flowy.explore`
  3. 仅拉起 `com.flowy.explore/.ui.DevPanelActivity`
  4. 轮询 `http://127.0.0.1:8787/exp01/clients`
- 真机结果：
  - `poll 2` 仍为空
  - `poll 3` 开始出现 `client_hello`
  - `runtimeVersion = 0.1.0058`
- 当前结论：
  - “打开 Flowy 后 daemon 自动启动并回连”已闭环成立

### 自动启动后的 accessibility 能力异常（2026-04-26）
- 现象：
  - `0.1.0058` 自动回连后，`client_hello.capabilities` 仅剩：
    - `ping`
    - `fetch-logs`
    - `capture-screenshot`
  - 缺少：
    - `dump-accessibility-tree`
    - `tap`
    - `scroll`
    - `input-text`
    - `back`
    - `press-key`
- 代码观察：
  - `DaemonStartupFlow.sendHello()` 会按 `AccessibilityStatusReader.isEnabled()` 一次性决定是否上报 accessibility/operation capabilities
- 当前设备证据：
  - `adb shell settings get secure enabled_accessibility_services` 返回 `null`
  - `adb shell dumpsys accessibility` 未见 Flowy service 处于 bound/enabled 生效状态
- 当前判断：
  - 新版本安装/重启后，当前机型上的 accessibility 授权状态可能丢失或未重新绑定
  - 这与 “daemon 自动启动” 是两个独立问题
- 下一步：
  1. 先恢复 Flowy accessibility 权限
  2. 再验证完整 capabilities 是否恢复
  3. 如果恢复，则进一步判断是否需要在启动后补发一次 `client_hello` / capability refresh

### accessibility 能力刷新真因与闭环（2026-04-26）
- 真因 1（设备/系统行为）：
  - 我在验证自动启动时用了：
    - `adb shell am force-stop com.flowy.explore`
  - 当前 Oplus / Android 16 真机上，这会把：
    - `enabled_accessibility_services`
    - `accessibility_enabled`
    清空/关闭
  - 证据：
    - `settings get secure enabled_accessibility_services -> null`
    - `settings get secure accessibility_enabled -> 0`
    - logcat 出现：
      - `removeEnableService diffService=[ComponentInfo{com.flowy.explore/com.flowy.explore.runtime.FlowyAccessibilityService}]`
- 真因 2（控制面同步缺口）：
  - `client_hello.capabilities` 只在 ws 建连时发送一次
  - 如果 Accessibility availability 变化，`/exp01/clients` 不会自动刷新
- 对应修复：
  1. Android：
     - `FlowyAccessibilityService` 在 `onServiceConnected/onDestroy` 时通知 availability change
     - `DaemonStartupFlow` 收到 availability 变化后补发 `client_hello`
  2. Mac daemon：
     - `RunClientSession` 支持同一 websocket 会话里的后续 `client_hello`
     - `AppState` 增加 `UpdateClientHello`
  3. build gate：
     - `scripts/dev/build-android-lab.sh` 新增 `mac-daemon-go-test`
- 构建/测试证据：
  - `./scripts/dev/build-android-lab.sh`
  - python gates 通过
  - `go test ./...`（`services/mac-daemon`）通过
  - 版本从 `0.1.0058` bump 到 `0.1.0059`
- 真机最终闭环：
  1. 安装 `0.1.0059`
  2. 恢复 accessibility
  3. 打开 Flowy，自动启动 daemon
  4. `/exp01/clients` 返回完整能力集：
     - `ping`
     - `fetch-logs`
     - `capture-screenshot`
     - `dump-accessibility-tree`
     - `tap`
     - `scroll`
     - `input-text`
     - `back`
     - `press-key`
  5. `dump-accessibility-tree capability-refresh-final-proof` 返回 `status=ok`
- 当前结论：
  - “打开 Flowy 自动起 daemon” 已闭环
  - “控制面能力集与 Accessibility 实际能力保持同步” 已闭环
  - 后续 accessibility 验证流程里不能再把 `force-stop` 当成普通冷启动手段
