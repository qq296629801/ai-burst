---
name: mag-coordination
description: 多 Agent 项目内协作要点（产品读需求、开发分层记录、测试单测计划、项目经理派工与 A2A）。
---

## 使用方式

- 产品：先读需求文档，再提交开发需求候选到需求池。
- 开发：按 FRONTEND / BACKEND 角色分别记录实现计划。
- 测试：记录单元测试范围与断言。
- 项目经理：`list_dispatchable_agents` → `dispatch_task`；必要时 `invoke_peer_agent` 或 `pm_delegate_reflection`。
