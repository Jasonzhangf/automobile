# XHS Exploration Template v1

## 目标

把小红书的页面探索沉淀成一套标准模板，供后续：

1. 页面识别
2. target 标注
3. workflow step 编排
4. 内容采集 flow

统一复用。

这份文档是：

- `docs/architecture/app-exploration-template-spec.md`

在小红书上的具体实例。

---

## 一、探索边界

当前模板只覆盖：

```text
首页
-> 搜索输入页
-> 搜索结果页
-> 帖子详情页
-> 评论展开页 / 回复输入态
-> 返回结果页
```

当前不覆盖：

- 登录流
- 发帖流
- 私信流
- 视频专用页
- OCR / vision 精细补洞

---

## 二、AppMeta

```json
{
  "appId": "xhs",
  "displayName": "小红书",
  "packageName": "com.xingin.xhs",
  "entryPolicy": {
    "allowed": ["manual_foreground_confirm", "desktop_icon_tap"],
    "forbidden": ["intent", "deep-link", "component"]
  },
  "timingPolicy": {
    "tap": { "minDelayMs": 700, "maxDelayMs": 1400 },
    "scroll": { "minDelayMs": 900, "maxDelayMs": 1800 },
    "back": { "minDelayMs": 600, "maxDelayMs": 1200 },
    "input": { "minDelayMs": 800, "maxDelayMs": 1500 }
  },
  "riskNotes": [
    "当前设备/账号上，Accessibility 开启态下，搜索结果页 -> 帖子详情页会触发风控",
    "因此 XHS 模板当前先定位为探索模板，不直接等同正式自动业务主链"
  ]
}
```

---

## 三、页面目录

## 1. `home_feed`

### 页面识别
- packageName = `com.xingin.xhs`
- 顶部出现：
  - `关注`
  - `发现`
  - `视频`
- 右上存在搜索入口

### 关键锚点
- `search_entry`
- `feed_card`
- `tab_discover`

### 允许转移
- `search_input`
- `post_detail`
- `blocker`

---

## 2. `search_input`

### 页面识别
- packageName = `com.xingin.xhs`
- 出现搜索输入框
- 常见辅助文本：
  - `猜你想搜`
  - `历史记录`
  - `问AI`

### 关键锚点
- `search_input`
- `search_submit`
- `search_history`

### 允许转移
- `search_result`
- `home_feed`
- `blocker`

---

## 3. `search_result`

### 页面识别
- packageName = `com.xingin.xhs`
- 当前关键词仍可见
- 出现结果列表
- 常见顶部筛选：
  - `综合`
  - `最新`
  - `今天`

### 关键锚点
- `result_list`
- `result_card`
- `detail_back`

### 允许转移
- `post_detail`
- `search_input`
- `blocker`

---

## 4. `post_detail`

### 页面识别
- packageName = `com.xingin.xhs`
- 出现作者区域
- 出现正文区域
- 出现图片区域或评论入口

### 关键锚点
- `author_anchor`
- `body_anchor`
- `media_gallery`
- `comment_entry`
- `like_entry`
- `reply_entry`
- `detail_back`

### 允许转移
- `comment_expanded`
- `reply_composer`
- `search_result`
- `blocker`

---

## 5. `comment_expanded`

### 页面识别
- packageName = `com.xingin.xhs`
- 评论区域明显展开
- 可见评论项列表
- 可见回复入口或输入入口

### 关键锚点
- `comment_list`
- `comment_item`
- `reply_entry`
- `like_entry`
- `detail_back`

### 允许转移
- `reply_composer`
- `post_detail`
- `blocker`

---

## 6. `reply_composer`

### 页面识别
- packageName = `com.xingin.xhs`
- 输入框文案类似：
  - `回复 @xxx`
- 出现 `发送`
- 常伴随输入法或表情栏

### 关键锚点
- `reply_input`
- `reply_submit`
- `detail_back`

### 允许转移
- `comment_expanded`
- `post_detail`
- `blocker`

---

## 7. `blocker`

### 页面识别
- 登录弹窗
- 权限弹窗
- 升级弹窗
- 风控页

