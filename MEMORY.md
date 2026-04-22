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
