# 多 Agent 协作与项目管理 — 技术方案

> **编制关系**：本文以 [产品/多Agent协作与项目管理产品线需求.md](../产品/多Agent协作与项目管理产品线需求.md) 为**唯一能力来源**；产品 **§2.1 全量模块**与正文各节功能**不得在技术方案中默示删减**。实施排期可分期上线，但**设计覆盖范围**须与产品全文一致；**逐项对照与缺口清零**见 **§17**。  
> 对应产品需求：[产品/多Agent协作与项目管理产品线需求.md](../产品/多Agent协作与项目管理产品线需求.md)  
> 关联升级决策：JDK **24**、Spring Boot **3.5+**（须官方支持 JDK 24 的 BOM）；**AgentScope Java**、**Temporal**（**Docker** 部署）、**WebSocket** 大屏、首期知识库**关键词/标签**、定时任务 **Redis 锁**；项目成员**不设**「仅负责拍板」独立角色；需求正文 **MySQL MEDIUMTEXT 落库**（首期不拆 MinIO 大对象）。

> **当前代码库已移除（与下文部分章节不一致）**：表 `mag_requirement_pool_item`、`mag_release_archive`、`mag_pm_assist_record`、`mag_agent_improvement_log` 及对应 REST、`mag:pool:decide` / `mag:release:archive` 菜单权限、待办聚合、`work-outputs` 聚合、发版→`ARCHIVE_REFLOW` 知识库写入、开发/测试侧「改进日志」Agent 工具。产品工具仍可将开发需求说明**直接合并**进 `mag_requirement_revision`；任务自动申报完成见 **§4.3.1**（编排成功**或**产品合并工具写入新版本）。**无**人工 `submit-complete` / `block` / `request-next` HTTP 接口。

---

## 0. 前置决策（已确认）

| 决策项 | 结论 |
|--------|------|
| **JDK / Spring Boot** | 升级至 **Java 24 + Spring Boot 3.5+**（与当前 `ai-burst-backend` BOM 对齐；小版本随安全补丁升级）；存量模块（权限、LLM）已随仓库迁移。 |
| **Temporal 部署** | **Docker**（Compose 或编排等价物）部署 Temporal Server；Worker 与 Spring Boot 同仓或同镜像多进程按环境选择。 |
| **项目成员角色** | **不设**「仅负责需求池拍板」的独立成员类型；拍板权限由 **RBAC（如 `mag:pool:decide`）+ 项目成员（OWNER/MEMBER/VIEWER）** 组合控制。 |
| **需求文档存储** | 正文 **MEDIUMTEXT 落库**（`mag_requirement_revision.content`）；首期不把需求大文本拆到 MinIO。 |
| **长流程** | **Temporal** |
| **定时任务** | **`@Scheduled` + Redis 分布式锁** |
| **知识库（首期）** | **MySQL 全文 / 标签 / 关键词**（二期向量库另案） |
| **大屏** | **WebSocket** |
| **外网检索** | **本期不建设**独立外网代理工具与审计表；成熟产品检索等留痕以 **需求池 `payload_json`**、**线程消息** 等为准（见 §17） |
| **Agent 栈** | **AgentScope Java**（`io.agentscope:agentscope-core`）为主 |
| **数据保留** | `mag_message`、`mag_agent_improvement_log` 等 **永久保留**（不自动 TTL 删除），依赖平台备份与容量规划 |
| **归档→知识库** | `quality_flag=1` **自动**写入 `mag_kb_entry`（`ARCHIVE_REFLOW`） |
| **站外通知** | **WebSocket + `mag_alert_event`**；**可接 QQ 机器人**（Webhook/HTTP 回调，与具体 Bot 协议对齐） |

---

## 1. 已定架构选型（复查表）

| # | 主题 | 结论 |
|---|------|------|
| 1 | 长流程 | **Temporal** |
| 2 | 定时任务（多实例） | **`@Scheduled` + Redis 分布式锁** |
| 3 | 知识库（首期） | **MySQL 全文 / 标签 / 关键词**（二期向量） |
| 4 | 大屏 | **WebSocket** |
| 5 | 外网检索 | **本期不实现**出站抓取与 `mag_external_fetch_audit`；留痕走池与消息 |
| 6 | Agent 栈 | **AgentScope Java** 为主 |
| 7 | 运行环境 | 应用与 Temporal **Docker** 化交付（镜像 JDK 24） |

---

## 2. 总体架构

```
                        ┌─────────────────────────────────────────┐
                        │              Temporal Server             │
                        │   (Workflow: 拍板等待 / 编排 / 超时)   │
                        └─────────────────┬───────────────────────┘
                                          │ gRPC
┌──────────┐   REST/WS    ┌───────────────▼──────────────┐    ┌──────────┐
│ Vue 3    │◄────────────►│ Spring Boot 3.5  (ai-burst)   │◄──►│  MySQL   │
│ 项目工作台 │  WebSocket   │  · mag-* 领域 API             │    │  业务表   │
│ 大屏/待办 │              │  · AgentScope（Activity 内）    │    └──────────┘
└──────────┘              │  · Temporal Worker             │    ┌──────────┘
                          │  · LLM 通道适配 (复用 llm_channel) │◄──►│  Redis   │
                          │  · 定时 + 分布式锁               │    │ 锁/缓存   │
                          └───────────────┬──────────────────┘    └──────────┘
                                          │
                          ┌───────────────▼──────────────┐
                          │ 厂商 HTTPS（通道表解密调用）    │
                          └──────────────────────────────┘
```

- **边界**：多 Agent 业务以 **`mag_` 表前缀 + `/api/mag/**`** 与现有 `sys_*`、`llm_*` 区分；**不**在 LLM 模块内堆叠项目域逻辑，通过 **适配层** 调用 `LlmChatRouter`（或等价 Bean）。
- **Temporal Worker**：可与 API 同进程（开发）或独立 Deployment（生产）；Workflow 定义必须 **幂等、可重放**，副作用仅在 **Activity** 内执行。
- **部署**：**Temporal Server 以 Docker 运行**（官方镜像或团队维护 Compose）；生产可扩展为 K8s，与「Docker 部署」决策一致。应用镜像基于 **JDK 24**。

---

## 3. 需求追溯矩阵

**完整逐条映射（含产品 §1～§9、§11 补齐项；§2.2 边界见 §17.12）见 §17**；下表为高层索引。

| 产品章节 | 技术落点 |
|----------|-----------|
| §1 产品目标（各条） | §17.2；编排见 §5～§6 Temporal、§8 WS、§9 API |
| §2.1 模块表（全行） | §17.3；库表 §4、API §9、前端 §11 |
| §3 五类 Agent | §17.4；`mag_agent.role_type` + AgentScope 工具集 §5.2 |
| §4.1～§4.4 主/子、协调、阻塞、可观测 | §17.5；`parent_agent_id`、`mag_thread`/`mag_message`、阻塞字段、筛选查询 §9 |
| §4.5 任务结项 | 申报完成直落 `DONE`；状态 §4.3.1；无独立核查 Agent 表 |
| §5.1～§5.8 项目管理 | §17.6；各子节见表内 |
| §5.6.1 成熟产品检索 | 池 `payload_json`、协调消息等留痕 §17.7（无独立外网审计表） |
| §6.1～§6.3 需求文档与池 | §17.7；`mag_requirement_*`、池状态 §19.1 |
| §7 权限 | §17.8；§10、`sys_permission` Flyway |
| §8 非功能 | §17.9；§12、§16.4～§16.6 |
| §9 验收要点 | §17.10 技术侧验证映射 |
| 产品 §11 补齐要点 | §17.11（本文已定稿口径） |

---

## 4. 领域模型与库表设计（MySQL / Flyway）

命名前缀 **`mag_`**；时间字段 `created_at` / `updated_at`；软删按需 `deleted` TINYINT。以下为 **v1 实体清单**（字段可在实现期微调，评审以本清单为范围）。

### 4.1 项目与成员

| 表名 | 说明 |
|------|------|
| **mag_project** | 项目：名称、状态、当前需求文档版本指针、配置 JSON（阈值、开关） |
| **mag_project_member** | 真人成员：`user_id`、`project_id`、项目内角色（OWNER/MEMBER/VIEWER 等） |

### 4.2 Agent 与沟通

