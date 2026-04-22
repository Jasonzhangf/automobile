# Android Overlay UI Spec v1

## 目标

定义 Flowy 手机端第一版正式 UI。

当前这一版首先服务于：

- `docs/architecture/android-overlay-workbench-spec.md`

也就是“开发工作台骨架”，不是完整产品化 UI。

- 形态是 **悬浮球**
- 默认 **半透明、贴边、低占用**
- 点击后展开 **轻量模态菜单**
- 服务于 **可见、可交互、可中断** 的 Agent hand

这份文档只定义：

1. 悬浮球形态
2. 展开菜单形态
3. 与 daemon / 感知 / 截图能力的关系
4. 生命周期与状态同步

---

## 一、设计结论

第一版手机端 UI 固定采用：

```text
FloatingBallOverlay
-> Expandable Modal Menu
-> Full Debug Panel (optional)
```

同时明确：

- **悬浮球是 UI 入口，不是主保活机制**
- **Foreground Service 才是后台常驻主干**
- 悬浮球负责：
  - 让用户随时看到 agent 状态
  - 提供最小交互入口
  - 快速触发调试动作

---

## 二、第一版 UI 结构

### 1. 收起态：悬浮球

要求：

- 小尺寸
- 半透明
- 可拖拽
- 自动吸附屏幕边缘
- 贴边后可半隐藏
- 不长期遮挡操作区域

建议默认属性：

- 直径：`44dp ~ 56dp`
- 默认透明度：`0.55 ~ 0.75`
- 空闲时降低透明度
- 用户交互时恢复高亮

收起态只显示最少信息：

- 连接状态点
- 忙/闲状态
- 可选的最近动作提示

---

### 2. 展开态：轻量模态菜单

触发方式：

- 单击悬浮球：展开
- 点击空白区域：收起
- 再次点击悬浮球：收起

在 workbench v1 中，展开菜单固定为：

#### A. 上半区：运行状态

- daemon：running / stopped
- control channel：connected / disconnected
- accessibility：enabled / disabled
- screenshot session：ready / missing

#### B. 下半区：一级菜单入口

- Agent 自身控制
- 捕获模式
- 系统设置

详细职责与 V1 闭环定义见：

- `docs/architecture/android-overlay-workbench-spec.md`

---

### 3. 全量调试面板

第一版可以不立即实现复杂页面，但接口必须预留。

它负责承载较重信息：

- 最近命令列表
- 最近 block 执行结果
- page state 摘要
- artifact 上传状态
- websocket 连接详情
- runtime version / server profile

规则：

- 悬浮球不承载重信息
- 重信息进入 full debug panel

---

## 三、交互规则

### 1. 基础手势

- 单击：展开 / 收起菜单
- 长按：进入拖拽
- 拖拽结束：自动吸附最近边缘
- 双击：执行默认快捷动作

第一版默认快捷动作建议为：

- `capture-screenshot`

后续可配置。

---

### 2. 吸附与隐藏

规则：

- 拖拽释放后，自动贴到最近左右边缘
- 贴边后允许半隐藏，只露出可拖出的热区
- 展开菜单时，悬浮球需自动完全露出

目的：

- 减少遮挡
- 允许用户在任意 app 中保留入口

---

### 3. 触摸策略

收起态默认：

- 不抢占输入焦点
- 只接收自身点击 / 拖拽
- 不扩大拦截区域

展开态：

- 菜单区域可交互
- 非菜单区域点击用于收起

规则：

- 不允许悬浮球长期阻断下层 app 正常点击
- 不允许默认全屏拦截

---

## 四、状态模型

悬浮球和菜单展示都从统一状态源读取。

第一版状态模型建议：

```text
ui state
-> daemon state
-> control channel state
-> accessibility state
-> screenshot capability state
-> last action state
```

### 1. daemon state

- `stopped`
- `starting`
- `idle`
- `busy`
- `error`

### 2. control channel state

- `disconnected`
- `connecting`
- `connected`
- `reconnecting`
- `error`

### 3. accessibility state

- `unknown`
- `enabled`
- `disabled`
- `degraded`

### 4. screenshot capability state

- `unknown`
- `ready`
- `permission_required`
- `session_lost`

### 5. last action state

- 最近 command name
- 最近执行时间
- 最近结果：`ok` / `error`

---

## 五、颜色与反馈策略

第一版只要求状态清晰，不追求视觉复杂。

建议：

- 绿色：connected / ready / ok
- 黄色：busy / reconnecting / waiting permission
- 红色：error / disabled / disconnected
- 灰色：stopped / unknown

悬浮球可通过：

- 边框颜色
- 小角标
- 短暂 pulse 动画

表达状态变化。

规则：

- 动画必须轻量
- 不允许持续闪烁影响操作

---

## 六、与后台常驻架构的关系

必须明确分层：

### 1. FloatingBallOverlay

职责：

- 显示入口
- 展示当前状态
- 呼出菜单

不负责：

- 维护长连接
- 执行核心 block
- 持有截图主逻辑

### 2. DaemonForegroundService

职责：

- 长时运行
- 保持 control channel
- 接收并执行命令
- 维护日志
- 管理 screenshot session / runtime 状态

### 3. AccessibilityService

职责：

- 获取页面树
- 监听窗口变化
- 提供 page observation 真源

### 4. DevPanelActivity / FullDebugPanelActivity

职责：

- 权限申请
- 深度调试界面
- 重信息展示

结论：

- **悬浮球不是保活主干**
- **Foreground Service 才是运行主干**

---

## 七、生命周期规则

### 1. 推荐运行关系

```text
user enables runtime
-> foreground service starts
-> overlay attaches
-> accessibility state syncs
-> overlay reflects daemon state
```

### 2. overlay 生命周期

- service 运行时可显示 overlay
- 用户可手动隐藏 overlay，但 daemon 可继续运行
- overlay 消失不等于 daemon 停止

### 3. daemon 生命周期

- daemon 停止时，overlay 必须展示 `stopped`
- daemon 重连时，overlay 必须展示 `reconnecting`
- daemon 出错时，overlay 必须给出可见反馈

---

## 八、权限与前提条件

第一版 UI 必须能明确展示下列前提是否满足：

- overlay permission
- accessibility enabled
- notification permission / foreground notification status
- screenshot permission state
- battery optimization exemption state

规则：

- 不满足时不能静默失败
- 菜单里必须能直接跳到对应检查或修复入口

---

## 九、第一版最小实现范围

只做下面这些：

1. 悬浮球显示 / 拖拽 / 吸附
2. 单击展开菜单
3. 菜单展示 4 组状态与动作
4. 状态从 daemon 读取
5. 支持：
   - start daemon
   - stop daemon
   - capture screenshot
   - dump accessibility tree
   - open logs

先不做：

- 复杂动画
- 多球模式
- 自定义主题
- 页面内高亮框选
- flow 级可视化编排

---

## 十、与后续 UI 的衔接

第一版悬浮球菜单稳定后，再往下扩：

1. Full debug panel
2. 当前 page state 摘要卡片
3. 当前 target filter 结果预览
4. 最近 workflow 执行轨迹
5. 升级与 runtime sync 页面

---

## 十一、当前接受的设计结论

1. 手机端第一版 UI 采用 **悬浮球 + 展开菜单**。
2. 悬浮球默认半透明、贴边、低占用。
3. 悬浮球是 UI 入口，不是主保活机制。
4. 后台常驻主干固定为 `Foreground Service`。
5. 悬浮球状态必须反映 daemon / control / accessibility / screenshot 四类状态。
6. 第一版菜单先服务于调试与 runtime 控制，不先做复杂产品 UI。
