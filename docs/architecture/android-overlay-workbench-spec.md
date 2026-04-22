# Android Overlay Workbench Spec v1

## 目标

定义 Flowy 第一版手机端开发工作台。

这一版不是完整产品 UI，而是：

- 一个可在任意 app 上使用的悬浮球工作台
- 一个由 Mac daemon 驱动的被动模式 Android agent 入口
- 一个支持页面观察、目标捕获、测试、保存、回传的开发闭环

一句话定义：`overlay workbench v1 = on-device flow authoring skeleton`

---

## 一、第一版的核心定位

第一版要解决的问题不是“把所有自动化都做完”，而是：

- 让 Mac daemon 可以驱动 Android agent
- 让 Android agent 可以观察当前页面
- 让用户可以在任意页面定义目标
- 让目标定义可以测试、保存、回传 daemon
- 让 daemon 产出 filter / workflow 的开发素材

因此这一版的本质是：

- **开发骨架**
- **流程素材创建骨架**
- **交互工作台**

不是：

- 完整产品化 UI
- 最终态 workflow 编辑器
- 最终态本地全自动编排器

---

## 二、第一版最小闭环

第一版固定围绕下面这条链路设计：

```text
Mac daemon send command
-> passive-mode Android agent receives command
-> agent observes current page
-> user enters capture mode from overlay
-> user long-presses target
-> target confirmation
-> alias input
-> target test
-> manual save
-> local save + send to daemon
-> daemon materializes target/filter/workflow seed
```

只有这条链路成立，第一版才算完成。

---

## 三、第一版范围冻结

### 1. 必做

- 悬浮球收起 / 展开骨架
- 状态区
- 一级菜单入口
- 被动模式 Android agent 与 Mac daemon 通信
- 当前页面 observation 回传
- 捕获模式主流程
- 保存结果本地落盘
- 保存结果发送 daemon
- daemon 将结果落成开发素材

### 2. 暂不做

- 复杂产品化视觉
- 多层独立设置页
- 本地主控模式完整流程
- 复杂 flow 可视化编辑器
- OCR 优先识别链路
- 复杂升级 UI
- 历史回放 UI

---

## 四、第一版 UI 骨架

### 1. 收起态

- 半透明悬浮球
- 贴边吸附
- 贴边后半隐藏
- 显示状态点 + 简短文字

文字规则：

- 默认显示当前状态
- 执行中显示当前动作

---

### 2. 展开态

点击悬浮球后，在球旁边向内展开。

结构固定为：`top: status area -> bottom: menu entries`

上半区负责展示：

- daemon state
- connection state
- accessibility state
- screenshot state
- current action

下半区第一版固定 3 个入口：

1. `Agent 自身控制`
2. `捕获模式`
3. `系统设置`

---

## 五、一级菜单职责

### 1. Agent 自身控制

负责：

- 被控模式 / 主控模式切换
- 本地配置编辑
- daemon IP / port 编辑
- 当前连接配置展示

当前约束：

- 被控 / 主控是互斥单选
- 当前阶段以 **被控模式** 为主路径
- 所有配置直接在悬浮菜单里修改
- 改完立即生效

---

### 2. 捕获模式

负责：

- 进入目标定义流程
- 在任意页面上选择目标
- 生成 alias
- 执行测试
- 保存目标定义

当前约束：

- 普通点击不拦截下层 app
- 长按用于选中目标
- 目标确认菜单在目标附近弹出
- alias 输入框也在目标附近弹出
- 测试通过后用户手动点击保存
- 保存 = 本地保存 + 发送 daemon

---

### 3. 系统设置

负责：

- 权限状态展示与跳转
- 升级入口
- 与运行环境相关的系统级检查

当前顺序固定为：

1. 权限相关
2. 升级相关

升级规则：

- 升级是普通菜单项
- 点击后自动检查 daemon 目录是否存在更高版本
- 如存在，则走升级流程

---

## 六、被动模式主路径

第一版 Android agent 的真主路径固定为：`passive mode`

含义：

- Android agent 主要由 Mac daemon 驱动
- Android 侧负责：
  - 接收命令
  - 观察页面
  - 响应捕获
  - 回传结果

第一版不要求：

- Android 端本地主控逻辑完整
- Android 端本地 workflow 编排完整

主控模式可以保留入口，但不作为第一版验收主路径。

---

## 七、页面观察与捕获闭环

### 1. 页面观察

第一版观察链路：

- Accessibility first
- screenshot second
- OCR not required in v1

最小 observation 输出：

- package name
- activity / window metadata
- page signature
- raw accessibility snapshot ref
- normalized page state ref
- screenshot ref（若可用）

---

### 2. 捕获模式详细流程

固定流程：

```text
enter capture mode
-> long-press target
-> target-near confirm / cancel
-> target-near alias input
-> target test menu
   -> smoke
   -> simulated tap
   -> real tap
-> manual save
-> local save + send daemon
```