| 表名 | 说明 |
|------|------|
| **mag_agent** | Agent 实例：`project_id`、`role_type`（PM/PRODUCT/BACKEND/FRONTEND/TEST）、`parent_agent_id`（主/子）、`name`、`llm_channel_id`（FK→`llm_channel.id`，可空则用项目默认）、`system_prompt_profile`、`extra_json`、`status` |
| **mag_thread** | 会话线程：项目内、可选 `task_id`、标题 |
| **mag_message** | 消息：`thread_id`、发送方类型（USER/AGENT/SYSTEM）、`sender_agent_id` 可空、`content`（TEXT/JSON）、`created_at` |

### 4.3 任务与模块

| 表名 | 说明 |
|------|------|
| **mag_module** | 功能模块树：`project_id`、`parent_id`、`name`、`tag` |
| **mag_task** | 任务：`module_id` 可空、`title`、`description`、`state`（见下表）、`assignee_agent_id`、`reporter_agent_id`、`requirement_ref`（文档节点 ID 或 JSON 指针）、`temporal_workflow_id`（可空）、`block_reason`、`blocked_by_agent_id` 可空、`row_version` 乐观锁 |
| **mag_task_state_log** | 可选：状态迁移审计 |

#### 4.3.1 任务状态枚举与产品 §5.2 对照

| 库内 `state`（建议 `VARCHAR(32)`） | 产品语义 |
|-------------------------------------|----------|
| `PENDING` | 待派发 |
| `IN_PROGRESS` | 进行中 |
| `DONE` | 已完成（**申报完成**写入；含用户/API 或系统自动申报） |
| `BLOCKED` | 阻塞（须 `block_reason` + `blocked_by_agent_id` 可空） |

迁移规则（摘要）：`submit-complete`：`IN_PROGRESS` → `DONE`（可清空 `temporal_workflow_id`）。

**自动申报完成（与实现对齐，可配置）**：**无**人工 `submit-complete` HTTP 接口；系统在满足规则时由应用服务 **`MagTaskService.submitComplete`** **等价写入**上述迁移（同一状态机与流程事件）。配置项 **`aiburst.mag.task.auto-submit-complete-on-orchestration-success`**（默认 `true`，可在 `application.yml` 关闭）。**触发要点（满足其一即可，先到者生效）**：① **编排落库成功后**（`MagOrchestrationAgentRunSucceededEvent`）：`mag_orchestration_run` 为 **AGENT**、**成功结束**，记录关联 **`task_id`**；任务 **`IN_PROGRESS`** 且 **`assignee_agent_id`** 与编排 Agent 一致；**最终回复** trim 后非空。② **产品工具合并后**（同一次 Activity 内）：`PRODUCT` Agent 调用 `mag_submit_dev_requirement_candidate` 且返回 **`revisionId` 非空**（实际写入新版本），`MagAgentRunTaskContext` 已绑定 **`task_id`**；任务 **`IN_PROGRESS`** 且指派人为该产品 Agent——立即调用 **`tryAutoSubmitAfterProductRequirementMerge`**，**不依赖**整段 ReAct 结束时的最终回复是否非空。**不提供**人工 **阻塞**、**要活** HTTP 接口。

### 4.4 项目经理协助与告警

| 表名 | 说明 |
|------|------|
| **mag_pm_assist_record** | 协助记录：问题类型、根因摘要、动作、被协助 `agent_id` 列表 JSON、`resolved` |
| **mag_alert_event** | 大屏/通知：类型、项目、关联任务、级别、`payload_json`、`created_at`、是否已确认 |

### 4.5 需求文档与需求池

| 表名 | 说明 |
|------|------|
| **mag_requirement_doc** | 文档头：`project_id`、当前版本号 |
| **mag_requirement_revision** | 版本：`doc_id`、`version`、`content`（**MEDIUMTEXT**）、`author_user_id` 可空 |
| **mag_requirement_pool_item** | 需求池卡片：状态（PENDING_USER/CLOSED/…）、关联 `revision` 或锚点、`payload_json`；**可选** `assigned_decider_user_id`（空=凡具备 `mag:pool:decide` 的本项目成员均可见该待拍板项；非空=仅该用户 + OWNER 可见，见 §16.2） |

### 4.6 发版归档与知识库

| 表名 | 说明 |
|------|------|
| **mag_release_archive** | 发版记录：版本号、时间、`snapshot_json`（或 MinIO key）、`quality_flag`（是否优质回流） |
| **mag_kb_entry** | 知识库条目：`source`（ARCHIVE_REFLOW/MANUAL）、`title`、`body`、`tags`（JSON）、`keywords`、`archive_id` 可空；**FULLTEXT(title,body)** + 标签索引 |

### 4.7 定时任务配置（可选）

| 表名 | 说明 |
|------|------|
| **mag_scheduled_job_config** | `job_key`、`cron`、`enabled`、`project_id` 可空、`last_run_at` |

### 4.8 Agent 改进日志（产品 §5.3）

| 表名 | 说明 |
|------|------|
| **mag_agent_improvement_log** | `project_id`、`agent_id`、`change_type`（如 CONFIG、PROMPT、FEEDBACK、QUALITY_TAG）、`summary`（短文本）、`detail_json`（结构化前后差异、版本号等）、`created_by_user_id` 可空（系统写入则空）、`created_at`；**仅 INSERT** 审计型 |

**Flyway**：`backend/.../V3__mag_module.sql`、`V4__mag_permission_seed.sql` **已入库**（与下述骨架一致，**以仓库文件为准**）。

---

## 5. AgentScope Java 集成要点

### 5.1 与 `llm_channel` 的桥接

- **Temporal Activity**（`MagOrchestrationActivitiesImpl`）委托 **`MagAgentScopeRunService`**：按 `mag_agent.llm_channel_id` 与 **触发用户** 调用 `llm_channel`（`selectByIdAndOwner`），**解密 API Key**，按 `LlmProtocol` 构建 **`OpenAIChatModel`**（`baseUrl` + `endpointPath` 与 `LlmProviderCatalog` 一致，兼容各厂商 OpenAI 式网关）或 **`AnthropicChatModel`**，并组装 **`ReActAgent`**（`enableMetaTool(false)`，工具集在实现期按角色扩展）。
- **每个 `mag_agent` 行**可绑定 `llm_channel_id`；可按角色使用独立通道以便计费隔离。
- 对话体验接口仍走现有 **`LlmChatService` + RestTemplate**；与 AgentScope **并存**，边界为：编排侧用 AgentScope，Playground 侧可继续用直连客户端。

### 5.2 Agent 与工具（Tools）

- **职能 Agent**：工具可包括 — `searchOrgKb`（SQL LIKE + FULLTEXT）；`appendThreadMessage`、`updateTaskState`（受策略约束）等；**具体清单**在实现期按 **角色** 注册为 AgentScope **`Toolkit`**。**本期不包含**服务端代拉外网 URL 工具及 `mag_external_fetch_audit`。
- **记忆**：短期可用 AgentScope **Memory** 绑定 `thread_id`；长期摘要可写入 `mag_message`（SYSTEM）。

### 5.3 编排与 Temporal 的分工

- **单次多轮对话、工具循环**：AgentScope **`ReActAgent`** 在 **Activity** 内执行（`aiburst.mag.agentscope.max-iters` 与 `GenerateOptions.executionConfig.timeout` 约束）。
- **跨小时/天的等待**（等人拍板、等用户补充材料）：**Workflow 睡眠 + Signal**；不要在 LLM 层阻塞线程。

### 5.4 超时、重试与费用（首期默认，可配置）

- **单次 Activity（含 AgentScope ReAct 环）**： wall-clock 上限建议 **120s**（`aiburst.mag.agentscope.call-timeout-seconds`，与 LLM 调用对齐），硬超时由 Temporal Activity 选项覆盖；**max-iters** 见 `aiburst.mag.agentscope.max-iters`。
- **重试**：Transient 网络/5xx **指数退避**；**429/配额** 记入 `mag_alert_event`，可配置降级或转人工排障。
- **Token/费用预算**：项目级 `mag_project.config_json` 预留 `daily_token_budget` / `daily_cost_cap`（可选实现）；超出时 **拒绝新的 Agent 调用** 并写告警，**首期可仅记录用量不硬拦**。
- **死循环 / 互等**：同线程内工具步数上限 + Temporal Workflow 历史长度监控；超过阈值 **终止 Activity** 并记 FAIL + `mag_alert_event`。

---

## 6. Temporal 设计概要

