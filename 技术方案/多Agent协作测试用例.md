# 多 Agent 协作（MAG）— 测试用例（需求全文覆盖）

本文档依据《[多Agent协作与项目管理产品线需求](../产品/多Agent协作与项目管理产品线需求.md)》（下称**产品文档**）与《[多Agent协作技术方案](多Agent协作技术方案.md)》编制。**覆盖原则**：产品文档中每一可验收功能点均在本文件中有**至少一条**测试用例或**明确标注为「范围外 / 依赖编排或二期」**的追溯行，**不省略**§1～§9、§11 及 §2.1 表格逐行。

**前缀**：`TC-MAG-`；**需求前缀**：`REQ-PRD-`（便于用例管理工具导入）。  
**优先级**：P0 阻断发布 / P1 核心 / P2 一般 / P3 可选或技术债跟踪。

---

## 0. 覆盖声明与符号

| 符号 | 含义 |
|------|------|
| API | 可通过 REST `/api/mag/**` 或平台接口验证 |
| UI | 前端页面或工作台操作验证 |
| E2E | 依赖 Temporal / AgentScope Java / 真实 Agent 编排 |
| CFG | 依赖配置项（如 `quality_flag`、阈值、`config_json`） |
| GAP | 当前实现未覆盖或仅部分覆盖，用例用于**缺口登记**，通过标准为「文档记录+后续迭代」 |
| OUT | 产品 §2.2 **明确不在本期范围**，用例为**反向确认不验收该项** |

---

## 1. 通用约定

| 项 | 说明 |
|----|------|
| 基础路径 | `/api/mag`，响应 `ApiResult` |
| 鉴权 | `Authorization: Bearer <token>` |
| 项目隔离 | 非成员访问带 `projectId` 的接口须失败 |
| 权限码 | 技术方案 §10 + V5：`mag:sched:manage`、`mag:kb:blueprint:import`、`mag:audit:fetch:view` |
| WebSocket | `/ws/mag?token=`；`SUBSCRIBE` / `UNSUBSCRIBE` / `PING` |

**测试数据**：用户 A（OWNER + 全量 mag 权限）、用户 B（VIEWER 或缺关键权限）；项目 P1、P2；五类 `role_type`：PM、PRODUCT、BACKEND、FRONTEND、TEST。

---

## 2. 附录 A — 产品需求全文 → 测试用例映射表（不省略）

### 2.1 产品 §1「产品目标」逐条

| REQ-ID | 产品位置 | 需求摘要 | 用例 ID | 优先级 | 验证方式 |
|--------|----------|----------|---------|--------|----------|
| REQ-PRD-G01 | §1 目标① | 录入、配置、启用/停用各类 Agent，同项目协同 | TC-MAG-G01 | P0 | API+UI：`POST/PUT .../agents`，`status` 启停 |
| REQ-PRD-G02 | §1 目标② | 项目级工作台：Agent 列表、沟通、任务状态 | TC-MAG-G02 | P0 | UI：工作台 Tab；API：agents/threads/tasks |
| REQ-PRD-G03 | §1 目标③ | 主/子层级；子要活；主向 PM；PM 按模块进度派工 | TC-MAG-G03 | P1 | API：`request-next`、模块 CRUD、任务 assignee；E2E：派工链 GAP 可标 |
| REQ-PRD-G04 | §1 目标④ | 全量记录行为与改进轨迹；定时任务触发 | TC-MAG-G04 | P1 | API：improvements、scheduled-jobs；E2E：调度执行 GAP |
| REQ-PRD-G05 | §1 目标⑤ | 可编辑需求文档；变更识别受影响点；任务与 Agent 对齐 | TC-MAG-G05 | P1 | API：requirement-doc、revisions、diff、analyze；E2E：自动重排 GAP |
| REQ-PRD-G06 | §1 目标⑥ | 运营大屏：是否干活、产出效率、异常；停摆/升级告警 | TC-MAG-G06 | P1 | API：dashboard、alerts；WS：`alert.new`；E2E： stall 检测 GAP |
| REQ-PRD-G07 | §1 目标⑦ | 无法落地→向 PM 说明；PM 协助留痕；升级产品；KB+成熟产品检索+多方案择优；窄口径拍板 | TC-MAG-G07 | P0 | API：block、pm-assist、pool、fetch-audit；E2E：升级链 GAP |
| REQ-PRD-G08 | §1 目标⑧ | 发版归档；对比过程/协调/停滞/排障沉淀；项目隔离；引用归档经验与模块 | TC-MAG-G08 | P1 | API：releases、kb、import-blueprint；隔离用例 TC-MAG-ISO |
| REQ-PRD-G09 | §1 目标⑨ | 组织知识库：归档优质回流 + 人工录入；产品/组织检索 | TC-MAG-G09 | P1 | API：kb entries；CFG：`quality_flag` 回流 |
| REQ-PRD-G10 | §1 目标⑩ | 系统用户处理待拍板；待办与/或需求池可达 | TC-MAG-G10 | P0 | API：`GET /todos` + 需求池；UI：TodoList + 工作台池 |
| REQ-PRD-G11 | §1 目标⑪ | 申报完成后任务进入已完成（DONE）；流程与留痕可审计 | TC-MAG-G11 | P0 | API：`submit-complete`→`DONE`；无独立核查 API |