### 3. 保存后的结果

保存结果至少要包含：

- target alias
- source page signature
- target locator candidate
- target bounds snapshot
- capture timestamp
- test results
- operator-chosen save flag

---

## 八、daemon 侧开发素材化

第一版 daemon 侧不能只做“收到了就存一下”。

收到保存结果后，要形成可复用开发素材。

最小素材输出：

- `target definition`
- `filter draft`
- `workflow seed`
- `capture evidence bundle`

### 1. target definition

至少包含：

- alias
- target ref
- target kind
- locator candidate
- page signature

### 2. filter draft

至少包含：

- 当前页面的 filter candidate
- 目标筛选线索
- ref 命名草案

### 3. workflow seed

至少包含：

- step name 草案
- target ref 引用
- operation ref 草案
- pre/post anchor 草案位

### 4. capture evidence bundle

至少包含：

- page observation
- screenshot ref（若有）
- raw tree ref
- save request payload
- test result payload

---

## 九、模块拆分真源

第一版按下面模块拆。

### A. Android UI 模块

1. `FloatingBallOverlay`
   - 悬浮球显示、拖拽、吸附、半隐藏

2. `OverlayMenuContainer`
   - 球旁展开、收起、菜单布局容器

3. `OverlayStatusPanel`
   - 状态区展示

4. `OverlayMenuEntryGrid`
   - 一级菜单入口区

### B. Android 捕获模块

5. `CaptureModeController`
   - 捕获模式开关与状态管理

6. `TargetSelectionOverlay`
   - 目标命中、长按选中、候选高亮

7. `TargetConfirmPopover`
   - 目标附近确认 / 取消

8. `AliasInputPopover`
   - 目标附近 alias 输入

9. `TargetTestPopover`
   - smoke / simulated tap / real tap / save

### C. Android 运行桥接模块

10. `OverlayUiStateStore`
    - UI 所有状态唯一真源

11. `DaemonStatusBridge`
    - daemon / WS / command 状态桥接

12. `ObservationBridge`
    - 当前 page observation 注入 UI

13. `AgentControlMenu`
    - 被控/主控、本地配置、daemon 配置

14. `SystemSettingsMenu`
    - 权限与升级菜单

### D. Mac daemon 模块

15. `ObservationRequestHandler`
    - 请求当前 observation

16. `CapturedTargetIngestFlow`
    - 接收保存后的目标定义

17. `TargetMaterializationFlow`
    - 生成 target/filter/workflow 素材

18. `CaptureArtifactStore`
    - 保存 evidence bundle

### E. Shared modules

19. `target-capture payload truth`
20. `filter draft truth`
21. `workflow seed truth`

---

## 十、最小实现顺序

### Step 1：overlay shell

先做：

- 悬浮球
- 展开收起
- 状态区空骨架
- 三个一级菜单入口

完成标准：

- UI 可见
- 可拖拽
- 可吸附
- 可半隐藏
- 可展开

---

### Step 2：passive-mode bridge

接上：

- daemon 状态
- connection 状态
- 当前 observation 摘要

完成标准：

- Android 被动模式能与 Mac daemon 通信
- overlay 状态区能显示真实运行状态

---

### Step 3：capture mode shell

接上：

- 进入捕获模式
- 长按选中
- confirm / cancel
- alias 输入
- test 菜单

完成标准：

- 能在任意页面走完整捕获 UI 流程
- 即使底层定位还很薄，也要先有完整骨架

---

### Step 4：save-target protocol

接上：

- 本地保存
- save payload
- send to daemon

完成标准：

- 用户点保存后，数据同时本地落盘并回传 daemon

---

### Step 5：daemon materialization

接上：

- target definition
- filter draft
- workflow seed

完成标准：

- daemon 收到保存结果后能产出后续流程开发素材

---

## 十一、第一版验收标准

第一版只有同时满足下面条件，才算骨架完成：

1. 悬浮球可显示、拖拽、吸附、半隐藏、展开
2. Android agent 在被动模式下可与 Mac daemon 通信
3. Mac daemon 可请求当前 page observation
4. 用户可在任意页面进入捕获模式并长按选中目标
5. 用户可输入 alias、执行测试、手动保存
6. 保存结果会本地落盘并发送 daemon
7. daemon 能将结果落成 target/filter/workflow 开发素材

---

## 十二、当前接受的设计结论

1. 第一版手机端不是完整产品 UI，而是 **overlay workbench**。
2. 第一版目标是建立“观察 -> 捕获 -> 测试 -> 保存 -> 素材化”的开发闭环。
3. Android 端第一版主路径固定为 **被动模式**。
4. 悬浮球负责交互入口，Foreground Service 负责常驻主干。
5. 捕获模式是第一版最核心能力。
6. 保存结果必须同时本地落盘并发送 daemon。
7. daemon 必须把保存结果素材化，而不是只做原始落盘。
