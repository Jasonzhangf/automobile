# App Exploration Template Spec v1

## 目标

定义 Flowy 在第三方内容 APP 上进行**页面探索 / step 标注 / workflow 起草**时的统一标准 spec。

这份 spec 不等于某个 APP 的正式业务 flow。

它回答的是：

1. 探索一个 APP 时，哪些对象必须先定义
2. 一个页面探索 step 的标准输入/输出是什么
3. 哪些内容属于通用骨架，哪些内容属于 APP profile
4. 探索证据怎样落盘，才能被后续 workflow 编程复用

一句话：

```text
先用统一探索模板沉淀页面 / 锚点 / step / 字段 / 证据
再从模板生成正式 workflow
```

---

## 一、适用范围

适用于：

- 小红书
- 微博
- 抖音类信息流
- 任何“列表 -> 详情 -> 评论/互动”形态的内容 APP

不适用于：

- 系统设置页这类结构稳定、无需业务 profile 的简单页面
- 已经完成稳定 workflow 建模、只剩执行细节的模块

---

## 二、模板分层

探索模板固定拆成 4 层：

```text
app meta
-> page catalog
-> exploration steps
-> collection/output schema
```

### 1. app meta

描述 APP 级边界：

- appId
- packageName
- entry policy
- blocker policy
- timing policy
- evidence policy

### 2. page catalog

描述页面级对象：

- 页面名
- 页面识别信号
- 关键锚点
- 允许的转移方向

### 3. exploration steps

描述“怎么从一个页面走到下一个页面，并确认成功”：

- observerSpec
- targetFilter
- operationPlan
- preAnchor
- postAnchor
- settle / retry policy

### 4. collection / output schema

描述后续真正采集时想拿到什么：

- post fields
- media fields
- comment fields
- interaction entry points
- evidence bundle

---

## 三、统一对象模型

## 1. AppMeta

建议结构：

```json
{
  "appId": "xhs",
  "displayName": "小红书",
  "packageName": "com.xingin.xhs",
  "entryPolicy": {
    "allowed": ["home_icon_tap", "manual_foreground_confirm"],
    "forbidden": ["intent", "deep-link", "component"]
  },
  "timingPolicy": {
    "tap": { "minDelayMs": 700, "maxDelayMs": 1400 },
    "scroll": { "minDelayMs": 900, "maxDelayMs": 1800 },
    "back": { "minDelayMs": 600, "maxDelayMs": 1200 }
  },
  "evidencePolicy": {
    "preferAccessibility": true,
    "preferScreenshot": true,
    "requireRunArtifacts": true
  }
}
```

规则：

- 第三方业务 APP 的 `entryPolicy` 必须明确禁止：
  - `intent`
  - `deep-link`
  - `component`
- timing policy 必须存在，不能留给脚本里临时 sleep

---

## 2. PageSpec

每个页面最少要定义：

```json
{
  "pageId": "search_result",
  "pageName": "搜索结果页",
  "identify": {
    "packageName": "com.xingin.xhs",
    "mustContainTexts": ["综合", "最新"],
    "mustContainAnchors": ["result_list", "result_card"]
  },
  "anchors": ["result_list", "result_card", "detail_back", "like_entry"],
  "allowedTransitions": ["post_detail", "blocker"]
}
```

规则：

- 页面名只用于人读，不是真源
- 真源是：
  - packageName
  - 页面文本
  - 页面锚点
  - 页面转移规则

---

## 3. AnchorSpec

Anchor 统一按抽象名定义，不直接使用 APP 文案名作为骨架真源。

建议分类：

### 页面推进锚点
- `search_entry`
- `search_input`
- `search_submit`
- `result_list`
- `result_card`
- `detail_back`

### 内容锚点
- `author_anchor`
- `title_anchor`
- `body_anchor`
- `media_gallery`
- `comment_list`
- `comment_item`

### 互动锚点
- `like_entry`
- `comment_entry`
- `reply_entry`
- `share_entry`
- `follow_entry`

### 例外锚点
- `blocker_close`
- `login_gate`
- `permission_gate`
- `upgrade_gate`

每个 anchor 在 APP spec 里再补具体匹配方式：

```json
{
  "anchorId": "result_card",
  "match": {
    "selector": {
      "clickable": true,
      "textContains": "赞"
    }
  },
  "notes": "这里只是例子，实际以页面证据修正"
}
```

---

## 4. ExplorationStepSpec