### 2.2 产品 §2.1「本期包含」表格 — 逐行（16 行全列）

| REQ-ID | 模块（原文） | 用例 ID | 优先级 | 验证方式 |
|--------|--------------|---------|--------|----------|
| REQ-PRD-M01 | Agent 管理：新增/编辑实例；角色、能力、模型工具配置 | TC-MAG-M01 | P0 | API `.../agents`、`PUT /agents/{id}`；`extra_json` 等 |
| REQ-PRD-M02 | 项目与成员：CRUD；真人成员；无「仅拍板」成员类型；OWNER/MEMBER/VIEWER；Agent 列表与职能线 | TC-MAG-M02 | P0 | API projects/members；RBAC+成员双因子；UI |
| REQ-PRD-M03 | 组织知识库：归档回流+人工录入；与 §5.6、§5.8 联动 | TC-MAG-M03 | P1 | API kb；回流 CFG；池 payload 联动 GAP |
| REQ-PRD-M04 | 用户待办：待拍板；待办与需求池联动；聚合他类可选 | TC-MAG-M04 | P0 | `GET /todos` §16.2；聚合告警/定时失败为 P3 GAP |
| REQ-PRD-M05 | 协作沟通可观测：线程消息可见；权限脱敏；按项目/Agent/时间筛选 | TC-MAG-M05 | P1 | API threads/messages；筛选 query GAP 则记技术方案 §17 |
| REQ-PRD-M06 | 协调链：PM 协调主 Agent；主协调子 Agent | TC-MAG-M06 | P2 | E2E/GAP；消息 kind 可审计 |
| REQ-PRD-M07 | 任务与申领：要活；主向 PM 缺口；PM 按模块派工 | TC-MAG-M07 | P1 | API request-next、module、task assignee |
| REQ-PRD-M08 | 任务结项：申报完成直落已完成 | TC-MAG-M08 | P0 | 与 TC-MAG-G11 同簇；无 `mag_task_verification` |
| REQ-PRD-M09 | 改进记录：变更/调优/反馈/版本可追溯 | TC-MAG-M09 | P1 | API improvements |
| REQ-PRD-M10 | 定时任务：计划触发检查/同步/汇报/周期 Agent 动作 | TC-MAG-M10 | P1 | API scheduled-jobs；执行留痕 GAP |
| REQ-PRD-M11 | 需求文档中心：编辑、版本、评审状态、变更影响入口 | TC-MAG-M11 | P1 | API doc/revisions/diff；评审状态字段 GAP |
| REQ-PRD-M12 | 运营大屏：实时动态；Agent 状态、执行、效率；告警 | TC-MAG-M12 | P1 | dashboard+WS+alerts |
| REQ-PRD-M13 | 阻塞与升级：主动沟通；PM 协助；升级产品；KB+成熟产品+比选；窄口径拍板 | TC-MAG-M13 | P0 | 与 TC-MAG-G07 同批用例展开 §5.6 |
| REQ-PRD-M14 | 需求池：窄口径拍板项；已闭环并存；与文档/升级单关联 | TC-MAG-M14 | P0 | pool+anchor/revision；payload 字段 |
| REQ-PRD-M15 | 发版归档：发布快照；对比/协调/停滞/经验 | TC-MAG-M15 | P1 | releases.snapshot_json 键完整性 GAP 可检 |
| REQ-PRD-M16 | 组织经验与模块复用：归档可检索；模块匹配引用/复制；与隔离配套 | TC-MAG-M16 | P1 | import-blueprint + KB + TC-MAG-ISO |