### 关键锚点
- `blocker_close`
- `login_gate`
- `upgrade_gate`

---

## 四、统一锚点映射

## 页面推进锚点

- `search_entry`
  - 首页右上搜索入口
- `search_input`
  - 搜索输入框
- `search_submit`
  - 键盘搜索 / 提交按钮
- `result_list`
  - 搜索结果滚动容器
- `result_card`
  - 结果卡片
- `detail_back`
  - 详情页或评论页返回入口

## 内容锚点

- `author_anchor`
- `title_anchor`
- `body_anchor`
- `media_gallery`
- `comment_list`
- `comment_item`

## 互动锚点

- `like_entry`
- `comment_entry`
- `reply_entry`
- `share_entry`
- `follow_entry`

---

## 五、标准探索 step

## Step 1：从首页进入搜索页

### stepId
`xhs_open_search_entry`

### from -> to
`home_feed -> search_input`

### observerSpec

```json
{
  "requireAccessibility": true,
  "requireScreenshot": false
}
```

### targetFilter

```json
{
  "anchorId": "search_entry",
  "selector": {
    "clickable": true
  }
}
```

### operationPlan

```json
{
  "kind": "tap",
  "backend": "accessibility"
}
```

### preAnchor

```json
{
  "packageName": "com.xingin.xhs",
  "mustContainTexts": ["关注", "发现"]
}
```

### postAnchor

```json
{
  "packageName": "com.xingin.xhs",
  "mustContainTexts": ["猜你想搜"]
}
```

### notes
- 如果 post-anchor 没命中，要区分：
  - 搜索页没打开
  - 打开了但页面文本还没刷新
  - 出现 blocker

---

## Step 2：输入关键词

### stepId
`xhs_input_keyword`

### from -> to
`search_input -> search_input`

### targetFilter

```json
{
  "anchorId": "search_input",
  "selector": {
    "editable": true
  }
}
```

### operationPlan

```json
{
  "kind": "input-text",
  "backend": "accessibility",
  "payload": {
    "text": "deepseek v4"
  }
}
```

### preAnchor

```json
{
  "packageName": "com.xingin.xhs"
}
```

### postAnchor

```json
{
  "packageName": "com.xingin.xhs",
  "mustContainTexts": ["deepseek v4"]
}
```

---

## Step 3：提交搜索

### stepId
`xhs_submit_search`

### from -> to
`search_input -> search_result`

### operation candidates

优先顺序：

1. `press-key(search)`
2. `tap(search_submit)`

### preAnchor

```json
{
  "packageName": "com.xingin.xhs",
  "mustContainTexts": ["deepseek v4"]
}
```

### postAnchor

```json
{
  "packageName": "com.xingin.xhs",
  "mustContainTexts": ["综合", "最新"]
}
```

### settle

```json
{
  "maxAttempts": 8,
  "pollIntervalMs": 400
}
```

---

## Step 4：结果页选择下一条卡片

### stepId
`xhs_open_next_result_card`

### from -> to
`search_result -> post_detail`

### targetFilter

```json
{
  "anchorId": "result_card",
  "selector": {
    "clickable": true
  }
}
```

### selectionPolicy

```json
{
  "mode": "first_unvisited_visible",
  "fallback": "scroll_result_list_and_retry"
}
```

### preAnchor

```json
{
  "packageName": "com.xingin.xhs",
  "mustContainTexts": ["综合", "最新"]
}
```

### postAnchor

```json
{
  "packageName": "com.xingin.xhs",
  "mustContainAnyTexts": ["赞", "评论", "收藏"]
}
```

### 风险说明
- 当前设备/账号上，这一步在 Accessibility 开启态下有风控风险
- 所以后续正式自动化要把它标记为：
  - `riskSensitive = true`
  - 当前仅做探索模板与人工辅助链路

---

## Step 5：详情页采集正文/图片/评论入口

### stepId
`xhs_collect_detail_overview`

### from -> to
`post_detail -> post_detail`

### 目标
- 识别：
  - 正文
  - 图片区
  - 评论入口
  - 点赞入口
  - 回复入口

### 输出字段