这是探索模板里最核心的对象。

建议结构：

```json
{
  "stepId": "open_next_result_card",
  "fromPage": "search_result",
  "toPage": "post_detail",
  "intent": "从结果列表进入下一篇帖子详情",
  "observerSpec": {
    "requireAccessibility": true,
    "requireScreenshot": false
  },
  "targetFilter": {
    "anchorId": "result_card",
    "selector": {
      "clickable": true
    }
  },
  "selectionPolicy": {
    "mode": "first_unvisited_visible"
  },
  "operationPlan": {
    "kind": "tap",
    "backend": "accessibility"
  },
  "anchorPolicy": {
    "preAnchor": {
      "packageName": "com.xingin.xhs",
      "mustContainTexts": ["综合", "最新"]
    },
    "postAnchor": {
      "packageName": "com.xingin.xhs",
      "mustContainAnchors": ["body_anchor", "comment_entry"]
    }
  },
  "postObservePolicy": {
    "maxAttempts": 8,
    "pollIntervalMs": 400
  },
  "failurePolicy": {
    "onTargetMissing": "scroll_and_retry",
    "onPostAnchorMiss": "capture_evidence_and_stop"
  }
}
```

---

## 四、探索 step 的标准字段

每个 step 必须回答 8 个问题：

1. 我当前在哪个 page
2. 我想去哪个 page
3. 我依赖哪个 anchor/filter
4. 我执行什么 operation
5. pre-anchor 怎样确认当前页没跑偏
6. post-anchor 怎样确认操作成功
7. 如果第一次 observe 没更新，要等多久、试几次
8. 失败后保留什么证据

如果 8 个问题回答不出来，这个 step 还不能进入正式 workflow。

---

## 五、页面探索输出结构

探索模板输出的不是“聊天总结”，而是结构化素材。

建议每个页面都沉淀：

### 1. Page summary

```json
{
  "pageId": "post_detail",
  "identifiedBy": ["author_anchor", "body_anchor", "comment_entry"],
  "risks": ["popup", "lazy_render", "reply_sheet"],
  "stableOperations": ["tap_like", "tap_comment_entry", "back_to_result"]
}
```

### 2. Field mapping

```json
{
  "post.author": "author_anchor",
  "post.bodyText": "body_anchor",
  "post.media[]": "media_gallery",
  "entry.like": "like_entry",
  "entry.comment": "comment_entry",
  "entry.reply": "reply_entry"
}
```

### 3. Evidence checklist

- accessibility dump 是否可识别主要文本
- screenshot 是否可补图片区
- 当前页 blocker 是否会遮挡
- 当前页 back 后回到哪里

---

## 六、探索阶段的证据门禁

每个探索 step 至少要拿到：

1. `response.json`
2. `page-context.json` 或等价页面事实
3. accessibility 或 root window state 证据
4. 成功/失败时的明确 post-anchor 结果

建议成功证据最少为：

```text
step request
-> operation response
-> post page observe evidence
-> anchor checked result
```

如果 post-anchor 失败，必须至少再补一项：

- `dump-window-state-root`
- 或 `capture-screenshot`

用于区分：

- 操作没发生
- 操作已发生但观察未刷新
- 页面跳到了 blocker

---

## 七、模板到正式 workflow 的提升条件

探索模板只有满足下面条件，才能提升为正式 workflow：

1. 页面目录稳定
2. 每个核心页面至少有一个稳定 identify spec
3. 关键 step 已有真机成功证据
4. blocker 分支已知
5. 字段映射已知
6. settle / retry policy 已知

如果这些还没齐，继续停留在 exploration template。

---

## 八、推荐文件落位

### 通用 spec
- `docs/architecture/app-exploration-template-spec.md`

### 某 APP 的探索模板
- `docs/experiments/<app>-exploration-template-v1.md`

### 某 APP 的正式业务 flow
- `docs/experiments/<app>-<goal>-flow-v1.md`

### 稳定经验
- `MEMORY.md`
- `AGENTS.md`

---

## 九、当前约束

- 第三方业务 APP 探索只允许：
  - `tap`
  - `scroll`
  - `input-text`
  - `back`
  - `press-key`
  - `capture-screenshot`
  - `dump-accessibility-tree`
- 不允许：
  - `intent`
  - `deep-link`
  - `component`
  - `JS / Auto.js`
- `post-anchor` 默认必须支持 bounded retry / poll，不能只做一次 observe。
