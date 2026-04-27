# Collection Workflow Skeleton v1

## 目标

定义跨 APP 内容采集的**通用骨架状态机**。

骨架与 APP profile 解耦：骨架定义"怎么跑"，profile 定义"找什么、点什么、拿什么"。

一句话结论：

```text
list_entry → detail_loop → detail_task → back_to_list → next_or_done
每个状态都有 anchor 校验 + retry 上限 + 回退路径
```

---

## 一、整体状态机

```
                     ┌──────────────────────┐
                     │     INIT             │
                     │  加载 config + profile│
                     │  初始化 dedup store   │
                     └─────────┬────────────┘
                               │
                     ┌─────────▼────────────┐
              ┌─────│     LIST_ENTRY        │
              │     │  搜索 或 时间线        │
              │     └─────────┬────────────┘
              │               │
              │     ┌─────────▼────────────┐
              │     │     PICK_NEXT        │
              │     │  从列表选择下一个 item │
              │     │  跳过已处理 (dedup)   │
              │     └─────────┬────────────┘
              │               │
              │     ┌─────────▼────────────┐
              │     │  ENTER_DETAIL        │
              │     │  tap item → 等锚点   │
              │     │  验证进入详情页       │
              │     └─────────┬────────────┘
              │               │
              │     ┌─────────▼────────────┐
              │     │  DETAIL_TASK         │
              │     │  按 config 执行任务   │
              │     │  采集 / 互动 / 滚动   │
              │     └─────────┬────────────┘
              │               │
              │     ┌─────────▼────────────┐
              │     │  BACK_TO_LIST        │
              │     │  press back / gesture│
              │     │  验证回到列表页       │
              │     └─────────┬────────────┘
              │               │
              │     ┌─────────▼────────────┐
              │     │  CHECK_CONTINUE      │
              │     │  已采集 < target_count│
              │     │  且列表未到底         │
              │     └──┬──────────┬────────┘
              │        │yes      │no
              └────────┘          │
                          ┌───────▼────────┐
                          │     DONE       │
                          │  写最终报告    │
                          └────────────────┘
```

---

## 二、状态定义

### 2.1 INIT

```json
{
  "state": "INIT",
  "actions": [
    "load_app_profile(appId)",
    "load_task_config(taskId)",
    "init_dedup_store(seen_ids_file)",
    "init_result_sink(output_path)",
    "validate_profile_selectors()"
  ],
  "on_failure": "abort"
}
```

**职责**：加载 APP profile 和任务配置，初始化去重存储和结果输出。
**去重存储**：是一个持久化文件（Mac daemon 侧），记录已处理 item 的唯一标识。
首次运行创建空文件；后续运行加载已有记录，跳过已处理项。

### 2.2 LIST_ENTRY

分支点：两种进入列表的方式。

```json
{
  "state": "LIST_ENTRY",
  "branches": {
    "search": {
      "steps": [
        "launch_app(intent)",
        "wait_anchor(activity=search_input)",
        "input_text(keyword)",
        "press_key(enter)",
        "wait_anchor(activity=search_result)",
        "observe_and_snapshot()"
      ]
    },
    "timeline": {
      "steps": [
        "launch_app(intent)",
        "wait_anchor(activity=feed)",
        "observe_and_snapshot()"
      ]
    }
  }
}
```

**锚点**：
- 搜索模式：搜索结果页 RecyclerView 出现 + 搜索 keyword 文本可见
- 时间线模式：Feed 页 RecyclerView scrollable 出现

### 2.3 PICK_NEXT

从列表中选择下一个未处理的 item。

```json
{
  "state": "PICK_NEXT",
  "actions": [
    "observe_page()",
    "filter_items(selector=profile.list_item_filter)",
    "dedup_check(item_id against seen_store)",
    "select_first_unseen()"
  ],
  "on_no_unseen": "scroll_and_retry | CHECK_CONTINUE",
  "scroll_retry_limit": 3
}
```

**去重策略**（见第四章详述）。

### 2.4 ENTER_DETAIL

点击 item 并验证进入了详情页。

```json
{
  "state": "ENTER_DETAIL",
  "pre_anchor": "page_is_list(current_activity == list_activity)",
  "operation": "tap(item_bounds_center)",
  "post_anchor": "page_is_detail(current_activity == detail_activity)",
  "anchor_poll": {
    "max_retries": 3,
    "interval_ms": [800, 1500],
    "strategy": "random_jitter"
  },
  "on_failure": {
    "retry_limit_reached": "BACK_TO_LIST | skip_item",
    "max_retries": 3
  }
}
```