### 2.3 产品 §2.2「本期不包含」— 验收边界

| REQ-ID | 产品位置 | 需求摘要 | 用例 ID | 说明 |
|--------|----------|----------|---------|------|
| REQ-PRD-OUT01 | §2.2-1 | 真实 IM 双向深度集成不在本期 | TC-MAG-OUT01 | OUT：不测企微/Slack 双向；可测导出/Webhook 若实现 |
| REQ-PRD-OUT02 | §2.2-2 | 自动代码合并生产不在本期 | TC-MAG-OUT02 | OUT |
| REQ-PRD-OUT03 | §2.2-3 | 跨租户协作另立需求 | TC-MAG-OUT03 | OUT |

### 2.4 产品 §3「五类 Agent」— 角色与职责

| REQ-ID | 类型 | 需求摘要 | 用例 ID | 优先级 | 验证方式 |
|--------|------|----------|---------|--------|----------|
| REQ-PRD-R01 | PM | 里程碑/模块、派工、阻塞协助、升级产品、风险变更 | TC-MAG-R01 | P2 | 实例创建+消息/协助记录；工具链 E2E GAP |
| REQ-PRD-R02 | PRODUCT | 需求结构、澄清、变更范围；KB→成熟产品→比选→窄口径池 | TC-MAG-R02 | P1 | 池 payload 与文档 API；E2E GAP |
| REQ-PRD-R03 | BACKEND | 子 Agent 分担 | TC-MAG-R03 | P1 | parent_agent_id + 任务绑定 |
| REQ-PRD-R04 | FRONTEND | 子 Agent 分担 | TC-MAG-R04 | P1 | 同 R03 |
| REQ-PRD-R05 | TEST | 测试设计与执行；申报完成与其他职能一致直落 DONE | TC-MAG-R05 | P1 | `submit-complete`→`DONE` |

### 2.5 产品 §4.1～§4.5

| REQ-ID | 产品位置 | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|----------|---------|--------|
| REQ-PRD-4101 | §4.1 | 1 主 + N 子 / 主职责 | TC-MAG-4101 | P1 |
| REQ-PRD-4102 | §4.1 | 子职责 + 「要活」 | TC-MAG-4102 | P1 |
| REQ-PRD-4201 | §4.2 | 主向 PM 请求任务/优先级 | TC-MAG-4201 | P2 |
| REQ-PRD-4202 | §4.2 | PM 依据进度/模块/依赖派工 | TC-MAG-4202 | P2 |
| REQ-PRD-4203 | §4.2 | 界面可见待派发/进行中/阻塞原因（UI+API：`state`、`block_reason`） | TC-MAG-4203 | P1 |
| REQ-PRD-4301 | §4.3 | 无法执行须主动协作消息+原因（`block` API + 消息 JSON） | TC-MAG-4301 | P1 |
| REQ-PRD-4302 | §4.3 | 每次 PM 协助写入协助记录（`pm-assist`） | TC-MAG-4302 | P1 |
| REQ-PRD-4401 | §4.4 | 请求派工/阻塞/澄清/联调等进时间线或线程 | TC-MAG-4401 | P1 |
| REQ-PRD-4402 | §4.4 | 用户可介入 @Agent 备注（USER 消息；@ 解析可为 GAP） | TC-MAG-4402 | P2 |
| REQ-PRD-4501 | §4.5 | 申报完成：`IN_PROGRESS`→`DONE` | TC-MAG-4501 | P0 |
| REQ-PRD-4502 | §4.5 | 人工 / API / 系统自动申报同一口径（`MagTaskAutomationProperties` 等） | TC-MAG-4502 | P1 |
| REQ-PRD-4503 | §4.5 | 完成后流程事件与编排/改进留痕可审计 | TC-MAG-4503 | P1 |

