# MEMORY.md

> 说明：本文件只记录可跨轮次复用的稳定经验，不记录流水账。

## 当前稳定经验

- 正式基座采用 `operation + event` 架构。
- 正式执行闭环固定为：`observe -> filter -> pre-anchor -> operation -> observe -> post-anchor`。
- `Accessibility` 是第一真源；`screenshot/vision` 负责补洞；`OCR` 最后介入。
- feed 页需要区分“语义目标”和“执行目标”。
- 共享 schema 与页面模型统一进入 `packages/`，不进入平台运行时代码目录。
- control daemon 是可切换角色，不绑定单一机器；开发阶段默认先走远端控制端。
- 手机端操作与反馈统一走 CLI + WebSocket 控制面；CLI 与 WebSocket 必须共享同一套 command/event 真源。
- 手机端升级必须内建，并与 daemon 通信链路及共享 server profile 统一。
- 全局实现遵守：`foundation -> blocks -> flows`，每个功能必须只有一个权威实现。
- 每次 build 必须自动 bump 版本，并在 build 流程内自动跑测试；compile-only 不算有效构建。
- blocks 是最小能力闭环，统一通过 ref 串联，不直接承载长流程状态机。
- 不能把 block 接口真源当成“block 已完成”；每个 block 先单测+覆盖，再进入 flow 编排测试。
- block 的测试不只测 input example，还要测标准 success result / error result 形状。
- 流程如果会重复出现，必须沉淀到文档 + skill + 可执行脚本，而不是只留在聊天记录里。
- block 规格补完的最小标准是：标准 example + success/error result + valid boundary fixture + invalid fixture + validator 拒绝测试 + build 集成 gate。
- 手机端正式 UI 第一版采用悬浮球 + 展开菜单；悬浮球是交互入口，不是主保活机制，后台常驻主干仍是 Foreground Service。
- overlay workbench v1 的真目标是建立“观察 -> 捕获 -> 测试 -> 保存 -> 素材化”的开发闭环，且 Android 第一主路径是被动模式。
- 500 行硬门限只约束代码文件，不约束架构文档与说明文档。
- Android workbench 第一批骨架可以先用 overlay service + status polling + shared-preference overrides 落地，先把交互入口和被动模式状态桥接接上，再逐步替换成更深的 runtime 集成。
- 跨 APP 自动化不要把“页面流程、业务字段、操作能力”混成一层；应拆成：通用 workflow skeleton + app profile + field mapping + extractor/output schema。
- 点赞、回复、评论入口这类“交互入口点”应视为目标锚点的一部分，而不是临时页面按钮文本；这样同一套骨架才能迁移到不同 APP。
- Android dev-lab 若要连远端 daemon 的 `ws://` / `http://` 明文地址，`network_security_config` 不能只放行 `localhost`；否则会直接在真机报 `CLEARTEXT communication ... not permitted`。
- AndroidX `FileProvider` 的 `<paths>` 配置要用无 namespace 的 `name/path` 属性；用 `android:name/android:path` 会在安装链路上报 `Name must not be empty`。
- APK in-app upgrade 在 OEM ROM 上不能只靠“安装器页面还停在前台”来判定成功；必须最终以 `upgrade_install_prompted` 日志 + pending state 清空 + `dumpsys package` 版本号升高作为闭环证据。
- 本项目一旦 Flowy 自身已具备截图能力，后续调试截图与证据截图必须优先走 **Flowy screenshot command**；`adb screencap` 不再作为默认或常规证据路径，尤其不能用于业务流程探索证据。
- 在当前 Oplus/Android 16 真机上，`am force-stop com.flowy.explore` 会把 Flowy Accessibility service 从系统启用态移除；因此 accessibility 验证不能把 `force-stop` 当成无害重启手段。
- `client_hello` 的 capabilities 不是一次性静态真相；当 Accessibility availability 变化时，Android 侧需要补发 `client_hello`，Mac daemon 侧也必须接受并刷新同一 device 的 hello 状态。
- Android 16 / Oplus 上的 MediaProjection 长驻会话要按这个顺序建：**先拿 grant -> 提升前台服务到 `mediaProjection` type -> `registerCallback()` -> `createVirtualDisplay()` -> 复用同一 `MediaProjection + VirtualDisplay + ImageReader`**；否则会分别撞上 `android:project_media`、`Must register a callback before starting capture`、或 token/resultData 复用错误。
- `MediaProjectionSessionHolder.store()` 不能静默吞异常；投屏授权链路一旦失败，至少要把真实异常打到 logcat/状态文本，否则会把“没拿到权限”和“session 初始化顺序错误”混成一个假象。
- 已验证的真机叶子能力应先收口成标准骨架模块，再进入业务流程探索；当前固定骨架为：`observe-page -> filter-targets -> evaluate-anchor -> execute-operation -> emit-event`，其中 `capture-screenshot / dump-accessibility-tree / tap / scroll / input-text / press-key / back` 作为骨架叶子块挂接。