### 6.1 工作流清单（首期）

| Workflow ID 模式 | 用途 |
|-------------------|------|
| `task-verify-{taskId}` | **（已废弃）** 历史占位；任务结项由 `submit-complete` 应用服务直写 `DONE`，不再使用独立核查 Workflow |
| `req-approval-{poolItemId}` | 需求池 **待用户拍板**：`await` **UserDecisionSignal** → Activity 更新池状态与需求修订 |
| `escalate-{threadId}` | 可选：阻塞升级链路与 SLA 超时合并 |

### 6.2 Activity 原则

- **查库、调 HTTP、调 AgentScope、发 WebSocket** 均在 Activity；**Workflow 代码无 IO**。
- **重试策略**：网络/5xx 指数退避；LLM 429/配额可配置降级（记录 `mag_alert_event`）。

### 6.3 与任务状态

- **DONE** 由申报完成路径写入（应用服务 + **乐观锁 `row_version`** 防并发）；与 Temporal 编排成功后的自动申报一致。

### 6.4 信号、载荷与计时器（首期约定）

| Workflow | Signal / Timer | 说明 |
|----------|----------------|------|
| **UserApprovalWorkflow** | **`UserDecisionSignal`** | JSON 建议字段：`decision`（APPROVE_CHANGE / APPROVE_AS_IS / REJECT 等枚举）、`userId`、`comment` 可选、`decidedAt`；由网关鉴权后注入 `userId` |
| **UserApprovalWorkflow** | 等待 Timer（可选） | 长期未拍板：**提醒**（写 `mag_alert_event` + 见 §16.3），不自动替用户决策 |

**本地开发**：Temporal Server 用 **Docker Compose**；Worker 与 API **同进程** 即可；集成测试可用 **Testcontainers**（实现期选型）。

---

## 7. 定时任务与 Redis 分布式锁

- **锁 Key**：`mag:sched:{jobKey}`（如 `mag:sched:stall-scan`），值 UUID，`SET NX EX` 或 Spring Integration RedisLock。
- **持锁时间**：略大于任务 P95；失败快速释放避免死锁。
- **典型 job**：停摆扫描（§5.5）、待拍板超时提醒、日聚合报表（可选）。

---

## 8. WebSocket（运营大屏）

- **路径建议**：`/ws/mag`；握手携带 **JWT**（Query 或 STOMP `Authorization`，以 Spring Security 方案为准）。
- **订阅**：客户端发送 `SUBSCRIBE project:{projectId}` 或 `SUBSCRIBE org`（全局看板，需 `mag:dashboard:org` 权限）。
- **推送事件**：Agent 心跳聚合、任务状态变更、`mag_alert_event` 新增；**Payload** 宜为瘦 JSON，详情走 REST。
- **背压**：服务端限频；前端断线重连指数退避。

### 8.1 消息信封（JSON 草案）

客户端 → 服务端（示例）：

```json
{ "op": "SUBSCRIBE", "channel": "project:1" }
{ "op": "UNSUBSCRIBE", "channel": "project:1" }
{ "op": "PING" }
```

服务端 → 客户端（示例）：

```json
{ "event": "task.state.changed", "projectId": 1, "payload": { "taskId": 2, "state": "DONE" } }
{ "event": "alert.new", "projectId": 1, "payload": { "alertId": 10, "level": "WARN" } }
{ "event": "agent.heartbeat", "projectId": 1, "payload": { "agentId": 3, "status": "BUSY" } }
```

`channel` 命名与 §8「订阅」一致；**详情字段**以瘦 payload 为准，完整对象走 REST。

---

## 9. REST API 清单（草案）