### 2.6 产品 §5.1 项目入口与工作台

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-5101 | 列表：名称、状态、Agent 数、最近活动、需求版本 | TC-MAG-5101 | P0 |
| REQ-PRD-5102 | 详情：成员、Agent（角色主从当前任务心跳可选）、沟通、任务、需求、改进摘要 | TC-MAG-5102 | P1 |
| REQ-PRD-5103 | 跳转本项目待办 | TC-MAG-5103 | P2 |
| REQ-PRD-5104 | 无「仅拍板」成员档位；拍板=RBAC+成员（SEC：无独立角色枚举） | TC-MAG-5104 | P0 |

### 2.7 产品 §5.2 任务与模块

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-5201 | 模块树/标签与需求双向关联（`task.moduleId`、池 `anchor` 等可为 GAP） | TC-MAG-5201 | P2 |
| REQ-PRD-5202 | 状态含：待派发、进行中、已完成、阻塞 | TC-MAG-5202 | P0 |
| REQ-PRD-5203 | 申报完成进入已完成（`submit-complete`） | TC-MAG-5203 | P0 |
| REQ-PRD-5204 | 阻塞记录原因与责任人 Agent（`block_reason`、`blocked_by_agent_id`） | TC-MAG-5204 | P1 |

### 2.8 产品 §5.3 改进记录

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-5301 | 配置/提示词/反馈/质量标注写入改进日志 | TC-MAG-5301 | P1 |
| REQ-PRD-5302 | 按 Agent/项目/时间过滤 | TC-MAG-5302 | P2 | 分页 query GAP |
| REQ-PRD-5303 | 导出可选 | TC-MAG-5303 | P3 | GAP |

### 2.9 产品 §5.4 定时任务

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-5401 | 为项目或 Agent 配置定时任务 | TC-MAG-5401 | P1 |
| REQ-PRD-5402 | 执行结果写时间线或通知 | TC-MAG-5402 | P2 | E2E/GAP |

### 2.10 产品 §5.5 运营大屏

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-5501 | 大屏视图、全屏/分辨率 | TC-MAG-5501 | P2 | UI |
| REQ-PRD-5502 | 实时动态（轮询或推送） | TC-MAG-5502 | P1 | WS+dashboard |
| REQ-PRD-5503 | Agent 状态：空闲/执行中/等待/阻塞/离线异常 | TC-MAG-5503 | P2 | snapshot 字段 GAP |
| REQ-PRD-5504 | 是否干活：绑定任务、产出时间、心跳 | TC-MAG-5504 | P2 | GAP |
| REQ-PRD-5505 | 产出效率：周期完成数、交付物更新等 | TC-MAG-5505 | P1 | §16.4 任务统计 |
| REQ-PRD-5506 | 按 Agent/职能线/项目筛选 | TC-MAG-5506 | P2 | query GAP |
| REQ-PRD-5507 | 告警：应干活未干活 | TC-MAG-5507 | P2 | E2E/GAP |
| REQ-PRD-5508 | 告警：PM 无法协调 | TC-MAG-5508 | P2 | alert 类型+事件编号 |
| REQ-PRD-5509 | 大屏高亮告警；可选声音/订阅 | TC-MAG-5509 | P3 | WS/通知 GAP |

