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