**锚点校验**：操作前后必须有**页面级变化**。
- pre-anchor：操作前确认当前在列表页（Activity 名 + 关键节点存在）
- post-anchor：操作后确认进入详情页（Activity 名变化 or 详情页特征节点出现）
- 锚点轮询：不能假设一次 dump 就拿到新页面，必须 bounded poll

### 2.5 DETAIL_TASK

按 config 执行详情页任务。这是**可扩展**的核心。

```json
{
  "state": "DETAIL_TASK",
  "actions": "from task_config.phases",
  "phases": [
    {
      "name": "snapshot",
      "operation": "dump-ui-tree-root",
      "extract": "profile.detail_fields"
    },
    {
      "name": "interact",
      "condition": "config.enable_like || config.enable_fav",
      "operations": "profile.interaction_ops"
    },
    {
      "name": "comments",
      "condition": "config.enable_comments",
      "steps": [
        "tap(comment_entry)",
        "wait_anchor(comment_panel_visible)",
        "scroll_and_collect(max_scrolls, keyword)",
        "close_comment_panel"
      ]
    }
  ],
  "on_failure": "skip_detail → BACK_TO_LIST",
  "retry_limit": 3
}
```

**子状态：评论滚动采集**（见第三章）。

### 2.6 BACK_TO_LIST

从详情页返回列表。

```json
{
  "state": "BACK_TO_LIST",
  "pre_anchor": "page_is_detail",
  "operation": "press_key(back)",
  "post_anchor": "page_is_list",
  "anchor_poll": {
    "max_retries": 3,
    "interval_ms": [800, 1500]
  },
  "on_failure": {
    "tried_3_times": "launch_app(intent) → LIST_ENTRY",
    "rationale": "如果 back 回不到列表，直接重启 app 重新进入"
  }
}
```

### 2.7 CHECK_CONTINUE

```json
{
  "state": "CHECK_CONTINUE",
  "conditions": [
    "result_count < config.target_count",
    "list_has_more_items (scroll 3 次仍有新 item)",
    "not_at_bottom"
  ],
  "true": "PICK_NEXT",
  "false": "DONE"
}
```

### 2.8 DONE

```json
{
  "state": "DONE",
  "actions": [
    "write_result_summary()",
    "write_dedup_store()",
    "emit_completion_event()"
  ]
}
```

---

## 三、评论滚动采集子流程

嵌套在 DETAIL_TASK.phases.comments 内部。

```
while not at_bottom and scroll_count < max_scrolls:
  ┌─────────────────────────────────┐
  │ 1. dump-ui-tree-root            │ ← 获取当前屏快照
  │ 2. extract_comments(snapshot)   │ ← 提取评论 text
  │ 3. keyword_match(text, search)  │ ← 搜索命中则记录
  │ 4. collect_all(new_comments)    │ ← 全量采集则全部记录
  │ 5. bottom_detect(curr, prev)    │ ← 到底判定
  │ 6. scroll forward               │ ← 滚动
  │ 7. sleep(random 0.8-1.5s)       │ ← 随机延迟
  │ 8. error_recovery(if needed)    │ ← 异常恢复
  └─────────────────────────────────┘
```

### 到底判定（三个信号，任一满足即停止）

| 信号 | 判定方法 | 可靠度 |
|------|---------|--------|
| A 重复 | 本次 text_set == 上次 text_set | 高 |
| B 尾标 | text 包含 "没有更多" / "到底了" | 高（若出现） |
| C 枯竭 | node 数不变 + 零新 text | 中 |

### 异常恢复

| 故障 | 症状 | 恢复 |
|------|------|------|
| ROOT_COMMAND_TIMEOUT | dump 超时 504 | restart app → intent 回 post detail → resume scroll |
| WS 断连 | 404 / 无响应 | restart app → 等 reconnect → dump 确认位置 → 继续 |
| 卡死 | phone app 无响应 | force-stop → restart → navigate back to post |
| scroll 不动 | 连续 2 次 scroll text 集合不变 | 判定到底，break |

**关键**：每次 scroll 前，先把当前屏已采集的 comments 写入 Mac daemon 落盘。
restart 后从落盘数据恢复，只从 scroll_count + 1 继续，不丢失已采集内容。