### 2.11 产品 §5.6 升级链路、协助、产品—需求池（含 5.6.1、5.6.2）

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-5601 | 产品 Agent 主导；非事事拍板 | TC-MAG-5601 | P2 | 流程/产品说明 |
| REQ-PRD-5602 | 同时满足：KB 无据 + 搜不到成熟产品 → 待用户拍板 | TC-MAG-5602 | P1 | 池创建规则 E2E/GAP |
| REQ-PRD-5603 | 多竞品须结构化对比择优+留痕；不要求拍板 | TC-MAG-5603 | P2 | payload 键 compareMatrix |
| REQ-PRD-5604 | KB 有据或命中成熟产品→更新需求/建议派工；可已闭环池项 | TC-MAG-5604 | P2 | E2E/GAP |
| REQ-PRD-5611 | 5.6.1-须同时满足①已执行检索+留痕 | TC-MAG-5611 | P1 | fetch-audit |
| REQ-PRD-5612 | 5.6.1-②命中已落地产品实体 | TC-MAG-5612 | P2 | 池 payload matureProductHits |
| REQ-PRD-5613 | 5.6.1-③与争议点可关联说明 | TC-MAG-5613 | P2 |
| REQ-PRD-5614 | 5.6.1-④闭环记录：检索摘要+产品标识+结论 | TC-MAG-5614 | P1 |
| REQ-PRD-5615 | 不视为命中：仅论文无产品实体 | TC-MAG-5615 | P3 | 规则/E2E GAP |
| REQ-PRD-5616 | 不视为命中：仅有概念文档说不清哪款产品 | TC-MAG-5616 | P3 | GAP |
| REQ-PRD-5617 | 不视为命中：多款明显不同未比选 | TC-MAG-5617 | P3 | GAP |
| REQ-PRD-5618 | 不视为命中：无锚定产品或不对题噪音 | TC-MAG-5618 | P3 | GAP |
| REQ-PRD-5619 | 不视为命中：未检索或范围不符应拦截 | TC-MAG-5619 | P3 | GAP |
| REQ-PRD-5620 | 升级顺序①执行→PM 说明 | TC-MAG-5620 | E2E |
| REQ-PRD-5621 | 升级顺序②PM 无法解决→大屏告警→升级产品 | TC-MAG-5621 | E2E/GAP |
| REQ-PRD-5622 | 升级顺序③产品：KB→检索→比选→写回；窄口径才建池 | TC-MAG-5622 | E2E/GAP |
| REQ-PRD-5623 | 升级顺序④结论写回文档/池并重排任务 | TC-MAG-5623 | E2E/GAP |
| REQ-PRD-5631 | PM 协助记录：问题类型、根因、动作、协助的 Agent、时间、是否解除阻塞 | TC-MAG-5631 | P1 | pm-assist 字段 |
| REQ-PRD-5632 | 协助记录项目内可检索、审计报表可选导出 | TC-MAG-5632 | P2 |
| REQ-PRD-5633 | 升级单/告警/池各项/已闭环/拍板结果持久化且关联项目需求 | TC-MAG-5633 | P1 |
| REQ-PRD-5634 | 需求池可看待拍板、已闭环、历史已确认 | TC-MAG-5634 | P1 | listPool 过滤 |
| REQ-PRD-5641 | 5.6.2 系统用户与权限一致；无仅拍板成员角色 | TC-MAG-5641 | P0 |
| REQ-PRD-5642 | 5.6.2 登录后待拍板可达（待办与/或池） | TC-MAG-5642 | P0 |
| REQ-PRD-5643 | 5.6.2 仅权限或指派用户可见可操作 | TC-MAG-5643 | P0 |
| REQ-PRD-5644 | 5.6.2 聚合告警/定时失败进待办为可选 | TC-MAG-5644 | P3 | GAP |

### 2.12 产品 §5.7 发版归档

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-5701 | 达里程碑可发版归档 | TC-MAG-5701 | P1 |
| REQ-PRD-5702 | 每次发版一条记录；版本号、时间、制品/说明引用 | TC-MAG-5702 | P1 |
| REQ-PRD-5703 | 快照含需求基线、模块清单、任务结论状态 | TC-MAG-5703 | P1 | snapshot_json |
| REQ-PRD-5704 | 经验：方案对比、协调问题、停滞案例、协助与升级结论、模块标签 | TC-MAG-5704 | P2 | 键完整性 |
| REQ-PRD-5705 | 归档只读；修正用新版或勘误附录 | TC-MAG-5705 | P1 | API 无 PUT 归档 |

### 2.13 产品 §5.8 隔离与共享

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-5801 | 强隔离：任务/需求草稿/配置/会话/线程不跨项目 | TC-MAG-5801 | P0 | TC-MAG-ISO |
| REQ-PRD-5802 | 新 Agent 会话属新项目；历史会话不可接续 | TC-MAG-5802 | P1 |
| REQ-PRD-5803 | 来源一：优质经验回流 KB | TC-MAG-5803 | P1 |
| REQ-PRD-5804 | 来源二：人工录入编辑下架 | TC-MAG-5804 | P0 |
| REQ-PRD-5805 | 新项目检索订阅组织 KB 与归档提炼物 | TC-MAG-5805 | P2 | GAP |
| REQ-PRD-5806 | 模块匹配：只读参照或复制蓝图 | TC-MAG-5806 | P1 | import-blueprint |
| REQ-PRD-5807 | 可注入组织经验；不得默认注入他项目会话原文 | TC-MAG-5807 | P2 | 安全审查 |
| REQ-PRD-5808 | 共享经验非跨项目运行态耦合 | TC-MAG-5808 | P1 |

