# App Collection Workflow Abstraction v1

## 目标

定义 Flowy 的跨 APP 自动化采集抽象。

这份文档回答 5 个问题：

1. 哪些部分是所有 APP 共用的骨架
2. 哪些部分是 APP 自己的 profile
3. 点赞 / 回复 / 评论入口应该放在哪一层
4. 采集内容如何结构化
5. 本地 / 远端 daemon 如何完整地下发与回收结果

一句话结论：

```text
同一套 workflow skeleton
+ 同一套 operation / observer / anchor / event 基座
+ 不同 APP 的 profile / field mapping / target filter
= 可迁移的跨 APP 自动化采集框架
```

---

## 一、抽象原则

### 1. 骨架与 APP 差异必须分离

不能把下面这些混在同一个 workflow 里：

- 通用操作能力
- 页面推进逻辑
- 某个 APP 的按钮文本
- 某个 APP 的字段命名

必须拆成：

1. `workflow skeleton`
2. `app profile`
3. `field mapping`
4. `artifact / output schema`

---

### 2. 点赞 / 回复 / 评论入口属于“目标锚点”，不是业务文案

例如：

- 小红书的“回复”
- 微博的“评论”
- 某 app 的“点赞小心心”

虽然视觉不同，但它们在骨架层都属于：

- `interaction entry`
- `target anchor`
- `operation candidate`

因此通用层只定义：

- `like_entry`
- `reply_entry`
- `comment_entry`

APP profile 再定义各自怎么找到它们。

---

### 3. 采集不是“页面截图而已”，而是结构化输出

最终目标不是拿一堆截图，而是拿到：

- 可复用的数据结构
- 对应证据
- 对应入口点
- 对应回放能力

---

## 二、统一骨架

## 1. 通用 workflow skeleton

所有信息流 / 内容平台都先映射成同一个流程：

```text
enter app
-> open search/input/discovery entry
-> submit query or choose topic
-> observe result/feed list
-> select next content card
-> open detail
-> collect body/media/comments
-> collect interaction entry points
-> back to list
-> continue
```

其中：

- 有没有 search，由 APP profile 决定
- 是结果页还是推荐流，由 APP profile 决定
- 评论是否独立页，由 APP profile 决定

但 skeleton 不变。

---

## 2. 通用 operation set

跨 APP 统一使用同一套操作原语：

- `tap`
- `long_press`
- `scroll`
- `input_text`
- `press_key`
- `back`
- `wait`
- `capture_screenshot`

补充：

- 这套 operation 不带业务语义
- “点搜索”、“点评论”、“点回复” 都是：
  - `tap(targetRef)`

---

## 3. 通用 target anchor set

所有 APP 的 target 都先归入统一锚点类别：

### A. 页面推进锚点

- `search_entry`
- `search_input`
- `search_submit`
- `result_list`
- `result_card`
- `detail_back`

### B. 内容锚点

- `author_anchor`
- `title_anchor`
- `body_anchor`
- `media_gallery`
- `comment_list`
- `comment_item`

### C. 交互入口锚点

- `like_entry`
- `reply_entry`
- `comment_entry`
- `share_entry`
- `follow_entry`

### D. 例外锚点

- `blocker_close`
- `login_gate`
- `permission_gate`
- `upgrade_gate`

规则：

- skeleton 只认这些抽象名
- 具体 APP 自己决定这些锚点如何匹配

---

## 三、APP profile

APP profile 是把“同一套骨架”落到某个 APP 上的适配层。

建议每个 APP profile 至少定义 4 类内容：

## 1. 页面类型

例如小红书：

- `home_feed`
- `search_input`
- `search_result`
- `post_detail`
- `comment_expanded`
- `blocker`

例如微博：

- `home_feed`
- `search_input`
- `search_result`
- `post_detail`
- `comment_sheet`
- `blocker`

---

## 2. 页面签名与 filter

每个页面定义：

- 如何识别页面
- 如何识别页面上的目标锚点

例如：

```text
page: xhs_post_detail
anchors:
  - body_anchor
  - media_gallery
  - comment_entry
  - like_entry
  - reply_entry
```

---

## 3. 字段映射

统一输出字段不直接依赖某个 APP 的控件名。

APP profile 负责把页面上的实际元素映射到统一字段：

- `post.title`
- `post.author`
- `post.bodyText`
- `post.media[]`
- `post.comments[]`
- `entry.like`
- `entry.reply`
- `entry.comment`

