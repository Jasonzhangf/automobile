# XHS (小红书) 采集规格 v1

## 目标

定义小红书帖子的完整采集框架。

采集对象（按优先级）：

1. **正文** — 一次 dump 全拿
2. **图片** — 数量 + 逐张操作
3. **点赞** — 数量 + 已/未点赞状态识别 + 切换
4. **收藏** — 数量 + 已/未收藏状态识别 + 切换
5. **评论入口** — 定位 + 打开评论面板
6. **评论列表** — 逐页 dump + 滚动加载 + 保留原始层级
7. **评论点赞** — 单条评论的点赞状态 + 切换
8. **评论定位** — 按关键字搜索定位到特定评论

---

## 一、已验证的事实

### 1.1 dump 能力边界

| 能力 | 是否可用 | 证据 |
|------|---------|------|
| 正文全文 | ✅ 一次 dump | node text 614 chars，滚动前后完全一致 |
| 标题 | ✅ 一次 dump | TextView text="DeepSeek V4 接入Claude Code最全教程🔥" |
| 作者 | ✅ 一次 dump | TextView text="库森说AI" |
| 图片数量 | ✅ 一次 dump | desc="图片,第1张,共16张" |
| 点赞数 + 状态 | ✅ 一次 dump | desc="点赞 522", flags.selected 表示已/未 |
| 收藏数 + 状态 | ✅ 一次 dump | desc="收藏 910", flags.selected 表示已/未 |
| 评论数 | ✅ 一次 dump | desc="评论 111" |
| 评论列表 | ❌ 详情页 dump 无评论节点 | 滚动3次后仍无评论节点 |
| 评论内容 | ❌ 需点击评论按钮打开面板 | 打开后另做 dump |
| 评论点赞 | ❌ 需在评论面板内识别 | 待验证 |

### 1.2 resourceId 全被混淆

小红书所有 resourceId 都是 `com.xingin.xhs:id/0_resource_name_obfuscated`。
定位只能靠：className + text + contentDescription + bounds + flags 组合。

### 1.3 点赞状态判断

- desc 格式：`"点赞 522"` / `"收藏 910"` / `"评论 111"`
- 未点赞：`flags.selected = false`
- 已点赞：`flags.selected = true`（需点赞后验证）
- 备选：点击后重新 dump 对比 desc 中数字变化

---

## 二、页面模型

### 2.1 页面类型

| 页面 | Activity | 识别锚点 |
|------|----------|---------|
| `xhs_search_input` | `GlobalSearchActivity` | EditText editable=true, Button text="搜索" |
| `xhs_search_result` | `GlobalSearchActivity` | HorizontalScrollView (tabs), RecyclerView (cards) |
| `xhs_post_detail` | `NoteDetailActivity` | 底栏 desc="点赞 N", desc="评论 N" |
| `xhs_comment_panel` | `NoteDetailActivity`（同 activity） | 评论列表 RecyclerView + 评论节点出现 |
| `xhs_blocker` | 未知 | 风控/登录页 |

### 2.2 详情页节点拓扑（62 nodes）

```
[0-13]  根节点 + 头部
[14]    图片画廊 FrameLayout (desc="图片,第X张,共Y张")
[16]      RecyclerView (scrollable, 图片滑动)
[33]    标题 TextView
[34]    正文 TextView
[37]    作者头像 ImageView (clickable)
[39]    作者信息区 LinearLayout (clickable)
[45]    作者名 TextView
[46]    关注按钮 FrameLayout (clickable)
[47]      text="关注" (未关注) / text="已关注" (已关注)
[48]    更多菜单 ImageView (clickable)
[51]    底部操作栏 ViewGroup
[52]      评论框 TextView (desc="评论框")
[53]      点赞 Button (desc="点赞 N")
[54]        ImageView (图标)
[55]        TextView (数量)
[56]      收藏 Button (desc="收藏 N")
[57]        ImageView (图标)
[58]        TextView (数量)
[59]      评论 Button (desc="评论 N")  ← 评论入口
[60]        ImageView (图标)
[61]        TextView (数量)
```

---

## 三、采集阶段设计

### 3.1 整体流程

```text
Phase 1: 详情页一次性采集（正文 + 图片元数据 + 互动数据）
Phase 2: 图片逐张采集（可选）
Phase 3: 点赞操作（可选）
Phase 4: 打开评论面板
Phase 5: 评论逐页采集（循环 dump + scroll）
Phase 6: 评论互动操作（评论点赞 / 评论搜索定位）
Phase 7: 返回
```

### 3.2 Phase 1 — 详情页一次性采集

一次 dump-ui-tree-root 提取：

