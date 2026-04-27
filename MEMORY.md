# MEMORY.md

> 说明：本文件只记录可跨轮次复用的稳定经验，不记录流水账。

## 当前稳定经验

- 正式基座采用 `operation + event` 架构。
- 正式执行闭环固定为：`observe -> filter -> pre-anchor -> operation -> observe -> post-anchor`。
- `Accessibility` 是第一真源；`screenshot/vision` 负责补洞；`OCR` 最后介入。
- 共享 schema 与页面模型统一进入 `packages/`，不进入平台运行时代码目录。
- control daemon 是可切换角色，不绑定单一机器；开发阶段默认先走远端控制端。
- 手机端操作与反馈统一走 CLI + WebSocket 控制面；CLI 与 WebSocket 必须共享同一套 command/event 真源。
- 手机端升级必须内建，并与 daemon 通信链路及共享 server profile 统一。
- 全局实现遵守：`foundation -> blocks -> flows`，每个功能必须只有一个权威实现。
- 每次 build 必须自动 bump 版本，并在 build 流程内自动跑测试；compile-only 不算有效构建。
- blocks 是最小能力闭环，统一通过 ref 串联，不直接承载长流程状态机。
- 不能把 block 接口真源当成"block 已完成"；每个 block 先单测+覆盖，再进入 flow 编排测试。
- 流程如���会重复出现，必须沉淀到文档 + skill + 可执行脚本，而不是只留在聊天记录里。
- 手机端正式 UI 第一版采用悬浮球 + 展开菜单；悬浮球是交互入口，不是主保活机制，后台常驻主干仍是 Foreground Service。
- 500 行硬门限只约束代码文件，不约束架构文档与说明文档。
- 跨 APP 自动化不要把"页面流程、业务字段、操作能力"混成一层；应拆成：通用 workflow skeleton + app profile + field mapping + extractor/output schema。
- Android dev-lab 若连远端 daemon 的 `ws://` / `http://` 明文地址，`network_security_config` 不能只放行 `localhost`。
- 本项目一旦 Flowy 自身已具备截图能力，后续调试截图与证据截图必须优先走 **Flowy screenshot command**；`adb screencap` 不再作为默认证据路径。
- 在当前 Oplus/Android 16 真机上，`am force-stop com.flowy.explore` 会把 Flowy Accessibility service 从系统启用态移除。
- 第三方业务 APP 的真实业务流探索必须坚持 **纯用户态操作**；`intent / deep-link / component 直开` 会触发真实风控风险。
- 一旦进入正式业务 flow 编程，用户态 operation 必须带 **随机时间间隔 / 抖动**，不能用固定节拍。
- 小红书当前约束：**只要 Flowy Accessibility Service 开启，搜索后点详情就会风控**；关闭后恢复正常。XHS 业务 flow 现阶段只能用 root backend。
- `open-deep-link` + `packageName` 在 Flowy daemon 内调用可能超时；adb 直接 `am start` 更可靠。
- operation backend 当前稳定策略：默认先走 accessibility；root 仅作为明确指定或必要时的增强 backend。

## XHS 页面选择器验证经验 (2026-04-27)

- XHS 所有 `resourceId` 全部被混淆 (`0_resource_name_obfuscated`)，不可用于定位。
- `contentDescription` 是最可靠定位锚：点赞/收藏/评论按钮的 cd 包含 `"点赞 N"` / `"已点赞N"` / `"收藏 N"` / `"已收藏N"` / `"评论 N"`。
- **点赞状态**：已点赞时 cd 变为 `"已点赞N"`，且 child ImageView `flags.selected=true`；未点赞时 cd 为 `"点赞 N"` 且 selected=false。**不要用 Button 自身的 selected，要用 child ImageView 的 selected**。
- **收藏状态**：与点赞完全相同的模式。
- **关注状态**：直接用 TextView `text` 判断：`"关注"` vs `"已关注"`。
- 评论面板的点赞按钮**没有 contentDescription**，也没有计数；只能通过层级关系定位（每条评论右侧 `LinearLayout(clickable=true)` > `ImageView.selected`）。
- 评论面板中没有搜索功能入口；定位特定评论只能通过滚动遍历 + 文本匹配。
- 评论列表结构：用户名(TextView, clickable) + 内容(TextView, 含时间/地区/回复) + 点赞(LinearLayout>ImageView)。

## 评论滚动采集经验 (2026-04-27)

- 每次 scroll 后 dump-ui-tree-root 拿到一屏全新评论快照；连续 3 次 scroll 评论内容零重叠。
- 到底判定三个信号：A) text set 完全重复；B) 出现尾部标记；C) node 数不变 + 零新 text。
- 连续高频 scroll+dump 会导致 phone daemon 卡死（实测 8 次后 504）；必须在每次 scroll 后加入随机延迟 0.8-1.5s。
- 采集结果传输复用 WS command dispatch artifacts 通道，不需要新传输通道。
- 评论面板 scrollable 容器是 RecyclerView，bounds 在 `[0, 331 - 1216, 2444]` 范围内。

## 采集骨架状态机经验 (2026-04-27)

- 骨架固定为 8 个状态：`INIT → LIST_ENTRY → PICK_NEXT → ENTER_DETAIL → DETAIL_TASK → BACK_TO_LIST → CHECK_CONTINUE → DONE`。
- 必须有 Checkpoint & Resume：checkpoint 记录 current_state / entry_context / list_position / current_item_id / completed_count；INIT 阶段检测 checkpoint 存在则 resume。
- DETAIL_TASK 必须增量写入：每个 phase 完成后立即追加到 item 文件，中途失败不丢已采集数据。
- 去重采用双层：内存 set + `seen_items.jsonl` 持久化落盘；用 title hash 作 item_id，不同 APP 由 profile 定义。
- 错误分 5 级：操作级(单步 retry 3次) → 页面级(skip item) → 列表级(restart app) → 系统级(wait reconnect) → 进程级(PROCESS_CRASH: restart app + load checkpoint, max 5次/run)。
- BACK_TO_LIST 失败后必须重建入口上下文（saved_keyword + scroll_index），不能只 launch app。
- 每次大状态转换前做 WS 健康检查 ping；fail → restart phone app → reconnect → load checkpoint。
- phone 端内存约束：dump 结果不保留副本，采集数据构建为 JSON 后立即通过 WS 发送，checkpoint 文件在 Mac daemon 侧。
- 图片 URL 无法通过 dump-ui-tree 获取（只能拿到 desc），需要截图 + OCR 或网络层拦截。
