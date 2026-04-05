---
name: mag-coordination
description: 多 Agent 项目内协作要点（产品读需求并合并开发说明、项目经理派工与 A2A）。
---

## 使用方式

- 产品：先读需求文档，再通过工具将开发需求说明**直接合并**进需求文档新版本（不经需求池）。
- 开发 / 测试：无独立「改进日志」工具；产出以编排回复与任务流程事件为准。
- 项目经理：`list_dispatchable_agents` → `dispatch_task`；必要时 `invoke_peer_agent` 或 `pm_delegate_reflection`。