### 2.14 产品 §6 需求文档与变更

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-6101 | §6.1 专用界面；富文本或结构化；版本列表与对比 | TC-MAG-6101 | P1 |
| REQ-PRD-6102 | §6.1 在线编辑批注改进；保存即新版本 | TC-MAG-6102 | P1 |
| REQ-PRD-6201 | §6.2 实质变更：Agent 主导分析建议+留痕 | TC-MAG-6201 | P1 | analyze API |
| REQ-PRD-6202 | §6.2 真人确认仅 5.6 窄口径 | TC-MAG-6202 | P0 |
| REQ-PRD-6203 | §6.2 标识受影响功能点/模块/任务 | TC-MAG-6203 | P2 | analyze 出参 |
| REQ-PRD-6204 | §6.2 触发 PM 组织主 Agent 重评工时依赖 | TC-MAG-6204 | E2E/GAP |
| REQ-PRD-6205 | §6.2 对未开始/进行中任务调整建议 | TC-MAG-6205 | E2E/GAP |
| REQ-PRD-6206 | §6.2 全链路可追踪调整 | TC-MAG-6206 | P2 |
| REQ-PRD-6301 | §6.3 池与文档条目可关联 | TC-MAG-6301 | P1 | revisionId、anchor_json |
| REQ-PRD-6302 | §6.3 状态：待拍板、已闭环产品、用户确认合理/变更、关闭等 | TC-MAG-6302 | P1 |
| REQ-PRD-6303 | §6.3 待拍板卡片生成条件（双条件） | TC-MAG-6303 | E2E/GAP |
| REQ-PRD-6304 | §6.3 已闭环卡片审计字段 | TC-MAG-6304 | P2 |
| REQ-PRD-6305 | §6.3 池字段示例与拍板后留痕 | TC-MAG-6305 | P1 | decide、product-close |

### 2.15 产品 §7 权限与安全

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-7101 | 拍板项 RBAC+成员/指派双约束 | TC-MAG-7101 | P0 |
| REQ-PRD-7102 | 项目数据隔离仅授权可见 | TC-MAG-7102 | P0 |
| REQ-PRD-7103 | 归档与经验库：读/检索/引用/复制分权 | TC-MAG-7103 | P1 |
| REQ-PRD-7104 | KB 人工维护单独授权 | TC-MAG-7104 | P0 |
| REQ-PRD-7105 | 跨项目引用显式授权与审计 | TC-MAG-7105 | P1 |
| REQ-PRD-7106 | 敏感沟通按角色脱敏 | TC-MAG-7106 | P2 | GAP |
| REQ-PRD-7107 | 导出与 API 鉴权 | TC-MAG-7107 | P0 |
| REQ-PRD-7108 | 注入 Agent 的历史经验脱敏裁剪 | TC-MAG-7108 | E2E/GAP |
| REQ-PRD-7109 | 外部工具范围声明与审计 | TC-MAG-7109 | P2 | GAP |

### 2.16 产品 §8 非功能

| REQ-ID | 需求摘要 | 用例 ID | 优先级 |
|--------|----------|---------|--------|
| REQ-PRD-8101 | 可追溯性：所列实体可关联查询 | TC-MAG-8101 | P1 | 穿透 SQL/报表 |
| REQ-PRD-8102 | 隔离性：跨项目仅显式归档/蓝图 | TC-MAG-8102 | P0 |
| REQ-PRD-8103 | 性能：时间线分页；消息可归档 | TC-MAG-8103 | P2 |
| REQ-PRD-8104 | 可靠性：定时失败重试与可见 | TC-MAG-8104 | P2 | GAP |
| REQ-PRD-8105 | 可扩展性：角色与协调策略可扩展 | TC-MAG-8105 | P3 | 架构审查 |

### 2.17 产品 §9 验收要点（15 条，与技术方案 §17.10 一致）

