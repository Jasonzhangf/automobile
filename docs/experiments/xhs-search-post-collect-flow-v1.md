# XHS Search Post Collect Flow v1

## 目标

定义 Flowy 在小红书上的第一条可复用自动化 flow：

```text
搜索关键词
-> 打开搜索结果列表
-> 进入帖子详情
-> 采集图片 / 正文 / 评论
-> 返回结果列表
-> 继续下一条
```

当前关键词示例：

- `deepseek v4`

---

## 一、Flow 边界

这条 flow 当前只定义：

1. 页面序列
2. 每步 observe / filter / anchor / operation
3. 采集结果结构
4. blocker 处理规则

当前不在本版 flow 内展开：

- 去重存储实现细节
- 文档导出格式
- OCR / vision 补洞细节
- 评论多页深翻策略优化

---

## 二、页面序列

### 1. `xhs_home_feed_page`

识别信号：

- 顶部 `关注 / 发现 / 视频`
- 右上搜索入口

目标：

- 从首页进入搜索页

### 2. `xhs_search_input_page`

识别信号：

- 搜索输入框
- 搜索提交入口
- 历史记录 / 猜你想搜 / 问AI

目标：

- 输入关键词并提交搜索

### 3. `xhs_search_result_page`

识别信号：

- 当前关键词
- 结果列表 scroll container
- 结果卡片

目标：

- 维护未访问帖子队列
- 打开下一条帖子

### 4. `xhs_post_detail_page`

识别信号：

- 正文区域
- 作者区域
- 图片区域
- 评论入口或评论列表

目标：

- 采集正文
- 采集图片
- 采集评论

### 5. `xhs_comment_expanded_page`

识别信号：

- 评论标题
- 评论输入框
- 评论列表 / 回复列表

目标：

- 滚动采集评论
- 判断评论流结束

### 6. `xhs_blocker_page`

识别信号：

- 登录弹窗
- 权限弹窗
- 升级弹窗
- 广告 / 遮挡层

目标：

- 消障后回主流程

---

## 三、最小操作集合

这条 flow 依赖的最小 operation：

- `tap`
- `input_text`
- `press_key`
- `scroll`
- `back`
- `capture_screenshot`

补充说明：

- `capture_screenshot` 不是为了“控制”，而是为了详情采集证据和图片区补洞。
- 正文 / 评论优先走 Accessibility。
- 图片优先走 screenshot artifact。

---

## 四、正式 flow

## Step 1：Open Search Entry

```text
observe home feed
-> filter search entry
-> pre-anchor: page contains 关注/发现/视频
-> tap(search entry)
-> post-anchor: search input appears
```

### observer

- requireAccessibility: true
- requireScreenshot: false
- filters:
  - `search_entry_filter`
  - `blocker_filter`

### success signal

- 输入框出现
- 或页面出现 `猜你想搜 / 历史记录 / 问AI`

---

## Step 2：Input Keyword

```text
observe search input page
-> filter search input
-> pre-anchor: input visible and editable
-> input_text("deepseek v4")
-> post-anchor: input value contains keyword
```

### observer

- requireAccessibility: true
- requireScreenshot: false
- filters:
  - `search_input_filter`
  - `search_submit_filter`

### success signal

- 输入框已带关键词

---

## Step 3：Submit Search

```text
observe search input page
-> filter submit action
-> pre-anchor: keyword already present
-> press_key(search) or tap(search submit)
-> post-anchor: result list visible
```

### observer

- requireAccessibility: true
- requireScreenshot: false
- filters:
  - `search_submit_filter`
  - `blocker_filter`

### success signal

- 搜索结果列表出现
- 当前关键词在结果页可见

---

## Step 4：Open Next Result Card

```text
observe result page
-> filter result cards
-> remove visited cards
-> choose next candidate
-> pre-anchor: card still visible and belongs to current keyword result set
-> tap(result card)
-> post-anchor: post detail visible
```

### observer

- requireAccessibility: true
- requireScreenshot: true
- filters:
  - `result_card_filter`
  - `result_list_filter`
  - `blocker_filter`

### selection policy

- first visible unvisited
- if none visible -> scroll result list -> observe again

### success signal

- 正文 / 作者 / 图片区域出现

---

## Step 5：Collect Post Detail

```text
observe post detail
-> capture screenshot
-> dump accessibility tree
-> extract body / author / image slots / first-screen comments
-> if comments not enough -> continue comment scroll loop
```

### observer

- requireAccessibility: true
- requireScreenshot: true
- filters:
  - `post_body_filter`
  - `post_image_filter`
  - `post_comment_filter`
  - `detail_scroll_filter`

### collected fields

- `post.title`
- `post.author`
- `post.body_text`
- `post.images[]`
- `post.comments[]`
- `artifacts.screenshot`
- `artifacts.accessibility`

### success signal

- 至少拿到：
  - 正文或标题
  - 一份 screenshot
  - 一份 accessibility raw

---

## Step 6：Comment Scroll Loop

```text
observe current detail/comment page
-> collect visible comments
-> detect comment scroll container
-> scroll downward
-> observe again
-> stop when repeated signature / no new comments / end marker
```

### stop conditions

- 连续 2 次没有新增评论
- 页面出现“没有更多评论”
- 评论容器到底

### note

- 若评论区与正文区不是同一个 scroll container，则切到 `xhs_comment_expanded_page`

---

## Step 7：Back To Result List

```text
back
-> observe
-> post-anchor: result page restored
-> continue next card
```

### success signal

- 关键词仍在
- 结果列表恢复
- 刚才访问过的卡片已加入 visited set

---

## 五、Blocker 处理

在任一步出现下面 blocker，都先走 blocker 分支：

- 登录要求
- 权限申请
- 升级提示
- 广告遮挡

处理规则：

```text
observe blocker
-> filter close/skip/later/deny
-> tap blocker action
-> observe
-> if blocker gone -> return previous step
-> else emit manual_intervention_required
```

---

## 六、采集结果结构

每条帖子至少输出：

```json
{
  "keyword": "deepseek v4",
  "rank": 1,
  "post": {
    "title": "",
    "author": "",
    "bodyText": "",
    "images": [],
    "comments": []
  },
  "artifacts": {
    "screenshots": [],
    "accessibilityRuns": []
  }
}
```

---

## 七、当前执行前置条件

这条 flow 在当前仓库要真正跑起来，必须先补齐 Flowy operate commands：

1. `tap`
2. `input_text`
3. `press_key`
4. `scroll`
5. `back`

当前已有观察链路可以直接复用：

1. `capture-screenshot`
2. `dump-accessibility-tree`

---

## 八、当前结论

小红书这条 automation flow 的主闭环已经收敛成：

```text
home
-> search input
-> search result
-> post detail
-> comment collect
-> back to result
-> next result
```

下一步不该再继续“手工乱点页面”，而是：

1. 先把 operate command 接到 Flowy control surface
2. 再按这份 flow 真机执行 `deepseek v4`