统一前缀 **`/api/mag`**，`ApiResult` 包装。下列与权限码 §10 绑定。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/projects` | 项目列表/创建 |
| GET/PUT/DELETE | `/projects/{id}` | 详情/更新/删除 |
| GET/POST | `/projects/{id}/members` | 成员 |
| GET/POST | `/projects/{id}/agents` | Agent 实例 |
| PUT | `/agents/{id}` | 更新 Agent 配置 |
| GET/POST | `/projects/{id}/threads` | 线程 |
| GET/POST | `/threads/{id}/messages` | 消息（触发 Agent 可由 POST message 或独立 `/run`） |
| GET/POST | `/projects/{id}/tasks` | 任务 |
| （服务内） | `submitComplete` | §4.3.1 **自动**结项（无对应 REST）；**无** `block` / `request-next` REST |
| GET/PUT | `/projects/{id}/requirement-doc` | 需求文档当前/保存新版本 |
| GET/POST | `/projects/{id}/requirement-pool` | 需求池 |
| POST | `/requirement-pool/{id}/decide` | 用户拍板 |
| GET/POST | `/kb/entries` | 知识库 CRUD（权限细分） |
| GET/POST | `/projects/{id}/releases` | 发版归档 |
| GET | `/dashboard/snapshot` | 大屏快照（轮询兜底，与 WS 并存可选） |
| GET | `/todos` | 当前用户待办聚合（拍板项） |
| GET/POST | `/projects/{id}/modules` | 功能模块树（与任务、需求锚点关联）；**产品 §5.2** |
| GET | `/projects/{id}/pm-assist` | **项目经理协助记录**列表与筛选；**产品 §5.6** |
| GET | `/projects/{id}/agents/{agentId}/improvements` | **改进日志**（可按时间分页）；**产品 §5.3** |
| GET/PUT | `/scheduled-jobs` | **定时任务配置**（全局或 `project_id` 过滤）；**产品 §5.4** |
| GET | `/projects/{id}/requirement-doc/revisions` | 需求版本列表；**产品 §6.1** |
| GET | `/projects/{id}/requirement-doc/diff` | 两版本正文对比（首期文本 diff）；**产品 §6.1** |
| POST | `/projects/{id}/requirement-change/analyze` | **变更影响分析**（异步任务 + 留痕，返回 jobId 或同步草案）；**产品 §6.2** |
| GET | `/projects/{id}/alerts` | 告警事件列表；`POST /alerts/{id}/ack` 确认已读；**产品 §5.5** |
| GET/PUT/DELETE | `/kb/entries/{id}` | 知识库单条维护；**产品 §5.8** |
| POST | `/projects/{id}/modules/import-blueprint` | 从归档快照或 KB 条目**受控复制**模块蓝图至本项目；**产品 §5.8** |
| POST | `/threads/{id}/run` 或 `/agents/{id}/run` | 可选：用户发消息后触发 Agent 编排（与 §5.2 AgentScope 对齐） |

上表中含 **§17** 新增列出的接口为**全量能力清单**组成部分；当前代码若未实现，须按迭代补齐，**不得从产品范围删除**。

**派工流水线（与产品 §4.2 对齐，后端已实现）**：`MagTaskDispatchGateService` 在 **`POST .../tasks/dispatch`、带指派执行人的任务创建、`POST .../pm-reassign`** 中校验：① `mag_requirement_revision` 最新正文 trim 后非空前，**仅**可向 `PRODUCT` 角色 Agent 指派新任务；② 存在指派给 `PRODUCT` 且 `state ∈ {PENDING,IN_PROGRESS,BLOCKED}` 的任务时，**禁止**再向任一 `PRODUCT` Agent 新建派工；③ 向 `FRONTEND` 指派前，须**无**上述未结项且指派给 `BACKEND` 的任务（**后端先于前端**）；④ 向 `TEST` 指派前，须**无**上述未结项且指派给 `FRONTEND` 或 `BACKEND` 的任务（**测试在前后端之后**）。主 Agent 工具 **`mag_ask_pm_for_next_tasks`** 同步门禁（`PRODUCT` 豁免①；`FRONTEND` 另受③约束；`TEST` 另受④约束）。失败返回 **409** 与 §19.7 码 **41016–41019**。

**OpenAPI 约定（实现期）**：SpringDoc 等暴露 `/v3/api-docs`；**分页** Query：`pageNum`（默认 1）、`pageSize`（默认 10，**上限 100**）；**错误体**沿用全局 `ApiResult`；**幂等**：部分写接口可带 `Idempotency-Key` 头（可选）。

---

## 10. 权限码（建议）

| perm_code | 说明 |
|-----------|------|
| `mag:project:list` | 可见项目列表 |
| `mag:project:manage` | 创建/编辑/删除项目、成员 |
| `mag:agent:manage` | 配置 Agent |
| `mag:task:operate` | 任务与申报 |
| `mag:req:edit` | 需求文档 |
| `mag:pool:decide` | 需求池拍板 |
| `mag:kb:manage` | 知识库维护 |
| `mag:release:archive` | 发版归档 |
| `mag:dashboard:view` | 项目大屏 |
| `mag:dashboard:org` | 组织级大屏 |

### 10.1 项目成员角色与权限默认矩阵（首期 Flyway 种子）

绑定在 **`sys_role` / 角色-权限** 或项目内逻辑均可，以下为 **默认推荐**（组织可增删权限码覆盖）。

| perm_code | OWNER | MEMBER | VIEWER |
|-----------|:-----:|:------:|:------:|
| `mag:project:list` | ✓ | ✓ | ✓ |
| `mag:project:manage` | ✓ | ✗ | ✗ |
| `mag:agent:manage` | ✓ | ✓ | ✗ |
| `mag:task:operate` | ✓ | ✓ | ✗ |
| `mag:req:edit` | ✓ | ✓ | ✗ |
| `mag:pool:decide` | ✓ | ✓ | ✗ |
| `mag:kb:manage` | ✓ | ✗ | ✗ |
| `mag:release:archive` | ✓ | ✓ | ✗ |
| `mag:dashboard:view` | ✓ | ✓ | ✓ |
| `mag:dashboard:org` | ✓ | ✗ | ✗ |

菜单 Flyway：在「多 Agent 协作」目录下挂子菜单（项目列表、工作台、需求池、知识库、大屏等）。

---

## 11. 前端（Vue 3）

- **路由**：`/mag/projects`、`/mag/project/:id`（工作台 Tab：成员、Agent、任务、沟通、需求、需求池、归档）、`/mag/dashboard/:projectId?`、`/mag/todos`。
- **WebSocket**：`useMagWebSocket()` composable，Pinia 存订阅状态与最近快照。
- **大屏**：ECharts 或轻量卡片；数据以 WS 增量 + 首屏 REST。

---

## 12. 安全与非功能

- **隔离**：所有查询带 `project_id` 与成员校验；Agent 运行上下文 **禁止** 注入其他 `project_id`。
- **外网抓取**：**本期不建设**服务端代拉 URL 与审计表；若二期引入出站抓取，须另立 SSRF 黑名单、超时与字节上限等方案。
- **密钥**：沿用 LLM 方案 AES；Temporal Payload 中**不存** apiKey。
- **观测**：Micrometer（Temporal、LLM 调用耗时、WS 连接数）；关键业务日志带 `projectId`、`taskId`、`workflowId`。

---

## 13. 配置项（示例）

| Key | 说明 |
|-----|------|
| `aiburst.mag.temporal.target` | Temporal 前端地址 |
| `aiburst.mag.temporal.namespace` | 默认 `default` |
| `aiburst.mag.redis.lock-prefix` | 锁前缀 |
| `aiburst.mag.kb.reflow-mode` | **已确认 `auto`**（`quality_flag=1` 自动入库）；保留 `manual_review` 供将来回滚 |
| `aiburst.mag.notify.qq.enabled` | 是否启用 **QQ 机器人** 外呼 |
| `aiburst.mag.notify.qq.webhook-url` | QQ Bot / 中间层 **HTTP 回调地址**（具体协议与签名见实现 README） |
| `aiburst.mag.notify.qq.detail-base-url` | 可选；告警详情页面前缀，拼进 QQ 消息链接 |
| `aiburst.mag.notify.qq.secret` | 可选；`X-Aiburst-Notify-Secret` 静态密钥 |

**Docker**：仓库根目录已提供 [docker-compose.temporal.yml](../docker-compose.temporal.yml)（`temporal` + `temporal-postgresql` + `temporal-ui`，PostgreSQL 宿主机端口 **5435**）；应用连接 Temporal 前端 **`localhost:7233`**。镜像版本随升级可改；与本文 **Temporal Docker** 决策一致（§19.6）。

---

## 14. 实施阶段建议（排期不缩减产品范围）

下列顺序为**工程落地建议**；产品 **§2.1 能力清单不缩减**（见产品 §2 交付策略），**§17** 为全量设计对照。某阶段未上线的能力须在 backlog 明确，**不得在技术文档中删除对应条目**。

1. **Boot 3.5 + JDK 24**（与当前仓库一致）+ LLM/权限冒烟。  
2. **Flyway V3** 建表 + 项目/成员/Agent/Task **CRUD API**（含 §9 已列路径的增量实现）。  
3. **AgentScope Java** 桥接通道 + 单线程对话 MVP。  
4. **Temporal** 接入（编排 Activity；任务结项由应用服务 `submit-complete`）。  
5. **WebSocket** 大屏 + 告警事件。  
6. 需求池、升级链、定时 Redis 锁 job；**要活/阻塞/协助记录/改进日志/模块树**等 §9 增补接口。  
7. 发版归档与知识库回流；**变更影响分析**与**版本 diff**；**蓝图引用** API。

---

## 15. DDL 草案（Flyway `V3__mag_module.sql` 骨架）

以下为 **MAG 库表在文档中的合并表述**：**`V3__mag_module.sql`** 为初始建表；**`V11__remove_task_verification.sql`** 删除 `mag_task_verification`；**`V12__remove_external_fetch_audit.sql`** 删除 `mag_external_fetch_audit` 及 `mag:audit:fetch:view` 权限。下列 DDL **已按 V3+V11+V12 后的有效结构整理**（字段长度、索引仍以仓库内迁移脚本为最终准绳）。与 `V1`/`V2` 风格一致（InnoDB、utf8mb4）。**外键**可选（先建表后加 FK 避免顺序问题）。**权限种子**见 `V4__mag_permission_seed.sql`（§19.6）。

```sql
-- 项目
CREATE TABLE mag_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active 0 archived',
    config_json JSON NULL COMMENT 'thresholds, feature flags',
    current_req_doc_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_project_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_in_project VARCHAR(32) NOT NULL COMMENT 'OWNER, MEMBER, VIEWER（不设仅拍板专用角色）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_proj_user (project_id, user_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Agent：parent_id 主/子（历史 VERIFY 已由 V11 迁移为 TEST，新库不应再写入 VERIFY）