---

## 四、去重机制

### 4.1 单次运行去重

```python
seen = set()
for item in list_page:
    item_id = extract_item_id(item)  # 帖子标题hash / 帖子URL / 唯一文本指纹
    if item_id in seen:
        continue
    seen.add(item_id)
    process(item)
```

### 4.2 跨次运行去重（持久化）

```
dedup_store_path: {output_dir}/seen_items.jsonl

每处理完一个 item，append 一行：
{"item_id": "...", "title": "...", "url": "...", "processed_at": "..."}

下次运行 INIT 时：
1. 读取 seen_items.jsonl → 加载到内存 set
2. PICK_NEXT 时检查 item_id 是否在 set 中
3. 如果全部已处理 → scroll 加载新内容 → 再检查
4. 如果 scroll 3 次无新内容 → CHECK_CONTINUE 判定列表到底
```

### 4.3 item_id 生成策略

不同 APP 用不同字段，由 profile 定义：

| APP | item_id 来源 | 说明 |
|-----|-------------|------|
| 小红书 | 帖子标题 text hash | resourceId 被混淆不可用 |
| 微博 | 微博 URL / mid | 文本截取前 50 字做 backup |
| 通用 fallback | title[:50] + author | 双字段组合 hash |

### 4.4 去重的边界条件

- **同一帖子出现在不同搜索结果**：用 item_id 全局去重，不会重复处理
- **列表刷新后 item 顺序变化**：用内容 hash 而非 position index
- **title 太短或相同**：fallback 到 title + author + publish_time 组合
- **多次重入（app crash / 重��）**：dedup_store 是文件落盘，不怕进程丢失

---

## 五、错误回退机制

### 5.1 分级错误策略

```
┌─────────────────────────────────────────────┐
│ Level 1: 操作级错误（单步失败）              │
│ → 当前步骤 retry，最多 3 次                  │
│ → 3 次都失败 → 跳过当前 detail，回退列表     │
├─────────────────────────────────────────────┤
│ Level 2: 页面级错误（进入/退出详情失败）      │
│ → 回退列表，标记当前 item 为 failed          │
│ → 继续下一个 item                            │
├─────────────────────────────────────────────┤
│ Level 3: 列表级错误（无法获取列表内容）       │
│ → restart app → 重新进入 LIST_ENTRY          │
│ → 3 次 restart 仍失败 → abort 当前 run       │
├─────────────────────────────────────────────┤
│ Level 4: 系统级错误（daemon 断连 / 设备离线） │
│ → 等待 reconnect                             │
│ → 超时 → abort，保存进度，下次 resume         │
└─────────────────────────────────────────────┘
```

### 5.2 错误记录

每次错误写入 error_log：

```json
{
  "state": "ENTER_DETAIL",
  "item_id": "abc123",
  "error": "ANCHOR_MISMATCH",
  "retry_count": 3,
  "action": "skip_item",
  "timestamp": "...",
  "snapshot": "artifacts/..."
}
```

### 5.3 不高频重试原则

- 每次 retry 之间间隔 random(1.0, 2.5) 秒
- 同一步骤最多 3 次
- 同一 detail 最多 3 次（含子步骤累计）
- 超过即跳过，不阻塞后续 item

---

## 六、配置模型

### 6.1 TaskConfig

```json
{
  "schemaVersion": "flowy-task-config-v1",
  "taskId": "xhs_search_collect_v1",
  "appId": "xhs",
  "entry": {
    "mode": "search",
    "keyword": "deepseek v4",
    "keyword_from": "config"
  },
  "target_count": 20,
  "detail_task": {
    "phases": ["snapshot", "comments"],
    "comments": {
      "enabled": true,
      "mode": "collect_all",
      "keyword": null,
      "max_scrolls": 50
    },
    "interactions": {
      "like_post": false,
      "fav_post": false,
      "like_comments_matching": null
    }
  },
  "output": {
    "format": "jsonl",
    "path": "output/xhs-deepseek-v4/"
  },
  "dedup": {
    "enabled": true,
    "store_path": "output/xhs-deepseek-v4/seen_items.jsonl"
  }
}
```

### 6.2 AppProfile（APP 级别，不随 task 变化）

