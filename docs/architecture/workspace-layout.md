# Workspace Layout v0.1

## 目标

把 Flowy 工作区定义成一个 **双 daemon + 共享协议 + 强制门禁** 的 monorepo：

- **Mac daemon**：本地调试枢纽，负责接收、落盘、回放、比对手机侧 run bundle
- **Android daemon / app runtime**：手机端可见 Agent hand，负责感知、截图、上传、升级检查、运行时热更新
- **Shared packages**：协议、页面模型、服务地址、回归样本的唯一真源

补充原则：

- daemon 控制面支持 **多端驱动**
- 手机端操作与反馈统一成 **CLI + WebSocket 控制面**
- 手机端支持 **内建升级**
- 全局功能实现遵守 **foundation / blocks / flows + 唯一真源**

## 阶段规则

当前阶段经历了两段：

### Phase 1：exploration first

- 先做实验闭环与证据收集
- 先验证最小 daemon 能否长时运行、接收命令、执行、反馈、记录日志
- 先验证 capture -> upload -> inspect 链路

### Phase 2：base-first design

- 实验足够后，不再继续扩实验面
- 先固化正式基座设计
- 先把 `operation / observer / anchor / workflow` 模型定为真源
- 再开始正式脚手架与基础功能层

因此本文件里的目录更多是 **模块真源边界 + 设计目标**。
正式执行模型见：`docs/architecture/operation-observer-anchor-workflow.md`

## control daemon 拓扑

### 1. control daemon 是角色，不绑定单一机器

Flowy 的控制面必须抽象成一个可切换角色。

它理论上可运行在：

- 手机本机
- 本地桌面
- 远端服务

当前开发默认：

- **先走远端控制端**
- 手机端负责 observer / operation / event emit
- Mac 继续承担调试、artifact 检查、回放

### 2. control mode 必须由共享配置驱动

控制端位置切换不能靠平台内部硬编码。

必须统一走：

```text
packages/config/server-profile/
```

共享配置至少要描述：

- control mode（on-device / desktop / remote）
- daemon base url
- command endpoint
- event/upload endpoint
- cli endpoint / cli route
- upgrade endpoint
- protocol version

### 3. transport 和 control role 分离

transport 是通道：

- adb reverse / forward
- ws
- http

control role 是控制平面所在位置：

- on-device
- desktop
- remote

这两个概念不能混写。

### 4. CLI 和 WebSocket 是同一控制面的两个入口

手机端控制与反馈不能拆成两套协议心智。

必须统一成同一个控制面：

- **WebSocket**：运行时长连接、命令下发、事件上报
- **CLI**：给开发脚本、远端操作员、自动化工具使用的人类入口

要求：

- 手机端 WebSocket 可以连接本地或远端控制端口
- 远端控制端口必须支持：
  - CLI 包装访问
  - WebSocket 直接访问
- CLI 和 WebSocket 底层共享同一套 command / event / workflow 真源

## 顶层目录

```text
flowy/
├─ AGENTS.md
├─ note.md
├─ .agent/
│  └─ flowy-dev-skill/
├─ docs/
│  └─ architecture/
│     └─ workspace-layout.md
├─ scripts/
│  ├─ dev/
│  ├─ verify/
│  └─ release/
├─ services/
│  └─ mac-daemon/
├─ explore/
│  └─ android-daemon-lab/
├─ apps/
│  └─ android-agent/
├─ packages/
│  ├─ protocol/
│  ├─ page-model/
│  ├─ config/
│  │  └─ server-profile/
│  └─ regression-fixtures/
└─ artifacts/
```

## 模块职责

### 1. `services/mac-daemon/`

只放 **Mac 本地 daemon** 代码。

职责：
- 接收手机侧上传的 run bundle
- 本地持久化、索引、回放、比对
- 提供本地 debug API / WebSocket / HTTP 接口
- 提供可被 CLI 包装的控制入口
- 提供开发期升级包与运行时文件下发入口

建议子模块：

```text
services/mac-daemon/
├─ src/
│  ├─ server/        # HTTP / WS server
│  ├─ runs/          # run bundle 持久化与索引
│  ├─ replay/        # 回放/比对
│  ├─ transport/     # adb reverse / ws / http transport glue
│  ├─ upgrade/       # 运行时升级包与 APK 升级元数据
│  └─ main.*
└─ tests/
```

