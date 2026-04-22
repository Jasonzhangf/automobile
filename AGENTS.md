# Project AGENTS.md（Flowy Routing Edition）

## 索引概要
- L1-L6 `purpose`：项目定位与文档职责。
- L8-L17 `project-facts`：当前确认事实。
- L19-L26 `hard-guards`：项目硬护栏。
- L28-L33 `doc-ownership`：AGENTS / note / skill / docs 的真源边界。
- L35-L40 `mandatory-flow`：本项目标准执行顺序。

## 项目定位
- 本项目目标是构建 **Android 上可见、可交互、可中断的自动化 Agent hand**。
- 当前阶段先做 **感知与调试底座**，不是木马、不是隐蔽控制工具。
- 先把页面识别、截屏、调试链路做通，再扩展操作执行。

## 当前确认事实
- 主感知路线：`AccessibilityService` + `AccessibilityNodeInfo`。
- 视觉补充路线：截图，其后再接 OCR / vision。
- 当前优先级：**感知 > 截图 > 归一化 > 操作**。
- 当前实验收敛后，正式基座按 **operation + event** 架构推进。
- 正式执行闭环固定为：**observe -> filter -> pre-anchor -> operation -> observe -> post-anchor**。
- daemon 控制面采用 **多端驱动**：控制端可在手机本机、本地桌面或远端服务之间切换；开发阶段默认先走远端控制端。
- 手机端的操作与反馈必须统一成 **CLI + WebSocket 控制面**：手机端 WebSocket 可连接本地或远端控制端口；远端控制端口必须可被 CLI 包装，也可被 WebSocket 直接驱动。
- 手机端必须支持 **内建升级**；runtime update 与 APK update 都通过 daemon 通信链路和同一套配置真源协同完成。
- 全局代码组织固定为：**共用函数库（foundation）+ 能力块（blocks）+ 编排（flows）**，且每个功能实现必须保持**唯一真源**。
- 每次 build 必须自动 **bump 版本 + 跑测试**；不接受 compile-only build。
- 当前开发拓扑：**Mac 本地 daemon + 手机端 Agent**；手机端的识别结果与截图需要方便地回收到 Mac 调试。
- 工作区采用 **双 daemon + shared packages** 的 monorepo 结构。
- 当前 Android 侧最小实验对象是 **long-running daemon runtime**，不是正式 APP 脚手架。
- 两个 daemon 的服务地址、升级地址、协议版本必须共享同一份配置真源。
- 升级分两类：**partial runtime upgrade** 与 **APK in-app update**。
- 代码分三层：**foundation / blocks / flows**，三层必须独立。
- 版本从 **`0.1.0001`** 起，每次编译自动 bump 四位 build number。
- 每个源码文件 **不得超过 500 行**。
- 每次编译 **必须自动跑回归测试**。
- 当前阶段已从实验闭环切到 **基座设计优先**；先固化基座模型，再进入正式实现。

## 项目硬护栏
- 无证据不宣称完成。
- 禁止静默失败；失败必须保留原因或日志。
- 未经明确授权，不做删除/回滚/迁移/发布等破坏性操作。
- 禁止 broad kill：`pkill` / `killall` / `kill $(...)` / `xargs kill`。
- 事实进 `AGENTS.md`，探索进 `note.md`，详细结构进 `docs/architecture/*.md`。
- 不接受任何超过 500 行的源码文件作为临时例外。
- 不接受只编译不回归的构建流程。
- 当前空目录可作为保留路径，但在实验闭环前不视为正式模块开工。

## 文档职责
- `./AGENTS.md`：项目级事实、边界、已接受决策。
- `./MEMORY.md`：跨轮次稳定经验与复用判断。
- `./CACHE.md`：当前阶段短期工作现场。
- `./note.md`：探索日记、实验假设、失败记录、下一步。
- `./.agent/flowy-dev-skill/`：本地开发工作流与调试方法真源。
- `./docs/architecture/`：目录结构、模块职责、升级策略、门禁设计。
- `./docs/architecture/operation-observer-anchor-workflow.md`：正式基座的执行模型真源。
- `./docs/architecture/blocks-interface-spec.md`：blocks 最小接口真源。
- `./docs/architecture/workspace-structure.md`：workspace 目录结构与模块落位真源。
- `./docs/architecture/development-workflow.md`：模块开发、测试、经验沉淀流程真源。
- `./docs/architecture/build-test-pipeline.md`：版本 bump 与 build/test 流水线真源。
- `./docs/experiments/`：当前实验规格、成功判据、实验边界。

## 标准执行顺序
1. 先读 `./AGENTS.md`。
2. 进入 `./docs/architecture/` 找对应结构/架构文档。
3. 进入 `./docs/experiments/` 找当前实验规格与 gate。
4. 进入 `./.agent/flowy-dev-skill/SKILL.md` 执行开发/调试流程。
5. 读取 `./note.md`，避免重复试错。
6. 实验后先写证据与结论，再决定是否把结论提升为事实。