```json
{
  "schemaVersion": "flowy-app-profile-v1",
  "appId": "xhs",
  "entry_points": {
    "main": "am start -n com.xingin.xhs/.index.v2.IndexActivityV2"
  },
  "pages": {
    "search_input": {
      "activity": "GlobalSearchActivity",
      "anchor": "EditText.editable=true"
    },
    "search_result": {
      "activity": "GlobalSearchActivity",
      "anchor": "RecyclerView.scrollable=true"
    },
    "post_detail": {
      "activity": "NoteDetailActivity",
      "anchor": "Button cd contains '点赞'"
    },
    "comment_panel": {
      "anchor": "TextView text contains '条评论'"
    }
  },
  "selectors": {
    "list_item_filter": {
      "className": "TextView",
      "clickable": true,
      "parent_scrollable": true,
      "bounds_min_height": 200
    },
    "detail_fields": {
      "title": "TextView index near body",
      "body": "TextView length > 100",
      "author": "resourceId nickNameTV",
      "like_count": "cd.startsWith('点赞')",
      "fav_count": "cd.startsWith('收藏')",
      "comment_count": "cd.startsWith('评论')"
    },
    "comment_fields": {
      "username": "TextView clickable in comment_area",
      "content": "TextView text.length > 5 in comment_area",
      "like_button": "LinearLayout(clickable) > ImageView in comment_area",
      "expand_replies": "text.startsWith('展开')"
    }
  },
  "id_strategy": {
    "item_id": "title_hash",
    "fallback": "title[:50] + author"
  },
  "backend": "root",
  "timing": {
    "operation_interval_ms": [800, 2000],
    "anchor_poll_interval_ms": [800, 1500],
    "anchor_poll_max_retries": 3
  }
}
```

---

## 七、结果输出结构

### 7.1 单帖输出

```json
{
  "item_id": "hash_of_title",
  "app": "xhs",
  "entry_mode": "search",
  "keyword": "deepseek v4",
  "collected_at": "2026-04-27T14:00:00+08:00",
  "detail": {
    "title": "...",
    "author": "...",
    "body": "...",
    "images": ["url1", "url2"],
    "likes": 522,
    "is_liked": false,
    "favorites": 983,
    "is_favorited": false,
    "is_followed": false
  },
  "comments": [
    {
      "username": "六水",
      "content": "老师你好 问一下...",
      "time": "4小时前",
      "region": "湖北",
      "is_author": false,
      "liked": false,
      "replies": [
        {
          "username": "库森说AI",
          "content": "...",
          "is_author": true
        }
      ]
    }
  ],
  "error_log": []
}
```

### 7.2 Run 报告

```json
{
  "run_id": "2026-04-27T14-00-00_xhs-search-deepseek",
  "task_config": {...},
  "app_profile": "xhs",
  "started_at": "...",
  "finished_at": "...",
  "stats": {
    "target_count": 20,
    "completed": 18,
    "skipped_duplicates": 5,
    "failed": 2,
    "total_scrolls": 47,
    "total_operations": 234
  },
  "errors": [...],
  "output_files": ["item_001.jsonl", "item_002.jsonl", ...]
}
```

---

## 八、实现约束

| 约束 | 要求 |
|------|------|
| 操作间隔 | 随机 [800, 2000]ms，禁止固定间隔 |
| 单步 retry | 最多 3 次 |
| 单 detail retry | 累计最多 3 次（跨步骤） |
| 锚点轮询 | random jitter 3 次 |
| scroll 间隔 | random [0.8, 1.5]s |
| 去重 | 文件落盘 + 内存 set 双层 |
| 结果持久化 | 每完成一个 item 立即写入，不攒批 |
| 操作 backend | 默认 root (XHS)；profile 可配置 |
| 绝对坐标 | 生产环境禁止使用 |
| JS/Auto.js | 业务 APP 内禁止使用 |
| 500行限制 | 适用所有代码文件 |

---

## 九、Review 补丁（v1.1 闭环修复）

以下是对 v1 骨架 review 发现的 18 个缺口的修复。

### 9.1 流程闭环修复

#### P1: BACK_TO_LIST 失败后重入路径

BACK_TO_LIST 的 on_failure 不能只写 "launch_app\(intent\)"，必须携带**入口上下文**：

