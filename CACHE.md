# CACHE.md

> 说明：本文件记录当前阶段的短期工作现场，可随任务推进频繁更新。

## 当前阶段

- 阶段：基座设计优先
- 当前重点：
  1. 固化 workspace 结构
  2. 固化开发流程
  3. 固化测试流程
  4. 固化经验沉淀方式

## 当前下一步

- 当前 5 个 core blocks 已补到：标准样本 + success/error + boundary + invalid + validator + build gate
- 当前已切到：悬浮球开发工作台 V1 规格定义
- 当前已完成：最小 UI 骨架与 passive-mode bridge 第一批代码
- 下一步进入：capture mode 的 target 选中 / confirm / alias / test / save 空骨架
- UI 稳定后，再回到 flow 编排与 runtime 落地

## 当前新增约束

- control daemon 支持多端切换，开发阶段先走远端
- 手机端操作与反馈统一走 CLI + WebSocket 控制面
- 手机端内建升级走 daemon 通信链路
- 全局功能实现必须保持唯一真源