CREATE TABLE mag_agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    parent_agent_id BIGINT NULL,
    role_type VARCHAR(32) NOT NULL COMMENT 'PM,PRODUCT,BACKEND,FRONTEND,TEST',
    name VARCHAR(128) NOT NULL,
    llm_channel_id BIGINT NULL COMMENT 'llm_channel.id',
    system_prompt_profile VARCHAR(64) NULL,
    extra_json JSON NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_project (project_id),
    KEY idx_parent (parent_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_agent_improvement_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL COMMENT 'CONFIG,PROMPT,FEEDBACK,QUALITY_TAG,...',
    summary VARCHAR(512) NOT NULL,
    detail_json JSON NULL,
    created_by_user_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent (agent_id, created_at),
    KEY idx_project (project_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_module (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(256) NOT NULL,
    tag VARCHAR(64) NULL,
    sort_order INT DEFAULT 0,
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    module_id BIGINT NULL,
    title VARCHAR(256) NOT NULL,
    description TEXT NULL,
    state VARCHAR(32) NOT NULL COMMENT 'PENDING,IN_PROGRESS,DONE,BLOCKED',
    assignee_agent_id BIGINT NULL,
    reporter_agent_id BIGINT NULL,
    requirement_ref JSON NULL,
    temporal_workflow_id VARCHAR(256) NULL,
    block_reason VARCHAR(512) NULL,
    blocked_by_agent_id BIGINT NULL,
    row_version INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_project_state (project_id, state),
    KEY idx_assignee (assignee_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_thread (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    title VARCHAR(256) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thread_id BIGINT NOT NULL,
    sender_type VARCHAR(16) NOT NULL COMMENT 'USER,AGENT,SYSTEM',
    sender_agent_id BIGINT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_thread (thread_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_requirement_doc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL UNIQUE,
    current_version INT NOT NULL DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_requirement_revision (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id BIGINT NOT NULL,
    version INT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    author_user_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_ver (doc_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_requirement_pool_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    state VARCHAR(32) NOT NULL,
    revision_id BIGINT NULL,
    anchor_json JSON NULL,
    payload_json JSON NULL,
    assigned_decider_user_id BIGINT NULL COMMENT '空=具备 mag:pool:decide 的成员可见；非空=仅该用户与 OWNER',
    temporal_workflow_id VARCHAR(256) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_project_state (project_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_release_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    version_label VARCHAR(64) NOT NULL,
    snapshot_json JSON NULL,
    minio_object_key VARCHAR(512) NULL,
    quality_flag TINYINT NOT NULL DEFAULT 0 COMMENT '1 eligible for kb reflow',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_kb_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source VARCHAR(32) NOT NULL COMMENT 'ARCHIVE_REFLOW,MANUAL',
    archive_id BIGINT NULL,
    title VARCHAR(256) NOT NULL,
    body MEDIUMTEXT NOT NULL,
    tags_json JSON NULL,
    keywords VARCHAR(512) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FULLTEXT KEY ft_title_body (title, body),
    KEY idx_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_pm_assist_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    problem_type VARCHAR(64) NULL,
    root_cause_summary TEXT NULL,
    action_taken TEXT NULL,
    assisted_agent_ids_json JSON NULL,
    resolved TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_alert_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NULL,
    task_id BIGINT NULL,
    alert_type VARCHAR(64) NOT NULL,
    level VARCHAR(16) NOT NULL DEFAULT 'INFO',
    payload_json JSON NULL,
    acknowledged TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_time (project_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_scheduled_job_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_key VARCHAR(64) NOT NULL UNIQUE,
    cron_expr VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    project_id BIGINT NULL,
    last_run_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明**：`mag_project.current_req_doc_id` 与 `mag_requirement_doc` 的回填可在同一迁移脚本末尾用 `UPDATE` 或应用启动脚本处理。  
**任务结项**：由应用服务 `submit-complete`（及可选自动申报监听器）将 `IN_PROGRESS` 置为 `DONE`，**无**独立核查子表与核查方字段。  
**需求正文**：`mag_requirement_revision.content` 使用 **MEDIUMTEXT** 直接落库；单版本体量若长期逼近上限再评估分块或 MinIO（**已确认首期坚持 MEDIUMTEXT**）。

---

## 16. 实现期默认约定（闭合产品 §11 中可由技术拍板的项）

### 16.1 组织知识库与归档回流

- **准入**：`mag_release_archive.quality_flag = 1` 表示**候选**优质；**已确认**：由定时或发版后任务 **自动** 生成 `mag_kb_entry`（`source=ARCHIVE_REFLOW`，可附 `archive_id`）。配置 **`aiburst.mag.kb.reflow-mode=auto`**（默认）；保留 `manual_review` 供将来回滚。
- **检索**：首期 **FULLTEXT + 标签/关键词**；组织知识库检索范围 = 组织可见条目 + 本项目授权。

### 16.2 待办与需求池读模型

- **`GET /api/mag/todos`**：聚合 `mag_requirement_pool_item` 中 `state = PENDING_USER`（与产品「待用户拍板」一致），过滤条件：
  1. 当前用户为该项目成员；
  2. 具备 `mag:pool:decide`；
  3. `assigned_decider_user_id IS NULL` **或** `= 当前 userId` **或** 用户为该项目 **OWNER**。
- 需求池列表 API 使用**相同过滤**；**不另建**待办影子表，必要时加缓存。

### 16.3 通知与告警（首期）

- **触达**：**WebSocket** 推送（§8.1 `alert.new` 等）+ 持久化 **`mag_alert_event`**；**已确认**可接 **QQ 机器人**：由独立适配模块订阅告警事件（或轮询 `mag_alert_event`），按 **`aiburst.mag.notify.qq.*`** 调用 Webhook；鉴权/签名与具体 Bot 框架（如 go-cqhttp、官方 Bot API 等）在实现 README 约定。**邮件/企微** 等平台消息中心 **可选复用**，不阻塞首期。
- **订阅**：大屏用户即 WS 订阅者；**静默时段**放入 `mag_project.config_json` 可选字段，默认不启用。

### 16.4 大屏「产出效率」（首期）

- **数据源**：以 **`mag_task` 状态迁移时间戳**（及可选 `mag_task_state_log`）为主：**周期内进入 DONE 的数量**、**平均停留时长**；**不包含** MinIO/代码托管 unless 二期对接。

### 16.5 外网检索

- **本期**：不实现服务端代拉外网 URL，**无** `mag_external_fetch_audit`；检索结论由 Agent 写入 **需求池 `payload_json`**、**线程消息** 等。

### 16.6 数据生命周期与灾备（已确认）

- **保留**：`mag_message`、`mag_agent_improvement_log` 等 **永久保留**，**不配置自动 TTL 物理删除**；容量与归档到冷存储若日后需要，另案。
- **备份**：归档与知识库 **跟随平台 MySQL 备份策略**（RPO/RTO **对齐运维基线**）。**法务删除/被遗忘权** 若适用，按合规单独立项实现。

---

## 17. 产品线需求全量追溯与实现映射（不省略）

本节与《多Agent协作与项目管理产品线需求》**全文对照**：产品 **§2.1 所列模块均为本期建设范围、能力清单不缩减**（见产品 §2 交付策略）；技术方案须**覆盖**下列全部能力的设计落点；若代码尚未实现某 API/页面，以本文 **§9** 与下表为**待实现清单**，不得视为「产品未要求」。

### 17.1 编制说明

- **追溯粒度**：产品正文**段落级/列表项级**均在 §17.2～§17.11 中有对应；§2.1 **表格逐行**在 §17.3 **不合并、不省略**。  
- **与产品 §11 关系**：产品 §11 所列「由技术方案细化」的条目，在 **§17.11** 给出已定稿口径；与 §0/§12/§18 **工程决策**冲突时，以 **§18 已确认决策**为准并在 §17.11 显式说明。

### 17.2 与产品 §1「产品目标」逐条响应

| # | 产品目标（摘要） | 技术设计与落点 |
|---|------------------|----------------|
| 1 | 录入/配置/启停各类 Agent，同项目协同 | `mag_agent` + `llm_channel_id`；`PUT /agents/{id}`；AgentScope 运行时按实例加载；`status` 启停 |
| 2 | 项目级工作台：Agent 列表、沟通、任务状态 | 前端 §11 路由；REST §9 `projects`、`agents`、`threads`、`messages`、`tasks` |
| 3 | 主/子层级；子完成后申领；主向 PM 拉活；PM 按模块进度拆解下发 | `parent_agent_id`；**要活** `POST /tasks/{id}/request-next`（或等价的线程消息 + 调度消费，见 §9）；PM Agent 工具 `assignTask` / 任务表更新 |
| 4 | 全量记录 Agent 行为与改进轨迹；定时任务触发 | `mag_agent_improvement_log`；`mag_scheduled_job_config` + §7 |
| 5 | 需求文档为事实来源之一；变更识别受影响点并重排 | `mag_requirement_revision`；**变更影响** `POST /projects/{id}/requirement-change/analyze`（异步 + 留痕 `payload_json`/审计表，见 §9）；驱动任务状态建议 |
| 6 | 运营大屏：Agent 是否干活、产出效率、告警 | §8 WS + `mag_alert_event`；指标 §16.4 |
| 7 | 派工无法落地 → 执行方向 PM 说明；PM 协助留痕；升级产品；知识库+成熟产品检索；窄口径真人拍板 | `mag_message` 结构化阻塞；`mag_pm_assist_record`；升级 Workflow `escalate-*`；池 §5.6 与 §6.3 |
| 8 | 发版归档；项目隔离；新项目只读/受控引用归档经验 | `mag_release_archive`；§12 隔离；`mag_kb_entry` + 模块蓝图 API |
| 9 | 组织知识库：归档回流 + 人工录入 | `mag_kb_entry`；§16.1 |
| 10 | 真人处理待用户拍板；待办与/或需求池可达 | `GET /todos`；需求池列表 §16.2；RBAC §10 |
| 11 | 申报完成后任务进入已完成（DONE） | §4.3.1 状态机；`POST /tasks/{id}/submit-complete` 与自动申报 |

### 17.3 与产品 §2.1「本期包含」模块表（全行，不省略）

| 产品模块 | 技术落点（库表 / 服务 / API / 前端 / 编排） |
|----------|-----------------------------------------------|
| **Agent 管理** | `mag_agent`（实例）；模板与「能力描述」放 `extra_json` / 后续 `mag_agent_template` 表（若单表不足则二期拆表，**首期实例级配置须可验收**）；`GET/POST /projects/{id}/agents`、`PUT /agents/{id}`；工作台 Agent Tab |
| **项目与成员视图** | `mag_project`、`mag_project_member`；项目 CRUD §9；成员 **不设仅拍板角色**（§0）；Agent 列表与主从 `parent_agent_id`；健康度/心跳：`extra_json` 或 Redis 心跳键 + 大屏聚合 |
| **组织知识库** | `mag_kb_entry`；来源 `ARCHIVE_REFLOW` / `MANUAL`；`GET/POST /kb/entries` 及单条 GET/PUT/DELETE；产品检索工具 `searchOrgKb` §5.2 |
| **用户待办** | `GET /todos` 聚合 `mag_requirement_pool_item.state=PENDING_USER` + §16.2 过滤；与需求池 **同一读模型**，前端 **待办菜单 + 需求池 Tab 双向跳转**（产品 §5.6.2） |
| **协作与沟通可观测** | `mag_thread`、`mag_message`；按项目/Agent/时间筛选：`GET /threads` 带 query（`agentId`、`from`、`to`）；权限脱敏：服务端按角色裁剪 `content` 或字段级 |
| **协调链** | 编排侧：PM Agent 与主 Agent 工具与任务表；消息层记录「请求派工、阻塞、澄清」；**不省略**主→PM、主→子 的会话类型（可用 `message` 内 JSON `type` 约定） |
| **任务与申领** | `mag_task`；子 Agent **要活** `POST /tasks/{id}/request-next`（写入消息队列或 `mag_message` SYSTEM 事件 + PM/主 Agent 消费）；PM 按 `mag_module` 与任务状态派工 |
| **任务结项** | `IN_PROGRESS` → `DONE`（申报完成）；§4.3.1；编排工具不绕过 HTTP 权限直改状态 |
| **改进记录** | `mag_agent_improvement_log`；`GET /projects/{id}/agents/{agentId}/improvements` 或 query 参数过滤；导出 CSV 可选 |
| **定时任务** | `mag_scheduled_job_config`；`GET/PUT /scheduled-jobs`（权限 `mag:project:manage` 或独立 `mag:sched:manage` 种子另议）；执行写时间线 `mag_message` 或 `mag_alert_event` |
| **需求文档中心** | `mag_requirement_doc`、`mag_requirement_revision`；版本列表 `GET .../requirement-doc/revisions`；对比 `GET .../requirement-doc/diff`（首期文本 diff）；**评审状态**可放 `doc` 扩展字段或 `revision` 旁路 JSON（实现期二选一，**须在 Flyway 落字段**） |
| **运营大屏** | §8；`GET /dashboard/snapshot`；Agent 状态空闲/执行中/等待/阻塞/离线来自心跳+任务绑定；告警 §5.5 与 `mag_alert_event` |
| **阻塞与升级链路** | 阻塞 `block_reason`、`blocked_by_agent_id`；`mag_pm_assist_record`；升级 `mag_alert_event` + Workflow；产品 Agent 闭环写池 `payload_json`（检索摘要、成熟产品标识、对比表）；**仅窄口径** `PENDING_USER` |
| **需求池** | `mag_requirement_pool_item`；状态全集 §19.1 扩展；与需求锚点 `anchor_json`；`POST .../decide`；已闭环记录可查 |
| **发版归档与发布版本** | `mag_release_archive`；快照 JSON 含需求基线、模块清单、任务结论；**只读**不可改历史行 |
| **组织经验与模块复用** | 回流 §16.1；新项目 `POST /projects/{id}/modules/import-blueprint`（来源 `archive_id` 或 `kb_entry_id`，复制后归本项目）；检索授权 §7 |

### 17.4 与产品 §3「五类 Agent」

| 产品类型 | `role_type` | 技术要点 |
|----------|-------------|----------|
| 项目经理（PM）Agent | `PM` | 派工工具、读 `mag_module`/任务；写 `mag_pm_assist_record`；升级链路入口 |
| 产品 Agent | `PRODUCT` | 维护需求修订；`searchOrgKb`；写需求池 `payload_json`（对比择优、检索留痕） |
| 后端开发 Agent | `BACKEND` | 子 Agent `parent` 指向主后端实例 |
| 前端开发 Agent | `FRONTEND` | 同上 |
| 测试 Agent | `TEST` | `mag_record_unit_test_plan` 等测试侧工具；任务申报完成与其余职能一致 |

### 17.5 与产品 §4 主/子、协调、阻塞、可观测（§4.1～§4.4）

- **§4.1～§4.2**：主/子、`parent_agent_id`；主向 PM 请求任务 → 消息 + 任务表更新；界面展示待派发/进行中/阻塞（任务查询 API 分组或前端聚合）。
- **§4.3**：阻塞须 **主动消息**（`mag_message` + 建议 `content` 内 JSON：`{ "kind":"BLOCK", "reasonCode", "summary" }`）；**每次** PM 协助写入 `mag_pm_assist_record`（`assisted_agent_ids_json`、`resolved`）。
- **§4.4**：线程时间线；用户 `@Agent` → `sender_type=USER` + 解析提及 AgentId 存 `payload`；权限控制读接口。
### 17.6 与产品 §5.1～§5.8（项目管理功能）

| 小节 | 需求要点 | 技术响应 |
|------|----------|----------|
| **§5.1** | 列表：Agent 数、最近活动、需求版本 | 列表接口聚合 `COUNT(agent)`、`MAX(message.created_at)`、`current_req_doc_id`/version；详情页 Tab 与跳转待办 |
| **§5.2** | 状态：待派发/进行中/已完成/阻塞 | 与 §4.3.1 一致 |
| **§5.3** | 改进日志可检索、导出 | §17.2 改进记录行 |
| **§5.4** | 定时任务可配置、结果可见 | `mag_scheduled_job_config` + 执行日志痕迹 |
| **§5.5** | 大屏实时、Agent 状态、效率、告警类型 | WS 事件 + §16.4；`STALL`、`PM_ESCALATION` 等 `alert_type` |
| **§5.6～5.6.2** | 升级顺序、协助记录、待办入口 | 工作流 + 表；§16.2 |
| **§5.6.1** | 成熟产品四条件、不命中情形 | 池 `payload_json` 等字段；**过滤规则**（排除 Demo）配置项 `aiburst.mag.product-search.min-signal`（实现期命名可调） |
| **§5.7** | 归档内容清单 | `snapshot_json` **模式**（JSON Schema 实现期固定）：含方案对比、协调问题、停滞原因、协助摘要、模块标签等键 |
| **§5.8** | 隔离与受控共享 | §12；跨项目 **禁止** 默认带 `project_id` 外键写入；蓝图复制显式 API + 审计 |

### 17.7 与产品 §6「需求文档与变更驱动」

- **§6.1**：富文本首期可用 Markdown/MEDIUMTEXT；**版本列表与对比** API 见 §9；协作冲突「保存即新版本」。
- **§6.2**：变更影响分析 **Agent 主导 + 留痕**；真人确认仅 §5.6 窄口径；输出受影响 `module_id`/`task_id` 列表与建议动作写入审计或 `mag_message` SYSTEM。
- **§6.3**：需求池状态扩展 **§19.1**；`payload_json` 建议 Schema 键：`problem`、`kbSummary`、`fetchSummary`、`matureProductHits[]`、`compareMatrix`、`chosenOption`、`rationale`、`pendingUserDecision`（仅 `PENDING_USER`）；用户拍板后追加 `userDecision` 与时刻。

### 17.8 与产品 §7「权限与安全」

- RBAC `mag:*` + 项目成员校验 **双因子**（所有 `projectId` 接口）；组织知识库 **引用/复制蓝图** 建议单独权限码 **`mag:kb:blueprint:import`**（Flyway 种子与 §10 表同步增补），或暂由 `mag:kb:manage` 与 `mag:project:manage` 组合代行（实现期二选一并在 §10 明示）；跨项目引用须 **留痕**（消息/池项/归档等）。
- 脱敏：消息查询接口按角色掩码敏感字段。

### 17.9 与产品 §8「非功能」

- 可追溯性：列出的各实体均可由 `project_id`/`task_id` 关联查询；大屏与池、归档、协助均已表化。
- 性能：消息分页、线程归档策略（冷数据表可选）。
- 可靠性：定时任务失败 → `mag_alert_event` + 可选重试字段于 `mag_scheduled_job_config`。

### 17.10 与产品 §9「验收要点」→ 技术侧验证映射

产品 §9 **每条验收表述**对应下表一行（**不合并、不省略**）。

| # | 产品 §9 原文要点 | 技术验证方式（摘要） |
|---|------------------|----------------------|
| 1 | 可创建项目并新增多类 Agent（含主/子），项目内见列表与状态 | `POST/GET .../projects`、`.../agents`；DB `mag_project`、`mag_agent.parent_agent_id` |
| 2 | 可观察到 Agent 间沟通记录 | `GET .../threads`、`.../messages`；`sender_type=AGENT` 可查 |
| 3 | 子 Agent 完工触发「要活」；主向 PM 请求派工；PM 按模块与进度更新分配 | `POST .../tasks/{id}/request-next` + 消息或编排；`mag_module` + 任务 `assignee`/`state` 变更审计 |
| 4 | 申报完成后任务为已完成（DONE）；各职能一致 | 状态机 §4.3.1；`POST .../submit-complete` |
| 5 | 改进记录可查询 | `GET .../improvements` + `mag_agent_improvement_log` |
| 6 | 定时任务按配置执行并在界面留痕 | `mag_scheduled_job_config` + 执行写 `mag_message` 或 `mag_alert_event` |
| 7 | 需求文档可编辑并新版本；变更后展示受影响功能点并驱动重排 | `PUT .../requirement-doc`；`POST .../requirement-change/analyze` 输出与任务建议留痕 |
| 8 | 仅 5.6 窄口径须用户拍板后落库 | 池状态 `PENDING_USER` + `decide` API + Workflow Signal |
| 9 | 运营大屏展示是否在干活与产出效率；停摆与 PM 无法协调可见告警 | §8 事件 + `GET .../dashboard/snapshot` + `mag_alert_event` |
| 10 | 派工后无法执行可向 PM 说明原因；可查 PM 协助了哪些问题与哪些 Agent | `POST .../block` + `mag_pm_assist_record` + 消息 |
| 11 | 升级产品后：命中成熟产品时已闭环记录符合 5.6.1；多竞品有对比择优留痕 | 池 `payload_json` 键完整 |
| 12 | 仅窄口径产生待用户拍板；已登录用户在待办与/或需求池可处理 | `GET /todos` + 需求池列表 §16.2；待办聚合他类为可选 |
| 13 | 组织知识库可见归档回流与人工录入；产品 Agent 可检索 | `mag_kb_entry` + 工具 `searchOrgKb` |
| 14 | 发版生成归档，含协调问题、停滞原因与经验摘要；项目隔离；新项目可引用/复制蓝图 | `mag_release_archive` + `POST .../import-blueprint` + §12 隔离校验 |

### 17.11 与产品 §11「建议在技术方案补齐的要点」— 本文已定稿

| 产品 §11 类别 | 技术已定稿 |
|----------------|------------|
| 组织知识库实现细节 | §16.1 准入与回流；首期 FULLTEXT；**置信度阈值** `mag_project.config_json.kbConfidenceThreshold`（可选）；二期向量 §16.1 |
| 真人成员与待办 | §16.2；成员枚举 §19.1；不设仅拍板角色 §0 |
| 通知与告警订阅 | §16.3；订阅者 = WS 连接 + 角色 `mag:dashboard:view`；事件：拍板创建、告警、`POOL_REMINDER`；静默 `config_json.quietHours` |
| 产出物与效率指标 | §16.4；首期以任务状态时间戳为主；**交付物更新频次** 可用 `mag_message`/`task.updated_at` 代理，MinIO 二期 |
| Agent 编排与韧性 | §5.4；预算 `config_json`；死循环告警 §5.4 |
| 外部检索合规 | **本期无出站抓取与审计表**；合规与引用规范在需求池/消息正文与人工编辑需求文档中体现；若二期引入代拉 URL，再单列 SSRF 与版权口径 |
| 数据生命周期 | §16.6 |
| 灾难恢复 | §16.6 RPO/RTO 跟随平台 |

### 17.12 与产品 §2.2「本期不包含或后续迭代」— 技术边界

以下**明确不在本期产品范围**（见产品需求 §2.2），技术方案**不**在 §2.1 全量清单中要求实现：

| 产品表述 | 技术边界 |
|----------|----------|
| 与真实 IM（企业微信、Slack）等**双向深度集成** | 可用导出/Webhook/QQ 机器人等替代，见 §16.3 |
| **自动代码合并到生产** | 不包含；CI 对接为扩展 |
| **跨租户项目协作** | 不包含；租户隔离另立需求 |

---

## 18. 已确认决策记录（业务答复归档）

| # | 议题 | 结论 |
|---|------|------|
| 1 | `mag_message` / `mag_agent_improvement_log` 保留 | **永久保留**（不自动按天删除） |
| 2 | `quality_flag=1` → 知识库 | **自动**写入 `mag_kb_entry` |
| 3 | 站外通知 | **可接 QQ 机器人**（Webhook 等，见 §13 与 §16.3）；与 WS/DB 并存 |
| 4 | 外网检索 | **本期不实现**服务端代拉与 `mag_external_fetch_audit` |
| 5 | URL/host 白名单 | **不适用**（无本期出站抓取） |

后续若变更上述口径，请在本表追加行并注明日期与评审记录。

---

## 19. 实现附录（枚举、工程布局、OpenAPI、QQ、仓库文件）

### 19.1 库内枚举（`VARCHAR` 建议值，与前后端常量对齐）

| 字段/场景 | 取值 | 说明 |
|-----------|------|------|
| `mag_task.state` | `PENDING`, `IN_PROGRESS`, `DONE`, `BLOCKED` | 见 §4.3.1（历史库可能仍有旧状态值，迁移见 Flyway V11） |
| `mag_agent.role_type` | `PM`, `PRODUCT`, `BACKEND`, `FRONTEND`, `TEST` | 与产品五类 |
| `mag_message.sender_type` | `USER`, `AGENT`, `SYSTEM` | |
| `mag_requirement_pool_item.state`（**与产品 §6.3 对齐，全量**） | `PENDING_USER`（待用户拍板，**仅 5.6 窄口径**）；`CLOSED_BY_PRODUCT`（已闭环-产品 Agent，可审计）；`USER_CONFIRMED_OK`；`USER_CONFIRMED_CHANGE`；`USER_REJECTED`；`CLOSED`（关闭）；`USER_CHANGE_REQUESTED`（用户要求修订，与拍板信号映射）；另有实现期可增：`COMPARING_OPTIONS`（多方案对比中，可选） | **禁止**用状态缩略导致产品 §6.3 任一分支无法落库；**前后端同表** |
| `UserDecisionSignal.decision`（Temporal） | `APPROVE_AS_IS`, `APPROVE_WITH_CHANGE`, `REJECT`, `DEFER` | 实现期可删减；落库映射到池状态 |
| `mag_alert_event.level` | `INFO`, `WARN`, `ERROR` | |
| `mag_alert_event.alert_type`（建议） | `STALL`, `PM_ESCALATION`, `TASK_BLOCKED`, `ORCH_ACTIVITY_FAILED`, `POOL_REMINDER`, `SYSTEM` 等 | 可扩展；以代码实际 `raise` 为准 |
| `mag_kb_entry.source` | `ARCHIVE_REFLOW`, `MANUAL` | |
| `mag_agent_improvement_log.change_type` | `CONFIG`, `PROMPT`, `FEEDBACK`, `QUALITY_TAG`, `OTHER` | |
| `mag_project_member.role_in_project` | `OWNER`, `MEMBER`, `VIEWER` | |

### 19.2 OpenAPI 与分页

- **生成**：Spring Boot 侧引入 **springdoc-openapi-starter-webmvc-ui**，分组 `mag` 仅扫描 `com.aiburst.mag.**`（实现时建包）。文档路径默认 **`/v3/api-docs`**，UI **`/swagger-ui.html`**（以实际 Boot 版本为准）。
- **草案文件**：仓库 [技术方案/mag-openapi-stub.yaml](mag-openapi-stub.yaml) 提供 **路径/分页/幂等头** 的 OpenAPI 3.0 片段，实现期与 SpringDoc 生成结果对齐或合并。
- **分页**：`pageNum`（≥1）、`pageSize`（1～100）；列表响应建议 `{ "list": [], "total": n }` 包在 `ApiResult.data` 内，与现有前端习惯对齐。
- **幂等**：`Idempotency-Key`（UUID）可选，Redis 或 DB 去重 TTL 24h，键名 `mag:idemp:{method}:{path}:{key}`。

### 19.7 `mag` 业务错误码（`ApiResult.code` 建议区间 **41000–41999**）

与全局 `ResultCode`（0、400、401…）并存：**HTTP 状态** 仍按语义返回（401/403/404/409/502 等），**body 内 `code`** 使用下表 **便于前端分支与日志检索**。实现可枚举 `MagResultCode` 或在 `GlobalExceptionHandler` 映射。

| code | 符号（日志/监控） | 典型 HTTP | 说明 |
|------|-------------------|-----------|------|
| 41001 | `MAG_FORBIDDEN` | 403 | 已登录但无本项目或本接口 `mag:*` 权限 |
| 41002 | `MAG_NOT_PROJECT_MEMBER` | 403 | 非项目成员访问带 `projectId` 的资源 |
| 41003 | `MAG_NOT_FOUND` | 404 | 项目/任务/线程/需求/池项等不存在或已删 |
| 41010 | `MAG_TASK_STATE_INVALID` | 409 | 任务状态不允许当前操作（如非 IN_PROGRESS 却申报完成） |
| 41011 | `MAG_ROW_VERSION_CONFLICT` | 409 | `row_version` 乐观锁冲突，客户端应刷新后重试 |
| 41013 | `MAG_POOL_DECIDE_NOT_ALLOWED` | 403 | 无 `mag:pool:decide` 或不符合 `assigned_decider_user_id` 规则 |
| 41014 | `MAG_POOL_STATE_INVALID` | 409 | 需求池项非待拍板态却调用 decide |
| 41016 | `MAG_DISPATCH_REQUIREMENT_NOT_READY` | 409 | 需求正文未就绪却向非产品派工，或非产品主 Agent 调用 `mag_ask_pm_for_next_tasks` |
| 41017 | `MAG_DISPATCH_PRODUCT_PIPELINE_BLOCKED` | 409 | 产品职能尚有未结项任务时再次向产品 Agent 派工 |
| 41018 | `MAG_DISPATCH_TEST_BLOCKED_BY_DEV` | 409 | 前端或后端尚有未结项任务时向测试派工，或测试主 Agent 向 PM 要活 |
| 41019 | `MAG_DISPATCH_FRONTEND_BLOCKED_BY_BACKEND` | 409 | 后端尚有未结项任务时向前端派工，或前端主 Agent 向 PM 要活 |
| 41020 | `MAG_TEMPORAL_START_FAILED` | 502 | 启动/信号 Workflow 失败（Temporal 不可用或参数非法） |
| 41021 | `MAG_TEMPORAL_QUERY_TIMEOUT` | 504 | 等待 Workflow/Activity 结果超时（若暴露同步接口） |
| 41999 | `MAG_UNKNOWN` | 500 | 未分类域错误（应记日志并迭代补码） |

**约定**：成功仍为 `code=0`；与现有 `ResultCode` 重复的 400/401/403 等可继续直接用全局码，**域内细分**优先用 41xxx。

### 19.8 SpringDoc 配置片段（实现期粘贴）

**`application.yml`（节选）**

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
  group-configs:
    - group: mag
      display-name: MAG — 多 Agent
      packages-to-scan:
        - com.aiburst.mag
```

**说明**：`com.aiburst.mag` 包在实现 Controller 时创建；未引入 springdoc 依赖前勿启动校验本段。

### 19.3 Temporal（Java）包结构与 Workflow Id

建议包根：`com.aiburst.mag.temporal`

| 类型 | 类名示例 | 说明 |
|------|-----------|------|
| Workflow | `UserApprovalWorkflow`（及编排类 Workflow，实现期命名） | 接口 + Impl；**无 IO** |
| Activity | `MagOrchestrationActivities` 等 | `@ActivityMethod`；DB/LLM/WS |
| 启动器 | `MagWorkflowClient` | 注入 `WorkflowClient`，封装 `req-approval-{poolItemId}` 等 |

**Payload**：Workflow 入参仅含 **id 与标量**（`taskId`、`poolItemId`、`projectId`）；**禁止**传入 apiKey、完整需求正文；大文本在 Activity 内按 id 查库。

### 19.4 AgentScope Java 依赖与边界

- **依赖**：在 `backend/pom.xml` 中引入 **`io.agentscope:agentscope-core`**（当前 **`1.0.11`**，JDK 17+，与 **JDK 24 / Boot 3.5** 配套；升级请以 [Maven Central — agentscope-core](https://central.sonatype.com/artifact/io.agentscope/agentscope-core) 最新 **稳定版** 为准）。`agentscope-core` 已传递 **DashScope / Anthropic / Gemini** 等 SDK；OpenAI 兼容通道使用内置 **`OpenAIChatModel`**（OkHttp），与 `llm_channel.base_url` + 目录中的 completion 路径拼接一致。
- **配置**：`aiburst.mag.agentscope.max-iters`、`aiburst.mag.agentscope.call-timeout-seconds`（见 `application.yml`）。
- **边界**：编排入口在 **`com.aiburst.mag.agentscope`**（如 `MagAgentScopeRunService`），**禁止**在 `com.aiburst.llm` 包内引用 `mag_task` 等域类型；通道解密复用 **`LlmCryptoService`** 与 **`LlmChannelMapper`**。

### 19.5 QQ 机器人 Webhook（与 §13 配置对应）

中间层（本仓库或独立小服务）接收应用 **HTTP POST**（内网 URL，带简单 **HMAC 或静态 Token** 头 `X-Aiburst-Notify-Secret`）。**JSON 载荷草案**：

```json
{
  "eventType": "MAG_ALERT",
  "alertId": 123,
  "projectId": 1,
  "level": "WARN",
  "alertType": "STALL",
  "title": "Agent 停摆",
  "summary": "任务 42 超阈值无进展",
  "occurredAt": "2026-04-04T12:00:00Z"
}
```

QQ 侧将 `title` + `summary` + 链接（若配置 `aiburst.mag.notify.qq.detail-base-url`）拼成消息。**具体 OneBot/CQHTTP 与签名**写在实现 README，本文仅约束字段语义。

### 19.6 仓库内已落地的工程文件

| 路径 | 说明 |
|------|------|
| [docker-compose.temporal.yml](../docker-compose.temporal.yml) | Temporal + PostgreSQL + UI（开发） |
| [backend/.../V3__mag_module.sql](../backend/src/main/resources/db/migration/V3__mag_module.sql) | `mag_*` 表 |
| [backend/.../V11__remove_task_verification.sql](../backend/src/main/resources/db/migration/V11__remove_task_verification.sql) | 移除 `mag_task_verification`；迁移任务/Agent 角色 |
| [backend/.../V12__remove_external_fetch_audit.sql](../backend/src/main/resources/db/migration/V12__remove_external_fetch_audit.sql) | 删除 `mag_external_fetch_audit`；移除 `mag:audit:fetch:view` |
| [backend/.../V4__mag_permission_seed.sql](../backend/src/main/resources/db/migration/V4__mag_permission_seed.sql) | `mag:*` 菜单与权限种子 |
| [技术方案/mag-openapi-stub.yaml](mag-openapi-stub.yaml) | OpenAPI 3.0 路径草案（§19.2） |

**前端占位路由**：V4 中 `component` 为 `mag/ProjectList` 等，实现期在 `frontend/src/views/mag/` 下补页面并与路由表对齐。

---

## 20. 参考（文档与工程索引）

- [产品/多Agent协作与项目管理产品线需求.md](../产品/多Agent协作与项目管理产品线需求.md)  
- [技术方案/大模型接入技术方案.md](大模型接入技术方案.md)  
- [技术方案/权限系统技术方案.md](权限系统技术方案.md)  
- 工程：`docker-compose.temporal.yml`、`V3`/`V4` Flyway、[mag-openapi-stub.yaml](mag-openapi-stub.yaml)（见 §19.6）

---

*版本 v1.4 | **§17 产品线需求全量追溯**（与产品全文对照不省略）；§9 API 增补全量清单；§14 明确排期不缩减范围 | v1.2：§19.4 由 LangChain4j BOM 改为 **AgentScope Java agentscope-core**；v1.1：§19.7 错误码、§19.8 SpringDoc、[mag-openapi-stub.yaml](mag-openapi-stub.yaml) | 随评审迭代*