```json
{
  "state": "BACK_TO_LIST",
  "on_failure": {
    "tried_3_times": {
      "action": "rebuild_entry_context",
      "search_mode": {
        "steps": [
          "launch_app\(intent\)",
          "wait_anchor\(search_input\)",
          "input_text\(saved_keyword\)",
          "press_key\(enter\)",
          "wait_anchor\(search_result\)",
          "scroll_to_restore_position\(saved_scroll_index\)"
        ]
      },
      "timeline_mode": {
        "steps": [
          "launch_app\(intent\)",
          "wait_anchor\(feed\)",
          "scroll_to_restore_position\(saved_scroll_index\)"
        ]
      }
    }
  }
}
```

要求：每次 LIST_ENTRY 成功后，必须保存 `entry_mode`、`keyword`、`current_scroll_index` 到 checkpoint。
resume 时用这些字段重建入口上下文。

#### P2: Checkpoint & Resume

checkpoint 结构（每完成一个 item 或每次状态转换时写入）：

```json
{
  "schemaVersion": "flowy-checkpoint-v1",
  "run_id": "...",
  "timestamp": "...",
  "current_state": "DETAIL_TASK",
  "entry_context": {
    "mode": "search",
    "keyword": "deepseek v4",
    "app": "xhs"
  },
  "list_position": {
    "scroll_index": 5,
    "visible_item_ids": ["id1", "id2", "id3"]
  },
  "current_item_id": "abc123",
  "current_detail_progress": {
    "snapshot": "done",
    "interact": "done",
    "comments": "in_progress",
    "comment_scroll_count": 7,
    "partial_comments_file": "output/.../abc123_comments_partial.jsonl"
  },
  "completed_item_ids": ["id0", "id1"],
  "completed_count": 2,
  "target_count": 20,
  "error_count": 0
}
```

resume 状态机入口：

```
INIT:
  if checkpoint_exists:
    load_checkpoint\(\)
    if checkpoint.current_state == "DETAIL_TASK":
      → DETAIL_TASK \(从 current_detail_progress 继续\)
    elif checkpoint.current_state == "BACK_TO_LIST":
      → BACK_TO_LIST
    else:
      → PICK_NEXT
  else:
    → normal INIT → LIST_ENTRY
```

#### P3: DETAIL_TASK 增量写入

每个 phase 完成后**立即追加**到 item 文件，不攒批：

```
Phase snapshot done   → append {detail: {...}} to item file
Phase interact done   → append {interactions: {...}} to item file
Phase comments done   → append {comments: [...]} to item file
Phase all done        → write item_finalized marker
```

如果 comments 失败但 snapshot 成功，item 文件仍然保存已采集的部分，`error_log` 记录 comments 失败原因。
这保证即使 skip，已采集的数据不丢失。

#### P4: 子回复展开

在评论滚动采集子流程中，遇到 `展开 N 条回复` 时：

```
if config.expand_replies:
    for each "展开 N 条回复" node:
        tap\(expand_node\)
        wait_anchor\(sub_replies_visible\)  # 等新的 reply node 出现
        dump_sub_replies\(\)
        append to parent comment's replies[]
        sleep\(random 0.5-1.0\)
```

注意：展开子回复会**改变当前屏 node 布局**，展开后需要重新 dump 一次才能拿到子回复内容。
展开后继续 scroll 时，已展开的子回复会折叠回去（XHS 行为），需要在展开时立即采集。

#### P5: LIST_ENTRY 输入失败处理

```json
{
  "state": "LIST_ENTRY",
  "input_text_failure": {
    "retry": 3,
    "strategies": [
      "clipboard_paste\(root_set_clipboard + paste\)",
      "input_text_ascii_only",
      "restart_keyboard_and_retry"
    ],
    "on_failure": "abort"
  }
}
```

### 9.2 错误闭环修复

#### P6: 进程级故障（phone app crash）

增加独立于操作级错误的进程级故障路径：

```json
{
  "level": "PROCESS_CRASH",
  "detection": "WS 断连 + adb dumpsys 确认 app 不存在",
  "recovery": [
    "force-stop com.flowy.explore",
    "am start com.flowy.explore/.ui.DevPanelActivity",
    "wait_reconnect\(8s\)",
    "load_checkpoint\(\)",
    "resume_from_checkpoint\(\)"
  ],
  "max_recovery_per_run": 5,
  "on_exceed": "abort"
}
```

#### P7: Run 级超时

```json
{
  "run_config": {
    "max_run_duration_ms": 3600000,  // 1 小时
    "max_per_detail_duration_ms": 300000,  // 5 分钟
    "on_run_timeout": "finalize current item → write checkpoint → DONE",
    "on_detail_timeout": "skip item → BACK_TO_LIST"
  }
}
```

