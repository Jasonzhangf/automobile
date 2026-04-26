# Overlay Apps Section 规格

## 一、菜单层级

悬浮球展开后，主菜单新增第四个入口：

```
状态区
─────────
Agent 自身控制
捕获模式
系统设置
应用           ← 新增
```

点击「应用」进入应用列表（第一版只有小红书）。

## 二、小红书搜索配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| keyword | string | "" | 搜索关键词，手动输入 |
| maxPosts | int | 10 | 采集帖子数量上限 |
| collectImages | bool | true | 是否采集图片 URL |
| collectLinks | bool | true | 是否采集笔记链接 |
| collectVideoLinks | bool | false | 是否采集视频链接 |
| collectComments | bool | true | 是否采集评论 |
| commentLimit | int | 0 | 评论采集条数，0=不限 |
| likeTarget | enum | NONE | 点赞目标：NONE / POST / KEYWORD_COMMENT |
| likeKeyword | string | "" | likeTarget=KEYWORD_COMMENT 时匹配的关键词 |

### 点赞策略

| 值 | 含义 |
|----|------|
| `NONE` | 不点赞 |
| `POST` | 点赞帖子主体 |
| `KEYWORD_COMMENT` | 点赞包含 likeKeyword 的评论 |

## 三、UI 结构

```
[应用] section
├── [小红书搜索]  ← 点击进入配置表单
│   ├── keyword 输入框（小输入框 + 确认）
│   ├── maxPosts 滑块/数字 1~50
│   ├── 开关组：collectImages / collectLinks / collectVideoLinks
│   ├── 评论区：collectComments(开关) + commentLimit(数字)
│   ├── 点赞区：likeTarget(三选一) + likeKeyword(条件显示)
│   └── [开始搜索] 按钮
```

## 四、数据流

```
用户配置 → XhsSearchConfig → 存 SharedPreferences
点击开始 → 读 config → 构建 step payloads → 本地/远程 WS 发送 → WorkflowStepFlow 执行
执行中 → 悬浮球状态显示当前步骤
完成 → 结果上传 artifact
```

## 五、文件落位

| 文件 | 职责 |
|------|------|
| `foundation/XhsSearchConfig.kt` | 数据类 |
| `foundation/XhsSearchConfigStore.kt` | SharedPreferences 持久化 |
| `ui/workbench/XhsSearchFormFactory.kt` | 配置表单 UI 构建 |
| `ui/workbench/WorkbenchViewFactory.kt` | 新增 buildAppsSection |
| `ui/workbench/WorkbenchOverlayService.kt` | 新增 Section.APPS + 路由 |
| `flows/XhsSearchFlow.kt` | 搜索编排流程（后续实现） |