### 2. `explore/android-daemon-lab/`

只放 **当前探索阶段的 Android daemon 最小实验对象**。

职责：
- 后台长时运行验证
- 接收 Mac 端命令
- 执行最小感知任务
- 回传结果
- 本地记录日志

### 3. `apps/android-agent/`

只放 **实验完成后的正式 Android APP / 产品化容器**。

职责：
- Accessibility 感知
- 截图采集
- Overlay/UI 调试面板
- run bundle 组装与上报
- 升级检查与应用内升级
- 运行时文件热更新（partial upgrade）

建议子模块：

```text
apps/android-agent/
├─ app/
│  └─ src/
│     ├─ main/       # 生产主代码
│     └─ debug/      # 内建开发能力：调试入口/开发面板/直连 dev server
└─ tests/
```

**规则**：
- `src/debug/` 放开发专用能力，不污染 release 主路径。
- 感知、截图、同步、升级不要堆在一个大文件里；按 capability 拆子目录。

### 4. `packages/protocol/`

放 **手机 <-> Mac daemon** 通信协议真源。

内容：
- run bundle manifest schema
- capture request/response schema
- upgrade manifest schema
- event/log schema
- CLI / WebSocket 共享的 command schema

### 5. `packages/page-model/`

放 **归一化页面模型** 真源。

内容：
- `PageState`
- `UINode`
- command candidate schema
- raw accessibility dump -> normalized model 的中间结构

### 6. `packages/config/server-profile/`

放 **服务地址与环境配置唯一真源**。

这里必须解决你说的：
- Android 内建开发能力
- 直接访问和服务器通信的同一个地址
- 检测升级 / 拉取 partial 更新 / 查询 APK 更新 都走同一套 server profile

建议：

```text
packages/config/server-profile/
├─ server-profile.json      # 唯一真源
├─ android/                 # 生成给 Android 用的配置（后续脚本生成）
└─ mac/                     # 生成给 Mac daemon 用的配置（后续脚本生成）
```

**规则**：
- 地址、端口、升级 endpoint、协议版本不允许在多个平台各写一份。
- 只允许从 `server-profile.json` 生成或读取。
- control mode、runtime update source、APK update source 也必须从这里统一生成。
- CLI 入口地址与 WebSocket 控制端口也必须从这里统一生成。


### 7. `packages/foundation/` + `packages/blocks/` + `packages/flows/`

这是全仓代码分层规则，不一定现在开工，但目录职责必须先定清：

- `packages/foundation/`：公用函数、纯工具、无业务状态依赖
- `packages/blocks/`：最小能力块，只完成单一能力编排
- `packages/flows/`：任务流程编排，负责状态机与串联

硬规则：
- foundation 不感知业务流程
- blocks 不复制 foundation
- flows 不复制 blocks
- 三层必须拆开，不允许把三层揉进一个文件
- 每个功能实现必须只有一个权威实现，禁止在不同层或不同模块里各写一份近似逻辑。

### 8. `packages/regression-fixtures/`

放 **回归测试样本**。

内容：
- 历史 run bundle 样本
- 截图样本
- accessibility raw dump 样本
- 归一化 page-state 期望输出

用途：
- 每次编译后的自动回归测试
- 验证 page-model、协议兼容、升级兼容

### 9. `scripts/`

统一脚本入口，禁止把随意脚本散落到各模块根目录。

建议：

```text
scripts/
├─ dev/
│  ├─ mac-daemon-dev.*          # 启动本地 daemon
│  ├─ android-install-dev.*     # 编译 + 安装 debug APK
│  ├─ android-runtime-sync.*    # partial 文件同步
│  └─ full-dev-loop.*           # 一键本地开发循环
├─ verify/
│  ├─ check-file-lines.*        # 检查单文件 <= 500 行
│  ├─ run-regression.*          # 编译后自动跑回归
│  ├─ verify-mac-daemon.*       # Mac daemon 验证
│  └─ verify-android-agent.*    # Android 端验证
└─ release/
   ├─ build-mac-daemon.*
   ├─ build-android-apk.*
   ├─ publish-runtime-bundle.*
   └─ publish-upgrade-manifest.*
```