| REQ-ID | 产品 §9 序号 | 用例 ID | 优先级 |
|--------|--------------|---------|--------|
| REQ-PRD-901 | 1 | TC-MAG-ACC-01 | P0 |
| REQ-PRD-902 | 2 | TC-MAG-ACC-02 | P0 |
| REQ-PRD-903 | 3 | TC-MAG-ACC-03 | P1 |
| REQ-PRD-904 | 4 | TC-MAG-ACC-04 | P0 |
| REQ-PRD-905 | 5 | TC-MAG-ACC-05 | P0 |
| REQ-PRD-906 | 6 | TC-MAG-ACC-06 | P1 |
| REQ-PRD-907 | 7 | TC-MAG-ACC-07 | P1 |
| REQ-PRD-908 | 8 | TC-MAG-ACC-08 | P1 |
| REQ-PRD-909 | 9 | TC-MAG-ACC-09 | P0 |
| REQ-PRD-910 | 10 | TC-MAG-ACC-10 | P1 |
| REQ-PRD-911 | 11 | TC-MAG-ACC-11 | P1 |
| REQ-PRD-912 | 12 | TC-MAG-ACC-12 | P2 |
| REQ-PRD-913 | 13 | TC-MAG-ACC-13 | P0 |
| REQ-PRD-914 | 14 | TC-MAG-ACC-14 | P1 |
| REQ-PRD-915 | 15 | TC-MAG-ACC-15 | P1 |

### 2.18 产品 §10「关联文档」

| REQ-ID | 需求摘要 | 用例 ID | 优先级 | 验证方式 |
|--------|----------|---------|--------|----------|
| REQ-PRD-10001 | 实现依赖技术方案、权限产品线、大模型产品线；权限码与通道配置可对齐文档 | TC-MAG-10001 | P3 | 文档/配置审查：对照 [权限产品线需求](../产品/权限产品线需求.md)、[大模型与API对接产品线需求](../产品/大模型与API对接产品线需求.md) 做一致性走查 |

### 2.19 产品 §11「技术方案补齐要点」— 不视为砍需求，验收依赖技术落地

| REQ-ID | 产品 §11 类别 | 用例 ID | 说明 |
|--------|----------------|---------|------|
| REQ-PRD-11101 | 组织知识库实现细节 | TC-MAG-11101 | P3 | 验证技术方案 §16.1、FULLTEXT、阈值 config |
| REQ-PRD-11102 | 真人成员与待办细节 | TC-MAG-11102 | P1 | 与 TC-MAG-564* 合并执行 |
| REQ-PRD-11103 | 通知与告警订阅 | TC-MAG-11103 | P2 | WS、QQ Webhook GAP |
| REQ-PRD-11104 | 产出物与效率指标 | TC-MAG-11104 | P2 | 与 §16.4 一致 |
| REQ-PRD-11105 | Agent 编排与韧性 | TC-MAG-11105 | P3 | E2E/GAP |
| REQ-PRD-11106 | 外部检索合规 | TC-MAG-11106 | P1 | SSRF+审计+截断 hash §12 |
| REQ-PRD-11107 | 数据生命周期 | TC-MAG-11107 | P3 | §16.6 永久保留策略 |
| REQ-PRD-11108 | 灾难恢复与备份 | TC-MAG-11108 | P3 | 运维基线，非应用单测 |

---

## 3. 附录 B — 关键用例步骤摘要（与上文 ID 对应）

### 3.1 项目隔离（REQ-PRD-5801 等）

**TC-MAG-ISO / TC-MAG-5801**

1. 用户 A 在 P1 创建线程、任务、消息。  
2. 用户 A 访问 P2 的 `GET .../threads`、`GET .../tasks`（篡改 projectId 或越权 ID）。  
3. **期望**：无 P1 数据泄露；非法 ID 返回 403/NOT_FOUND。

### 3.2 产品 §9 验收用例（TC-MAG-ACC-01～15）