#### P8: 列表位置恢复

每次 PICK_NEXT 成功选择 item 时，记录 `scroll_index`（从第几次 scroll 找到这个 item）。
resume 时：

```
1. 进入 LIST_ENTRY（重建搜索/时间线）
2. scroll forward scroll_index 次（快速滚动到之前的位置附近）
3. 从该位置开始 observe → filter → dedup → pick next
```

注意：不需要精确到同一位置，只需要大致恢复到之前的浏览深度。

#### P9: 独立 scroll 计数器

PICK_NEXT 和 DETAIL_TASK\(comments\) 的 scroll 必须独立计数：

```json
{
  "scroll_counters": {
    "pick_next": {
      "current": 0,
      "max": 3,
      "reset_on_new_item_found": true
    },
    "comment_scroll": {
      "current": 0,
      "max": "config.detail_task.comments.max_scrolls",
      "reset_on_entering_new_detail": true
    }
  }
}
```

#### P10: Graceful Stop

```json
{
  "stop_mechanism": {
    "signal_source": "Mac daemon WS command / phone overlay UI button",
    "behavior": "finish_current_step → write_current_item → write_checkpoint → DONE",
    "force_stop": "finish_current_step → force_stop → DONE \(discard uncommitted\)"
  }
}
```

### 9.3 资源/传输闭环修复

#### P11: 采集���果传输路径

明确传输链路：

```
phone daemon
  → 采集数据生成 JSON
  → 通过 WS response 的 artifacts 字段上传到 Mac daemon
  → Mac daemon 落盘到 output_dir
```

具体：每个 block 执行后，结果作为 WS response 返回。Mac daemon 的 `phase-a-send-command.sh` 已经会自动保存 artifacts。
因此**传输链路是复用现有的 WS command dispatch**，不需要新的传输通道。

但需要在 TaskConfig 里明确：

```json
{
  "output": {
    "sink": "mac_daemon_via_ws",
    "path": "output/xhs-deepseek-v4/",
    "incremental": true
  }
}
```

#### P12: 评论 scroll 每次增量写入

每个 scroll + dump 周期完成后：

1. 提取新评论 → 构建 partial JSON
2. 通过 WS response artifact 上传到 Mac daemon
3. Mac daemon append 到 item 文件的 comments 部分
4. checkpoint 更新 comment_scroll_count

这样即使 phone crash，已采集的评论不会丢。

#### P13: 截图集成

截图作为**证据**（不是主采集手段）集成到 workflow：

```json
{
  "evidence_policy": {
    "screenshot_on_error": true,
    "screenshot_on_enter_detail": false,
    "screenshot_on_comment_hit": true,
    "screenshot_storage": "mac_daemon_artifacts/"
  }
}
```

截图不替代 dump-ui-tree 作为数据源，只作为 debug 证据。

#### P14: Phone 端内存约束

```
phone 端内存规则：
- dump-ui-tree 结果不在 phone 端保留副本
- 采集数据构建为 JSON 后立即通过 WS 发送
- phone 端不持久化任何采集数据
- checkpoint 文件存在 Mac daemon 侧，不在 phone
```

#### P15: 屏幕/电池保护

```json
{
  "device_policy": {
    "keep_screen_on": true,
    "acquire_wake_lock": true,
    "disable_auto_lock": true,
    "implementation": "WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON"
  }
}
```

#### P16: WS 连接健康检查

在每个大状态转换前做 ping：

```
LIST_ENTRY → PICK_NEXT:   ping, fail → reconnect → resume
PICK_NEXT → ENTER_DETAIL: ping, fail → reconnect → resume
DETAIL_TASK → BACK_TO_LIST: ping, fail → reconnect → resume
BACK_TO_LIST → CHECK_CONTINUE: ping, fail → reconnect → resume
```

ping 失败的处理：

```json
{
  "ws_health_check": {
    "on_state_transition": true,
    "timeout_ms": 5000,
    "on_fail": "wait 8s → retry ping → if still fail → restart phone app → reconnect → load checkpoint"
  }
}
```

#### P17: 图片采集修正

当前 dump-ui-tree 只能拿到 `desc="图片,第1张,共16张"`，**拿不到实际 URL**。

修正 output 结构：

