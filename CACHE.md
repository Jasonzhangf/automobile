# CACHE.md

> 说明：本文件记录当前阶段的短期工作现场，可随任务推进频繁更新。

## 当前阶段

- 阶段：采集骨架设计完成 → 准备编码实现
- 当前重点：最小可用采集 flow 编码 + 测试闭环

## 当前已完成

- ✅ 采集骨架状态机 v1.1（含 18 个闭环修复补丁）：`docs/architecture/collection-workflow-skeleton.md`
- ✅ XHS 页面选择器验证（点赞/收藏/关注/评论/评论点赞/子回复展开）
- ✅ 滚动采集实验验证（dump 后搜索 + 到底判定 + 重试恢复）
- ✅ 去重策略设计（双层：内存 set + seen_items.jsonl）
- ✅ 骨架状态机全景图（含故障旁路）
- ✅ 完整验证证据链写入 note.md

## 当前下一步

1. 最小骨架编码：foundation 层 → blocks 层 → flow 层，逐层推进
2. 第一个 flow：XHS search → enter detail → snapshot → back（无评论、无互动）
3. 每加一个 feature 前先跑已有测试回归
4. 逐步加 feature：评论滚动 → 互动操作 → 去重 → checkpoint/resume

## 当前约束

- code ≤ 500 行/文件
- 每次 build = version bump + 跑测试
- foundation / blocks / flows 三层独立
- 采集结果通过 WS artifacts 通道传到 Mac daemon
- 不用绝对坐标，不用 JS/Auto.js
- 操作随机间隔 0.8-2.0s
- XHS 用 root backend