## 硬性门禁

### 1. 单文件行数限制

**硬性规则：每个源码文件 <= 500 行。**

要求：
- `scripts/verify/check-file-lines.*` 必须进入本地构建和 CI。
- 超过 500 行直接失败，不允许靠 review 口头放过。
- 大文件必须拆成：orchestration / blocks / pure helpers。

### 2. 每次编译自动跑回归

**硬性规则：任何 build 都必须自动触发回归测试。**

要求：
- `build -> regression` 是强绑定，不允许只编译不测。
- Mac daemon 与 Android 端都需要自己的模块测试。
- `packages/protocol` / `packages/page-model` 修改后，必须跑共享样本回归。

推荐顺序：

```text
lint/static-check
-> file-line gate
-> unit tests
-> regression fixtures replay
-> build artifact
-> post-build smoke verification
```

## Android 安装与开发模式

Android 必须支持 **内建开发模式**：

- Debug 版安装后，应用内就能：
  - 直连 dev server
  - 检测配置变更
  - 检测 partial runtime update
  - 检测 APK update
- 不依赖每次都改代码重新发包才能调试 server 地址

同时必须支持 **内建升级**：

- 通过 daemon 通信端口检查 runtime update
- 通过 daemon 通信端口检查 APK update
- partial runtime upgrade 与 APK update 共享同一套 server profile 真源
- 开发期默认连远端控制端，后续再切换到本机或桌面端

建议：
- 在 `app/src/debug/` 内提供 dev settings / hidden dev panel
- dev panel 可显示：
  - 当前 server profile
  - 当前 runtime bundle 版本
  - 当前 APK 版本
  - 上次同步结果
  - 连接测试结果

## 升级策略：两段式

### A. Partial upgrade（不动安装包）

适用于：
- 流程定义
- DSL / 规则
- prompt / agent config
- page recognition mapping
- 非 native 的资源文件
- 可解释型 page model 规则

特点：
- 通过文件传输直接更新
- 由 Mac daemon / server 下发 runtime bundle
- Android 端拉取后校验 manifest / checksum / protocol version
- 成功后热切换或下次启动生效

建议目录：

```text
apps/android-agent/runtime/
├─ active/
├─ staging/
├─ rollback/
└─ manifests/
```

### B. APK update（需要动安装包）

适用于：
- native / Kotlin 代码改动
- 权限改动
- Accessibility service 声明改动
- Overlay/UI 容器改动
- 网络栈/升级机制本身改动

特点：
- 通过应用内升级入口执行
- 安装前先检查版本、兼容性、最小 runtime bundle 版本
- 安装后自动执行一次 smoke regression

## 代码边界规则

- 两个 daemon 代码必须分目录，禁止混写。
- 通信协议、页面模型、服务地址配置必须收敛到 `packages/`，禁止两端复制。
- 任何跨端共享事实先落 `packages/`，再由两端消费。
- 运行时升级文件与 APK 升级文件必须分开发布、分开版本化。

## 推荐开发顺序

### Phase 0：实验闭环（当前阶段）
1. 先做最小实验文档与证据格式。
2. 先验证 `services/mac-daemon` 的最小接收/落盘。
3. 先验证 Android 最小 daemon：长时运行、接收命令、反馈、日志。
4. 先验证 capture + screenshot + upload。

### Phase 1：实验通过后再做脚手架
1. 再收敛 `packages/protocol` 真源。
2. 再铺 `packages/page-model` / `packages/config` / `packages/regression-fixtures`。
3. 再正式搭 `apps/android-agent` 与后续正式 APP 模块。
4. 最后再做 partial upgrade 与应用内 APK upgrade。

## 当前不做的事

- 不在现在定义复杂发布流水线
- 不在现在把所有升级通道一次做完
- 不在现在把操作执行和感知耦合
- 不接受任何 >500 行文件作为“先跑通再说”的例外

## 版本规则

- 起始版本：`0.1.0001`
- 每次编译自动 bump 4 位 build number
- 版本规则属于强制门禁，不依赖手工维护