---

## 4. 页面转移规则

定义：

- 从哪个页面能去哪个页面
- 哪个 anchor 触发转移
- 转移后的 post-anchor 是什么

例如：

```text
search_result --tap(result_card)--> post_detail
post_detail --tap(comment_entry)--> comment_expanded
post_detail --back--> search_result
```

---

## 四、内容提取 contract

## 1. 通用内容结构

所有 APP 最终都尽量收敛成统一结构：

```json
{
  "source": {
    "app": "xhs",
    "pageType": "post_detail",
    "keyword": "deepseek v4"
  },
  "post": {
    "title": "",
    "author": "",
    "bodyText": "",
    "media": [],
    "comments": []
  },
  "entryPoints": {
    "like": null,
    "reply": null,
    "comment": null
  },
  "artifacts": {
    "screenshots": [],
    "accessibility": []
  }
}
```

---

## 2. 内容与入口要分开

不要把“评论内容”和“评论入口”混成一层。

应该分成：

### 内容字段

- `post.title`
- `post.bodyText`
- `post.comments[]`

### 入口字段

- `entryPoints.like`
- `entryPoints.reply`
- `entryPoints.comment`

原因：

- 入口是后续交互流程的起点
- 内容是采集结果
- 两者生命周期不同

---

## 3. 评论采集 contract

评论必须支持：

- 增量采集
- 去重
- 结束判断
- 回复层级

建议结构：

```json
{
  "commentId": "",
  "author": "",
  "text": "",
  "timeText": "",
  "likeCountText": "",
  "replyCountText": "",
  "replies": []
}
```

补充：

- `replyCountText` 是内容字段
- `reply_entry` 是入口锚点

---

## 五、daemon 抽象

## 1. daemon 的职责

本地或远端 daemon 都走同一套职责：

1. 下发 command / workflow
2. 接收 runtime event
3. 拉取 artifacts
4. 汇总结构化结果
5. 持久化 run bundle

daemon 不是“只能发命令”，而是要完整闭环：

```text
command
-> runtime observe/operate
-> event feedback
-> artifact upload
-> result materialization
```

---

## 2. runtime 的职责

手机端 runtime 负责：

1. 根据 command 执行 operation
2. 在关键节点做 observe
3. 回传 page state / event / artifact
4. 不静默失败

---

## 3. 本地 / 远端切换

这个框架不绑定控制端位置：

- 本机 daemon
- Mac daemon
- 远端 daemon

都只是同一个 control surface 的不同部署位。

因此要求：

- CLI 统一
- WebSocket 统一
- CommandEnvelope / EventEnvelope 统一
- page model 统一

---

## 六、推荐分层

建议固定 4 层：

## 1. Core Skeleton

跨 APP 共享：

- workflow skeleton
- operation set
- anchor taxonomy
- output schema

## 2. App Profile

每个 APP 独有：

- 页面类型
- 页面识别规则
- 目标 filter
- 页面转移规则

## 3. Field Mapping

每个页面 / 内容类型独有：

- title 从哪里抽
- body 从哪里抽
- media 从哪里抽
- comment 从哪里抽
- like/reply/comment 入口从哪里抽

## 4. Runtime / Daemon

执行与回收层：

- command dispatch
- operation execution
- observation
- artifact upload
- result assembly

---

## 七、XHS 示例映射

## 1. skeleton 层

```text
open search
-> input keyword
-> submit
-> open result card
-> collect detail
-> collect comments
-> back
-> next
```

## 2. app profile 层

小红书页面：

- `xhs_home_feed_page`
- `xhs_search_input_page`
- `xhs_search_result_page`
- `xhs_post_detail_page`
- `xhs_comment_expanded_page`
- `xhs_blocker_page`

## 3. field mapping 层

小红书 detail 页提取：

- `title`
- `author`
- `bodyText`
- `media[]`
- `comments[]`
- `entry.like`
- `entry.reply`
- `entry.comment`

---

## 八、最终结论

跨 APP 自动化采集的正确抽象是：

```text
通用操作行为
+ 通用目标锚点
+ 通用内容 contract
+ 通用 daemon 控制面
+ APP profile
+ field mapping
= 可复用 workflow
```

所以今后的实现顺序应该固定成：

1. 先补通用 operate blocks
2. 再补统一 anchor / field / output schema
3. 然后为每个 APP 只新增 profile 与 mapping
4. daemon 始终走统一 control surface