```json
{
  "detail": {
    "image_count": 16,
    "image_descriptions": ["图片,第1张,共16张", ...],
    "image_urls": null,
    "image_url_note": "需要截图+OCR或网络层拦截才能获取实际URL"
  }
}
```

如果 TaskConfig 中 `enable_image_capture: true`，则额外流程：

```
1. 记录当前图片 desc（第几张）
2. 滑动图片 gallery
3. screenshot 每一张
4. 截图作为 artifact 上传到 Mac daemon
```

#### P18: Resume 完整定义

INIT 阶段增加 checkpoint 检测：

```json
{
  "state": "INIT",
  "actions": [
    "check_checkpoint_exists\(output_dir\)",
    "if exists: load_checkpoint\(\)",
    "if checkpoint.current_item_id is not null: mark as failed_partial → append to error_log",
    "load_app_profile\(checkpoint.app_id or config.app_id\)",
    "init_dedup_store\(\)",
    "init_result_sink\(\)"
  ],
  "next_state": {
    "has_checkpoint": "LIST_ENTRY \(with saved entry_context\)",
    "no_checkpoint": "LIST_ENTRY \(fresh\)"
  }
}
```

resume 时的决策：

```
if checkpoint exists:
  1. 加载已完成 item 到 dedup set（从 completed_item_ids）
  2. 如果 current_item 有 partial 数据：标记为 failed_partial，已采集部分保存
  3. 重建 entry_context（mode + keyword）
  4. 进入 LIST_ENTRY → scroll 到 saved position → PICK_NEXT
  5. 从 checkpoint.completed_count 继续计数
```

---

## 十、最终状态机全景（含修复）

```
                    ┌──────────────────────┐
                    │       INIT           │
                    │  加载 config/profile │
                    │  检查 checkpoint?    │
                    │  → resume or fresh   │
                    └─────────┬────────────┘
                              │
                    ┌─────────▼────────────┐
             ┌─────│    LIST_ENTRY         │
             │     │  搜索/时间线          │
             │     │  \(含 keyword +        │
             │     │   scroll position\)    │
             │     │  [P16: ws_health]     │
             │     └─────────┬────────────┘
             │               │
             │     ┌─────────▼────────────┐
             │     │     PICK_NEXT        │
             │     │  dedup check         │
             │     │  scroll to find new  │
             │     │  [P9: independent    │
             │     │   scroll counter]    │
             │     │  [P16: ws_health]     │
             │     └─────────┬────────────┘
             │               │
             │     ┌─────────▼────────────┐
             │     │   ENTER_DETAIL       │
             │     │  tap + anchor poll   │
             │     │  [P16: ws_health]     │
             │     └─────────┬────────────┘
             │               │
             │     ┌─────────▼────────────┐
             │     │    DETAIL_TASK       │
             │     │  Phase: snapshot     │──── [P3: 增量写入]
             │     │  Phase: interact     │──── [P3: 增量写入]
             │     │  Phase: comments     │──── [P3+P12: 每scroll增量]
             │     │    ├─ scroll loop    │
             │     │    ├─ keyword match  │
             │     │    ├─ bottom detect  │
             │     │    └─ expand replies │──── [P4: 子回复展开]
             │     │  [P7: per-detail     │
             │     │   timeout 5min]      │
             │     │  [P15: keep screen]  │
             │     └─────────┬────────────┘
             │               │
             │     ┌─────────▼────────────┐
             │     │    BACK_TO_LIST      │
             │     │  [P16: ws_health]     │
             │     │  fail → rebuild      │
             │     │  entry context [P1]  │
             │     └─────────┬────────────┘
             │               │
             │     ┌─────────▼────────────┐
             │     │  CHECK_CONTINUE      │
             │     │  [P7: run timeout]   │
             │     └──┬──────────┬────────┘
             │        │yes      │no
             └────────┘          │
                         ┌───────▼────────┐
                         │     DONE       │
                         │  write report  │
                         └────────────────┘

        故障旁路:
        ┌──────────────────────────────────┐
        │ PROCESS_CRASH [P6]               │
        │ → restart app → load checkpoint  │
        │ → resume_from_checkpoint [P18]   │
        │ max 5 times per run              │
        ├──────────────────────────────────┤
        │ GRACEFUL_STOP [P10]              │
        │ → finish step → write checkpoint │
        │ → DONE                           │
        └──────────────────────────────────┘
```