| 用例 ID | 步骤摘要 | 期望 |
|---------|----------|------|
| TC-MAG-ACC-01 | 创建项目；创建 PM/BACKEND/FRONTEND/TEST/PRODUCT 及子 Agent | 列表 `roleType`、`parentAgentId` 正确 |
| TC-MAG-ACC-02 | 建线程；AGENT/USER 发消息；`GET messages` | 时间线可见；非成员不可读 |
| TC-MAG-ACC-03 | `request-next` | 协调线程含 `REQUEST_NEXT` JSON |
| TC-MAG-ACC-04 | start → submit-complete | 任务 `DONE`；`temporal_workflow_id` 按实现清空或保留以代码为准 |
| TC-MAG-ACC-05 | 任务流程事件 / 改进日志可查 | 申报完成写入可查询记录（无核查子表） |
| TC-MAG-ACC-06 | improvements GET/POST | 按 Agent、项目过滤 |
| TC-MAG-ACC-07 | scheduled-jobs PUT/GET；执行痕迹 | 配置成功；执行 GAP 备注 |
| TC-MAG-ACC-08 | 保存需求；revisions；diff；analyze | 版本递增；SYSTEM 消息；traceId |
| TC-MAG-ACC-09 | 池项 `PENDING_USER`；decide；指派与 OWNER 规则 | §16.2 过滤；状态映射正确 |
| TC-MAG-ACC-10 | dashboard；alerts；WS | 统计字段；订阅与推送 |
| TC-MAG-ACC-11 | block；pm-assist | 阻塞字段；协助记录字段完整 |
| TC-MAG-ACC-12 | 池 payload 成熟产品与对比；fetch 审计 | 字段存在；审计落库 |
| TC-MAG-ACC-13 | todos 与池一致性 | 无权限不可见 |
| TC-MAG-ACC-14 | MANUAL + ARCHIVE_REFLOW KB | source 区分；检索 GAP |
| TC-MAG-ACC-15 | release；import-blueprint；跨项目 | 源归档不变；隔离 |

### 3.3 REST API 分模块清单（技术方案 §9）

与 **附录 A** 映射重复处不再展开；补充 **仅 API 层**检查点：

- **项目与成员**：分页字段 `agentCount`、`lastActivityAt`、`currentRequirementVersion`（REQ-PRD-5101）。  
- **任务**：`Idempotency-Key` 重复 submit-complete（可选实现）。  
- **需求池**：`decide` 全决策枚举；`product-close`（REQ-PRD-6305）。  
- **KB**：`ARCHIVE_REFLOW` 只读不可删改（REQ-PRD-5804）。  
- **告警**：`ack` 后 acknowledged（REQ-PRD-5633）。  
- **fetch-audit**：仅成员+`mag:audit:fetch:view`（REQ-PRD-5611）。

### 3.4 权限与安全

| 用例 ID | 说明 |
|---------|------|
| TC-MAG-SEC-01～06 | VIEWER 写任务；无 pool:decide 拍板；无 kb:manage；无 sched；无 blueprint；无 audit |
| TC-MAG-SAFE-01～03 | SSRF；审计；任务状态迁移须经公开 API |

### 3.5 WebSocket（§8）

TC-MAG-WS-01～05：token、connected、SUBSCRIBE、PING、broadcast 事件。

### 3.6 前端冒烟（§11）

TC-MAG-UI-01～04：项目列表、工作台全 Tab、知识库、大屏 WS。

---

## 4. 测试数据与 SQL 抽检

```sql
SELECT * FROM mag_project_member WHERE project_id = ?;
SELECT id, state, block_reason, blocked_by_agent_id, assignee_agent_id FROM mag_task WHERE project_id = ?;
SELECT id, state, revision_id, anchor_json, payload_json FROM mag_requirement_pool_item WHERE project_id = ?;
SELECT id, source, archive_id, title FROM mag_kb_entry;
SELECT id, version_label, quality_flag, snapshot_json FROM mag_release_archive WHERE project_id = ?;
```

---

## 5. GAP 汇总（测试报告必填）

执行附录 A 时，凡标注 **GAP** 的 REQ，须在测试报告中填写：**当前验证深度**（仅 API / 未实现 / 需 E2E 环境）、**计划迭代**。不得将 GAP 行从产品范围删除（与产品 §2 交付策略一致）。

---

## 6. 修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-04-04 | 初版 |
| v2.0 | 2026-04-04 | **需求全文覆盖**：§1～§11（含 §10 关联文档走查）、§2.1 十六行、§2.2 边界、附录 A 追溯矩阵 + GAP/OUT 符号 |
| v2.1 | 2026-04-05 | 去掉核查 Agent / `mag_task_verification` / 待核查类状态；用例与产品「申报即 DONE」对齐 |

---

*编排类（PM/产品 Agent 自动升级、定时 Job 真实执行、多方案比选自动化）以 **E2E** 或 **GAP** 标注；接口与数据模型以 **API+DB** 验证为准。*