```json
{
  "post.author": "author_anchor",
  "post.bodyText": "body_anchor",
  "post.media[]": "media_gallery",
  "entry.comment": "comment_entry",
  "entry.like": "like_entry",
  "entry.reply": "reply_entry"
}
```

---

## Step 6：进入评论展开态

### stepId
`xhs_open_comment_expanded`

### from -> to
`post_detail -> comment_expanded`

### postAnchor

```json
{
  "packageName": "com.xingin.xhs",
  "mustContainAnyTexts": ["评论", "回复"]
}
```

### 分支说明
- 这里允许：
  - 内联展开
  - 独立评论层
  - 直接进入回复输入态

所以探索时必须记录：
- 实际进入哪个 page
- 对应 post-anchor 是什么

---

## Step 7：评论列表采集

### stepId
`xhs_collect_comment_list`

### from -> to
`comment_expanded -> comment_expanded`

### 目标字段

```json
{
  "comment.author": "comment_item.author",
  "comment.bodyText": "comment_item.body",
  "comment.likeEntry": "comment_item.like_entry",
  "comment.replyEntry": "comment_item.reply_entry",
  "comment.replies[]": "comment_item.replies"
}
```

### 备注
- 当前阶段先做：
  - 评论项识别
  - 子回复入口识别
- 暂不要求一次性刷完整个评论宇宙

---

## Step 8：返回结果页

### stepId
`xhs_back_to_result_list`

### from -> to
`post_detail -> search_result`

### operationPlan

```json
{
  "kind": "back",
  "backend": "accessibility"
}
```

### postAnchor

```json
{
  "packageName": "com.xingin.xhs",
  "mustContainTexts": ["综合", "最新"]
}
```

### settle

```json
{
  "maxAttempts": 8,
  "pollIntervalMs": 400
}
```

---

## 六、字段输出目标

## 1. post

```json
{
  "postId": "",
  "author": "",
  "title": "",
  "bodyText": "",
  "media": [],
  "likeState": "unknown",
  "commentCountText": "",
  "entries": {
    "like": {},
    "comment": {},
    "reply": {}
  }
}
```

## 2. comment

```json
{
  "author": "",
  "bodyText": "",
  "liked": "unknown",
  "replyEntry": {},
  "children": []
}
```

---

## 七、探索门禁

小红书探索模板当前的最小 gate：

### Gate A：页面识别
- `home_feed`
- `search_input`
- `search_result`
- `post_detail`

### Gate B：关键 step
- `xhs_open_search_entry`
- `xhs_input_keyword`
- `xhs_submit_search`
- `xhs_back_to_result_list`

### Gate C：详情识别
- `like_entry`
- `comment_entry`
- `reply_entry`
- `body_anchor`
- `media_gallery`

### Gate D：评论识别
- `comment_item`
- `comment.reply_entry`
- `comment.like_entry`

---

## 八、当前限制

- `search_result -> post_detail` 在当前设备/账号组合下属于风险敏感链路
- 因此这份模板现在是：
  - **探索模板**
  - **字段与 step 模板真源**
  - 不是“可放心全自动执行”的正式业务 flow 真源

正式自动业务 flow 要等：

1. 该链路风险处理策略明确
2. 点赞/评论/正文/图片提取都有稳定证据
3. accessibility / root / screenshot 的分工明确

---

## 九、可执行 payload 模板

当前第一批可执行模板已落到：

- `packages/regression-fixtures/xhs/workflow-steps/`

文件：

1. `step-01-open-search-entry.payload.json`
2. `step-02-input-keyword.payload.json`
3. `step-03-submit-search.payload.json`
4. `step-04-open-next-result-card.payload.json`
5. `step-05-open-comment-expanded.payload.json`
6. `step-06-back-to-result-list.payload.json`

直接发送方式：

```bash
./scripts/dev/phase-a-send-workflow-step.sh \
  packages/regression-fixtures/xhs/workflow-steps/step-01-open-search-entry.payload.json \
  xhs-open-search-entry
```

说明：

- 这些 payload 是 **探索模板执行输入真源**
- 不是“已经业务稳定”的保证
- `step-04-open-next-result-card` 仍保留风险标记，当前只建议在人工看护或探索环境下使用