```json
{
  "phase": "detail_snapshot",
  "extracted": {
    "title": "DeepSeek V4 接入Claude Code最全教程🔥",
    "author": "库森说AI",
    "bodyText": "完整正文...",
    "publishTime": "昨天 17:29 浙江",
    "imageCount": 16,
    "likes": {
      "count": 522,
      "selected": false
    },
    "favorites": {
      "count": 910,
      "selected": false
    },
    "commentCount": 111,
    "followed": false
  }
}
```

**提取规则**：

| 字段 | 匹配方式 |
|------|---------|
| title | className=TextView, bounds.top 在图片区下方、正文区上方 |
| author | className=TextView, parent 可点击, 在头像右侧 |
| bodyText | className=TextView, text.length > 100, bounds.top > title.bottom |
| imageCount | 从 desc 正则 `共(\d+)张` 提取 |
| likes.count | 从 Button desc 正则 `点赞\s*(\d+)` 提取 |
| likes.selected | Button 的 flags.selected |
| favorites.count | 从 Button desc 正则 `收藏\s*(\d+)` 提取 |
| favorites.selected | Button 的 flags.selected |
| commentCount | 从 Button desc 正则 `评论\s*(\d+)` 提取 |
| followed | text="关注" → false, text="已关注" → true |

### 3.3 Phase 2 — 图片逐张采集

- 当前图片画廊是 RecyclerView [16]，可滚动
- 每张图片当前通过 `desc="图片,第X张,共Y张"` 标识
- 采集方式：每次左滑一张 → dump → 截图 → 记录
- 或只截图不 dump（图片是视觉内容，dump 无额外信息）

### 3.4 Phase 3 — 点赞操作

```text
observe → 判断 flags.selected → 决定是否点击
```

- tap 点赞按钮 center
- wait 随机 1-3 秒
- dump 验证 flags.selected 变化 + desc 数字变化
- 如果需要恢复：再次 tap 取消点赞

### 3.5 Phase 4 — 打开评论面板

- tap 评论按钮 center (desc="评论 N" 的 Button)
- wait 评论面板加载
- dump 验证评论节点出现
- **post-anchor**: 新增节点中包含评论内容（text 非空的 TextView 且不在原详情页节点集合中）

### 3.6 Phase 5 — 评论逐页采集

评论面板打开后，进入循环：

```text
while 未到采集上限 and 还有新评论:
    dump-ui-tree-root
    提取当前可见评论
    写入（推送到 Mac daemon）
    scroll down
    wait 新评论加载
```

**评论结构（保留原始层级）**：

```json
{
  "id": "comment_001",
  "author": "用户A",
  "text": "评论正文",
  "timeText": "2小时前",
  "likeCount": 15,
  "liked": false,
  "replies": [
    {
      "id": "reply_001",
      "author": "用户B",
      "text": "回复正文",
      "timeText": "1小时前",
      "likeCount": 3,
      "liked": false,
      "replies": [
        {
          "id": "reply_002",
          "author": "用户A",
          "text": "子回复正文",
          "timeText": "30分钟前",
          "likeCount": 0,
          "liked": false,
          "replies": []
        }
      ]
    }
  ]
}
```

**层级保留规则**：

- 主评论：直接在评论列表中的一级节点
- 回复（子评论）：内联展开在主评论下方
- 如果回复被折叠，需要 tap "展开N条回复" 来展开
- 层级关系通过 bounds.top 顺序 + 缩进量（bounds.left）判断
- 每条评论的 `replies[]` 是递归结构，深度不限

### 3.7 Phase 6 — 评论互动

#### 6a. 评论定位（搜索关键字）

小红书评论面板可能有搜索功能（待验证）。
如果没有内置搜索：

```text
循环 dump + scroll → 匹配 text.contains(keyword) → 定位到该评论
```

#### 6b. 评论点赞

- 单条评论的点赞按钮
- 匹配方式：评论节点内的 ImageView/Button，desc 包含数字
- 操作方式：同帖子点赞，tap → 验证 selected 变化

---

## 四、CommentNode 采集规格

### 4.1 单条评论 dump 结构（待验证评论面板的实际节点）

预期结构：

```
评论条目容器 (ViewGroup, click=True)
  ├── 头像 (ImageView)
  ├── 用户名 (TextView, text="用户A")
  ├── 评论正文 (TextView, text="评论内容")
  ├── 时间 (TextView, text="2小时前")
  ├── 点赞 (ImageView/Button, desc containing number)
  └── 回复区 (ViewGroup, optional)
        ├── "展开N条回复" (TextView, click=True, optional)
        └── 回复节点 (recursive)
```

