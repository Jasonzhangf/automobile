# XHS Regression Fixtures

放小红书探索模板对应的共享样本真源：

- workflow step payload
- 目标关键词样本
- 后续成功/失败 response 样本
- 后续页面证据样本

当前第一批先放：

- `workflow-steps/*.payload.json`

用途：

1. 直接喂给 `run-workflow-step`
2. 作为真机探索的标准输入
3. 后续补 response / artifact 样本时作为同一目录真源