### 4.2 去重策略

- 用 `(author, text, timeText)` 三元组作为去重 key
- 同一页 dump 不重复写入
- 跨页 dump 通过 text 首尾重叠检测跳过已采集条目

### 4.3 结束条件

- 到达采集上限（可配置，默认 = 评论总数）
- 连续 N 次 scroll 无新评论出现
- 到达 "没有更多评论" 提示

---

## 五、输出 Contract

### 5.1 单帖采集结果

```json
{
  "schemaVersion": "flowy-xhs-collection-v1",
  "source": {
    "app": "xhs",
    "keyword": "deepseek v4",
    "postIndex": 1
  },
  "post": {
    "title": "...",
    "author": "...",
    "bodyText": "...",
    "publishTime": "...",
    "imageCount": 16,
    "imageUrls": [],
    "likes": {
      "count": 522,
      "selected": false
    },
    "favorites": {
      "count": 910,
      "selected": false
    },
    "commentCount": 111,
    "followed": false
  },
  "comments": [],
  "interactions": {
    "liked": false,
    "favorited": false,
    "commentsLiked": []
  },
  "artifacts": {
    "screenshots": [],
    "uiTreeDumps": []
  },
  "collectedAt": "2026-04-27T12:30:00+08:00"
}
```

### 5.2 评论写入方式

- 每次 dump 后立即提取、立即推送
- Mac daemon 收到后 append 到该帖子的 comments[]
- 手机端不累积全量评论在内存中

---

## 六、Filter 定义（XHS Profile 层）

### 6.1 详情页 filter

```json
{
  "filterId": "xhs_detail_body",
  "match": {
    "className": "android.widget.TextView",
    "minTextLength": 100,
    "notDesc": true
  },
  "select": { "strategy": "longest_text" }
}
```

### 6.2 点赞按钮 filter

```json
{
  "filterId": "xhs_like_button",
  "match": {
    "className": "android.widget.Button",
    "descPattern": "^点赞\\s*\\d+"
  },
  "select": { "strategy": "first_best" }
}
```

### 6.3 评论入口 filter

```json
{
  "filterId": "xhs_comment_entry",
  "match": {
    "className": "android.widget.Button",
    "descPattern": "^评论\\s*\\d+"
  },
  "select": { "strategy": "first_best" }
}
```

### 6.4 评论条目 filter（待评论面板验证后补充）

```json
{
  "filterId": "xhs_comment_item",
  "match": {
    "className": "android.widget.TextView",
    "withinCommentPanel": true,
    "minTextLength": 1
  },
  "select": { "strategy": "all_visible" }
}
```

---

## 七、状态判断规则

| 状态 | 判断方式 | 备选 |
|------|---------|------|
| 已点赞帖子 | Button desc="点赞 N" 的 flags.selected=true | 点击后 desc 数字 +1 |
| 已收藏帖子 | Button desc="收藏 N" 的 flags.selected=true | 点击后 desc 数字 +1 |
| 已关注作者 | text="已关注" | text="关注" 表示未关注 |
| 已点赞评论 | 评论内点赞按钮 flags.selected=true | 待验证 |
| 评论面板已打开 | dump 中出现评论列表节点（不在原详情页集合中） | |

---

## 八、与通用骨架的映射

| 通用锚点 | XHS 实现 |
|---------|---------|
| `body_anchor` | className=TextView, minTextLength=100 |
| `media_gallery` | FrameLayout desc="图片,第X张,共Y张" |
| `like_entry` | Button desc="点赞 N" |
| `comment_entry` | Button desc="评论 N" |
| `comment_list` | 评论面板中的 RecyclerView |
| `comment_item` | 评论面板中的单条评论容器 |
| `follow_entry` | FrameLayout containing text="关注"/"已关注" |

---

## 九、实现顺序

1. **Phase 1 详情页快照** — 最高优先级，一次 dump 全拿
2. **Phase 4+5 评论采集** — 打开面板 + 逐页 dump
3. **Phase 3 点赞操作** — tap + 状态验证
4. **Phase 6a 评论定位** — scroll + text 匹配
5. **Phase 6b 评论点赞** — 评论内点赞按钮
6. **Phase 2 图片逐张** — 低优先级

---

## 十、待验证项

- [ ] 评论面板的实际节点结构（打开评论按钮后 dump）
- [ ] 已点赞时 flags.selected 是否为 true
- [ ] 评论是否有展开回复的入口
- [ ] 评论面板中是否有搜索功能
- [ ] 评论点赞的节点结构
- [ ] 长正文帖子是否真的不截断（>2000字的帖子）
- [ ] 回复折叠/展开的节点变化

