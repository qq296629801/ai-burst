<template>
  <el-card v-if="project" shadow="never" class="head">
    <div class="title-row">
      <h2>{{ project.name }}</h2>
      <span class="meta">
        ID {{ project.id }} · {{ project.status === 1 ? '进行中' : '已归档' }}
        <template v-if="project.agentCount != null"> · Agent {{ project.agentCount }}</template>
        <template v-if="project.currentRequirementVersion != null">
          · 需求版本 v{{ project.currentRequirementVersion }}
        </template>
        <template v-if="project.lastActivityAt"> · 最近活动 {{ project.lastActivityAt }}</template>
      </span>
    </div>
    <el-tabs v-model="tab">
      <el-tab-pane label="成员" name="members">
        <el-table :data="members" border stripe size="small">
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="roleInProject" label="角色" width="120" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="Agent" name="agents">
        <div class="toolbar">
          <el-button v-permission="'mag:agent:manage'" type="primary" size="small" @click="openAgent">
            新建 Agent
          </el-button>
        </div>
        <el-table :data="agents" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="roleType" label="类型" width="160">
            <template #default="{ row }">{{ agentRoleLabel(row.roleType) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80" />
          <el-table-column label="大模型通道" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ agentChannelLabel(row.llmChannelId) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                v-permission="'mag:agent:manage'"
                link
                type="primary"
                size="small"
                @click="openEditAgent(row)"
              >
                编辑
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="模块" name="modules">
        <div class="toolbar">
          <el-button v-permission="'mag:agent:manage'" type="primary" size="small" @click="openModule">
            新建模块
          </el-button>
          <el-button v-permission="'mag:kb:blueprint:import'" type="success" size="small" @click="openBlueprint">
            导入蓝图
          </el-button>
          <el-button size="small" @click="loadModules">刷新</el-button>
        </div>
        <el-table :data="modules" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="parentId" label="父" width="72" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="tag" label="标签" width="140" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button v-permission="'mag:agent:manage'" link type="primary" size="small" @click="editModule(row)">
                编辑
              </el-button>
              <el-button v-permission="'mag:agent:manage'" link type="danger" size="small" @click="removeModule(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="任务" name="tasks">
        <div class="toolbar wrap">
          <el-button v-permission="'mag:task:dispatch'" type="primary" size="small" @click="openDispatch">
            派工
          </el-button>
          <span class="form-hint">
            进行中任务在关联 Agent 编排成功且系统开启自动结项时，将自动变为「已完成」；不提供人工申报完成、阻塞与要活入口。
          </span>
        </div>
        <el-table :data="tasks" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="title" label="标题" />
          <el-table-column label="执行 Agent" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ assigneeAgentLabel(row.assigneeAgentId) }}</template>
          </el-table-column>
          <el-table-column prop="state" label="状态" width="120" />
          <el-table-column label="流程阶段" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="taskPhaseTagType(row.state)">{{ taskPhaseLabel(row.state) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280">
            <template #default="{ row }">
              <el-button
                v-permission="'mag:project:list'"
                link
                type="info"
                size="small"
                @click="openTaskFlow(row)"
              >
                流程
              </el-button>
              <el-button
                v-permission="'mag:task:dispatch'"
                v-if="row.state === 'PENDING' || row.state === 'IN_PROGRESS' || row.state === 'BLOCKED'"
                link
                type="primary"
                size="small"
                @click="openReassign(row)"
              >
                改派
              </el-button>
              <el-button
                v-permission="'mag:task:operate'"
                v-if="row.state === 'PENDING'"
                link
                type="primary"
                size="small"
                @click="startTask(row)"
              >
                开始
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="需求文档" name="req">
        <div class="toolbar wrap">
          <el-button v-permission="'mag:req:edit'" type="primary" size="small" @click="saveReq">保存新版本</el-button>
          <el-button size="small" @click="openRevisions">版本列表</el-button>
          <el-input-number v-model="diffV1" :min="1" size="small" placeholder="v1" />
          <el-input-number v-model="diffV2" :min="1" size="small" placeholder="v2" />
          <el-button size="small" @click="loadDiff">对比</el-button>
          <el-button v-permission="'mag:req:edit'" type="warning" size="small" @click="openAnalyze">
            变更影响分析
          </el-button>
        </div>
        <el-input v-model="reqContent" type="textarea" :rows="14" style="margin-top: 12px" />
        <el-input
          v-if="diffText"
          v-model="diffText"
          type="textarea"
          :rows="8"
          readonly
          style="margin-top: 12px"
          placeholder="版本 diff 结果"
        />
      </el-tab-pane>
      <el-tab-pane label="沟通" name="threads">
        <el-button v-permission="'mag:task:operate'" type="primary" size="small" @click="addThread">新线程</el-button>
        <el-table :data="threads" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="title" label="标题" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openThreadRoom(row)">消息</el-button>
              <el-button
                v-permission="'mag:task:operate'"
                link
                type="primary"
                size="small"
                @click="runThreadRow(row)"
              >
                触发编排
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="编排执行" name="orchRuns">
        <div class="toolbar">
          <el-button size="small" @click="loadOrchestrationRuns">刷新</el-button>
          <el-tag :type="magWs.connected ? 'success' : 'info'" size="small" style="margin-left: 8px">
            WS {{ magWs.connected ? '已连接' : '未连接' }}
          </el-tag>
        </div>
        <el-table :data="orchestrationRuns" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="startedAt" label="开始时间" width="170" />
          <el-table-column label="类型" width="96">
            <template #default="{ row }">{{ orchRunKindLabel(row.runKind) }}</template>
          </el-table-column>
          <el-table-column label="目标" width="112">
            <template #default="{ row }">
              <span v-if="row.runKind === 'AGENT'">Agent #{{ row.agentId }}</span>
              <span v-else-if="row.runKind === 'THREAD'">线程 #{{ row.threadId }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column prop="workflowId" label="workflowId" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="orchStatusTagType(row.status)" size="small">{{ orchStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="说明" min-width="140" show-overflow-tooltip />
          <el-table-column label="结果/错误" min-width="200">
            <template #default="{ row }">
              <div v-if="row.resultSummary" class="result-md-list-cell">
                <span class="result-md-list-text" :title="row.resultSummary">{{ row.resultSummary }}</span>
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="openResultMarkdownPreview('编排执行 · 结果/错误', row.resultSummary)"
                >
                  预览
                </el-button>
              </div>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column prop="finishedAt" label="结束时间" width="170" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="告警" name="alerts">
        <el-button size="small" @click="loadAlerts">刷新</el-button>
        <el-table :data="alerts" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="level" label="级别" width="80" />
          <el-table-column prop="alertType" label="类型" width="140" />
          <el-table-column prop="payloadJson" label="载荷" show-overflow-tooltip />
          <el-table-column prop="acknowledged" label="已确认" width="88" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                v-if="!row.acknowledged"
                link
                type="primary"
                size="small"
                @click="ackAlert(row)"
              >
                确认
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="user.hasPerm('mag:sched:manage')" label="定时任务" name="sched">
        <div class="toolbar">
          <el-button type="primary" size="small" @click="openSched">新建/更新</el-button>
          <el-button size="small" @click="loadSched">刷新（本项目）</el-button>
        </div>
        <el-table :data="schedJobs" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="jobKey" label="Key" />
          <el-table-column prop="cronExpr" label="Cron" width="160" />
          <el-table-column prop="enabled" label="启用" width="72" />
          <el-table-column prop="projectId" label="项目" width="88" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog v-model="agentDlg" :title="agentEditingId ? '编辑 Agent' : '新建 Agent'" width="560px" destroy-on-close>
    <el-form label-width="108px">
      <el-form-item v-if="agentEditingId" label="类型" required>
        <el-select v-model="agentForm.roleType" placeholder="选择类型" style="width: 100%">
          <el-option
            v-for="opt in agentRoleOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-else label="类型" required>
        <el-select
          v-model="agentForm.roleTypes"
          multiple
          collapse-tags
          collapse-tags-tooltip
          placeholder="可多选：PM、产品、前端、后端、测试等"
          style="width: 100%"
        >
          <el-option
            v-for="opt in agentRoleOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <div class="form-hint">每种类型各创建一个 Agent，共用下方名称前缀与通道。</div>
      </el-form-item>
      <el-form-item :label="agentEditingId ? '名称' : '名称前缀'" required>
        <el-input v-model="agentForm.name" :placeholder="agentEditingId ? 'Agent 名称' : '例如：默认、团队A'" />
      </el-form-item>
      <el-form-item label="大模型通道">
        <el-select
          v-model="agentForm.llmChannelId"
          placeholder="选择本人名下的通道（派工自动编排等场景必填）"
          filterable
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="c in llmChannels"
            :key="c.id"
            :label="`${c.channelName}（${c.providerName}）`"
            :value="c.id"
          />
        </el-select>
        <div class="form-hint">须与「大模型 → 通道配置」中当前账号创建的通道一致，否则自动编排会报未绑定/无权限。</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="agentDlg = false">取消</el-button>
      <el-button type="primary" @click="saveAgent">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="dispatchDlg" title="项目经理派工" width="520px" @opened="onDispatchDlgOpened">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mag-dispatch-hint"
      title="派工顺序（系统校验）"
      description="需求正文为空时仅可派产品；产品有未结项任务时勿再派产品；派测试前须前端/后端任务均已结项。"
    />
    <el-form label-width="100px" class="mag-dispatch-form">
      <el-form-item label="标题" required><el-input v-model="dispatchForm.title" placeholder="任务标题" /></el-form-item>
      <el-form-item label="说明"><el-input v-model="dispatchForm.description" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="功能模块">
        <el-select v-model="dispatchForm.moduleId" clearable filterable placeholder="可选" style="width: 100%">
          <el-option v-for="mod in modules" :key="mod.id" :label="moduleOptionLabel(mod)" :value="mod.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="执行 Agent" required>
        <el-select v-model="dispatchForm.assigneeAgentId" filterable placeholder="选择执行人" style="width: 100%">
          <el-option
            v-for="a in dispatchableAgents"
            :key="a.id"
            :label="`${a.name} (#${a.id}) · ${agentRoleLabel(a.roleType)}`"
            :value="a.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dispatchDlg = false">取消</el-button>
      <el-button type="primary" @click="saveDispatch">确认派工</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="reassignDlg" title="改派执行 Agent" width="480px">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mag-dispatch-hint"
      title="改派目标须满足与同派工一致的流水线规则。"
    />
    <el-form label-width="100px" class="mag-dispatch-form">
      <el-form-item label="任务"><span>{{ reassignTask?.title }}</span></el-form-item>
      <el-form-item label="执行 Agent" required>
        <el-select v-model="reassignForm.assigneeAgentId" filterable placeholder="选择执行人" style="width: 100%">
          <el-option
            v-for="a in dispatchableAgents"
            :key="a.id"
            :label="`${a.name} (#${a.id}) · ${agentRoleLabel(a.roleType)}`"
            :value="a.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reassignDlg = false">取消</el-button>
      <el-button type="primary" @click="saveReassign">确认改派</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="moduleDlg" :title="moduleEditId ? '编辑模块' : '新建模块'" width="440px">
    <el-form label-width="88px">
      <el-form-item label="名称" required><el-input v-model="moduleForm.name" /></el-form-item>
      <el-form-item label="父模块ID"><el-input v-model.number="moduleForm.parentId" placeholder="0 为根" /></el-form-item>
      <el-form-item label="标签"><el-input v-model="moduleForm.tag" /></el-form-item>
      <el-form-item label="排序"><el-input v-model.number="moduleForm.sortOrder" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="moduleDlg = false">取消</el-button>
      <el-button type="primary" @click="saveModule">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="blueprintDlg" title="导入蓝图" width="440px">
    <el-form label-width="100px">
      <el-form-item label="来源"><span>知识库 KB</span></el-form-item>
      <el-form-item label="KB 条目 ID" required><el-input v-model.number="blueprintForm.sourceId" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="blueprintDlg = false">取消</el-button>
      <el-button type="primary" @click="saveBlueprint">导入</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="magResultMdPreviewDlg" :title="magResultMdPreviewTitle" width="880px" destroy-on-close>
    <div class="mag-md-body mag-result-md-dialog-body" v-html="magResultMdPreviewRenderedHtml" />
  </el-dialog>

  <el-dialog v-model="revDlg" title="需求版本" width="720px">
    <el-table :data="revisions" border size="small" max-height="360">
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="authorUserId" label="作者" width="100" />
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column label="预览" width="100" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openReqRevisionMarkdownPreview(row)">预览</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog v-model="analyzeDlg" title="变更影响分析" width="520px">
    <el-input v-model="analyzeForm.changeSummary" type="textarea" :rows="5" placeholder="变更摘要" />
    <template #footer>
      <el-button @click="analyzeDlg = false">取消</el-button>
      <el-button type="primary" @click="saveAnalyze">提交分析</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="schedDlg" title="定时任务配置" width="480px">
    <el-form label-width="100px">
      <el-form-item label="ID（更新填）"><el-input v-model.number="schedForm.id" placeholder="新建留空" /></el-form-item>
      <el-form-item label="jobKey" required><el-input v-model="schedForm.jobKey" /></el-form-item>
      <el-form-item label="Cron" required><el-input v-model="schedForm.cronExpr" placeholder="0 0 * * * ?" /></el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="schedForm.enabled" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="项目 ID"><el-input v-model.number="schedForm.projectId" placeholder="全局可空" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="schedDlg = false">取消</el-button>
      <el-button type="primary" @click="saveSched">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="flowDlg" title="任务流程走向" width="920px" destroy-on-close align-center @opened="onFlowDlgOpened">
    <div v-if="flowTask" class="flow-task-head">
      <span>任务 #{{ flowTask.id }} · {{ flowTask.title }}</span>
      <div class="flow-task-head-tags">
        <el-tag size="small" type="info">{{ flowTask.state }}</el-tag>
        <el-tag size="small" :type="taskPhaseTagType(flowTask.state)">{{ taskPhaseLabel(flowTask.state) }}</el-tag>
      </div>
    </div>
    <el-tabs v-model="flowTab" @tab-change="onFlowTabChange">
      <el-tab-pane label="流程图" name="chart">
        <p class="flow-hint">
          下图按<strong>项目经理派工 → 执行方干活 → 申报结项</strong>展示标准走向；<strong>橙色高亮</strong>为当前所处环节。下方为系统已记录的实际事件链。
        </p>
        <el-steps
          class="task-flow-steps"
          :active="taskPipelineStepActive"
          finish-status="success"
          :process-status="flowTask?.state === 'BLOCKED' ? 'error' : 'process'"
          align-center
        >
          <el-step title="派工落地" description="PM / PM Agent 指定执行人" />
          <el-step title="待开始 → 执行中" description="执行方点击「开始」后进入干活" />
          <el-step title="申报完成" description="执行方提交后任务标记为已完成" />
          <el-step title="已完成" description="DONE" />
        </el-steps>
        <el-alert
          v-if="flowTask?.state === 'BLOCKED'"
          type="warning"
          :closable="false"
          show-icon
          class="flow-block-alert"
          title="当前任务处于阻塞状态，解除后请继续从「执行中」推进。"
        />
        <div class="flow-chart-caption flow-chart-caption-main">标准流程走向（当前位置高亮）</div>
        <div ref="mmdPipelineEl" class="mermaid-host mermaid-host-main" />
        <div class="flow-chart-caption">已发生事件串联（审计明细）</div>
        <div ref="mmdChainEl" class="mermaid-host" />
      </el-tab-pane>
      <el-tab-pane label="时间线" name="timeline">
        <el-empty v-if="!flowEvents.length" description="暂无流程事件（派工/开始/阻塞等操作后会记录）" />
        <el-timeline v-else>
          <el-timeline-item v-for="e in flowEvents" :key="e.id" :timestamp="formatFlowTime(e.createdAt)" placement="top">
            <div class="flow-ev-title">{{ flowEventTitle(e.eventType) }}</div>
            <div class="flow-ev-sum">{{ e.summary || '—' }}</div>
            <div class="flow-ev-meta">
              行为体 {{ e.actorType || '—' }}
              <template v-if="e.actorAgentId != null"> · Agent #{{ e.actorAgentId }}</template>
            </div>
            <pre v-if="e.detailJson" class="flow-detail">{{ formatFlowDetailJson(e.detailJson) }}</pre>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
      <el-tab-pane label="执行记录" name="executionLogs">
        <el-button size="small" style="margin-bottom: 8px" @click="loadFlowExecutionLogs">刷新</el-button>
        <el-empty
          v-if="!flowExecutionLogs.length"
          description="尚无编排执行留痕（任务关联 Agent 跑 Temporal 成功/失败或触发被拒后会出现）"
        />
        <el-table v-else :data="flowExecutionLogs" border size="small" max-height="360">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column label="结果" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="taskExecutionOutcomeTagType(row.executionOutcome)">
                {{ taskExecutionOutcomeLabel(row.executionOutcome) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="执行 Agent" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ magAgentName(row.agentId) }} (#{{ row.agentId }})</template>
          </el-table-column>
          <el-table-column prop="workflowId" label="workflowId" min-width="160" show-overflow-tooltip />
          <el-table-column prop="orchestrationRunId" label="编排 run" width="96" />
          <el-table-column label="摘要/错误" min-width="200">
            <template #default="{ row }">
              <div v-if="row.resultSummary" class="result-md-list-cell">
                <span class="result-md-list-text" :title="row.resultSummary">{{ row.resultSummary }}</span>
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="openResultMarkdownPreview('执行记录 · 摘要/错误', row.resultSummary)"
                >
                  预览
                </el-button>
              </div>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column prop="startedAt" label="开始" width="160" />
          <el-table-column prop="finishedAt" label="结束" width="160" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>

  <el-dialog v-model="threadDlg" :title="threadRoomTitle" width="720px" @opened="onThreadDlgOpened">
    <div class="thread-chat">
      <div ref="threadChatScrollRef" class="thread-chat-messages">
        <div v-if="!threadMessages.length" class="thread-chat-empty">暂无消息，可在下方发送 USER 消息。</div>
        <div
          v-for="m in threadMessages"
          :key="m.id"
          class="thread-chat-row"
          :class="{
            'is-user': threadMessageIsUser(m),
            'is-agent': threadMessageIsAgent(m),
            'is-system': threadMessageIsSystem(m),
          }"
        >
          <template v-if="threadMessageIsUser(m)">
            <div class="thread-chat-main user">
              <div class="thread-chat-bubble user">
                <template v-if="m.displayLines">
                  <div v-for="(line, idx) in m.displayLines" :key="idx" class="thread-chat-line">{{ line }}</div>
                </template>
                <template v-else>{{ m.content }}</template>
              </div>
              <div class="thread-chat-foot user">
                <span class="thread-chat-label">我</span>
                <span v-if="m.createdAt" class="thread-chat-time">{{ formatFlowTime(m.createdAt) }}</span>
              </div>
            </div>
          </template>
          <template v-else-if="threadMessageIsAgent(m)">
            <div class="thread-chat-avatar agent" :title="threadSenderMeta(m).title">
              {{ threadAvatarLetter(m) }}
            </div>
            <div class="thread-chat-main">
              <div class="thread-chat-head">
                <span class="thread-chat-name">{{ threadSenderMeta(m).title }}</span>
                <el-tag type="success" size="small" effect="plain">Agent</el-tag>
                <span v-if="m.senderAgentId != null" class="thread-chat-id">#{{ m.senderAgentId }}</span>
                <span v-if="threadSenderMeta(m).subtitle" class="thread-chat-role">{{ threadSenderMeta(m).subtitle }}</span>
              </div>
              <div class="thread-chat-bubble agent">
                <template v-if="m.displayLines">
                  <div v-for="(line, idx) in m.displayLines" :key="idx" class="thread-chat-line">{{ line }}</div>
                </template>
                <template v-else>{{ m.content }}</template>
              </div>
              <div v-if="m.createdAt" class="thread-chat-foot">
                <span class="thread-chat-time">{{ formatFlowTime(m.createdAt) }}</span>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="thread-chat-system">
              <span class="thread-chat-system-tag">{{ threadSenderMeta(m).title }}</span>
              <div v-if="m.displayLines" class="thread-chat-system-lines">
                <div v-for="(line, idx) in m.displayLines" :key="idx" class="thread-chat-line">{{ line }}</div>
              </div>
              <span v-else class="thread-chat-system-body">{{ m.content }}</span>
              <span v-if="m.createdAt" class="thread-chat-time">{{ formatFlowTime(m.createdAt) }}</span>
            </div>
          </template>
        </div>
      </div>
      <div class="thread-chat-composer">
        <el-input v-model="threadMsg.content" type="textarea" :rows="2" placeholder="发送 USER 消息内容" />
        <el-button v-permission="'mag:task:operate'" type="primary" @click="postThreadMsg">发送</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { renderMarkdownToSafeHtml } from '@/utils/renderMarkdown'
import {
  magGetProject,
  magListMembers,
  magListAgents,
  magCreateAgent,
  magUpdateAgent,
  magListTasks,
  magDispatchTask,
  magPmReassignTask,
  magStartTask,
  magGetRequirementDoc,
  magSaveRequirementDoc,
  magListThreads,
  magCreateThread,
  magListModules,
  magCreateModule,
  magUpdateModule,
  magDeleteModule,
  magImportBlueprint,
  magListAlerts,
  magAckAlert,
  magListScheduledJobs,
  magUpsertScheduledJob,
  magListReqRevisions,
  magGetRequirementRevision,
  magReqDiff,
  magReqAnalyzeChange,
  magListMessages,
  magPostMessage,
  magRunThread,
  magListOrchestrationRuns,
  magListTaskFlowEvents,
  magListTaskExecutionLogs,
} from '@/api/mag'
import { fetchLlmChannels } from '@/api/llm'
import { useMagWebSocket } from '@/composables/useMagWebSocket'

const route = useRoute()
const user = useUserStore()
const magWs = useMagWebSocket()
const projectId = computed(() => route.params.projectId)
const tab = ref('members')
const project = ref(null)
const members = ref([])
const agents = ref([])
const tasks = ref([])
const threads = ref([])
const reqContent = ref('')
const modules = ref([])
const alerts = ref([])
const schedJobs = ref([])
const revisions = ref([])
const diffV1 = ref(1)
const diffV2 = ref(2)
const diffText = ref('')
const orchestrationRuns = ref([])
const magResultMdPreviewDlg = ref(false)
const magResultMdPreviewTitle = ref('Markdown 预览')
const magResultMdPreviewMarkdown = ref('')

const dispatchableAgents = computed(() => agents.value || [])

/** 与 el-steps 四步对齐：派工=0；待开始/干活=1～2；申报/结项=3；DONE 时 active=4 表示全部完成 */
const taskPipelineStepActive = computed(() => {
  const s = flowTask.value?.state
  if (!s) return 0
  if (s === 'DONE') return 4
  if (s === 'IN_PROGRESS' || s === 'BLOCKED') return 2
  if (s === 'PENDING') return 1
  return 0
})

const agentRoleOptions = [
  { value: 'PM', label: 'PM' },
  { value: 'PRODUCT', label: '产品（PRODUCT）' },
  { value: 'BACKEND', label: '后端（BACKEND）' },
  { value: 'FRONTEND', label: '前端（FRONTEND）' },
  { value: 'TEST', label: '测试（TEST）' },
]

const agentDlg = ref(false)
const agentEditingId = ref(null)
const llmChannels = ref([])
/** 编辑用 roleType；新建批量用 roleTypes（与 agentRoleOptions.value 同枚举） */
const agentForm = ref({
  roleType: 'BACKEND',
  roleTypes: [],
  name: '',
  llmChannelId: null,
})

/** 批量创建时类型顺序固定，名称后缀可读 */
const agentRoleCreateOrder = ['PM', 'PRODUCT', 'BACKEND', 'FRONTEND', 'TEST']
const agentRoleNameSuffix = {
  PM: 'PM',
  PRODUCT: '产品',
  BACKEND: '后端',
  FRONTEND: '前端',
  TEST: '测试',
}
const dispatchDlg = ref(false)
const dispatchForm = ref({ title: '', description: '', moduleId: null, assigneeAgentId: null })
const reassignDlg = ref(false)
const reassignTask = ref(null)
const reassignForm = ref({ assigneeAgentId: null })
const flowDlg = ref(false)
const flowTask = ref(null)
const flowEvents = ref([])
const flowExecutionLogs = ref([])
const flowTab = ref('chart')
const mmdChainEl = ref(null)
const mmdPipelineEl = ref(null)

const FLOW_EVENT_LABELS = {
  TASK_CREATED: '创建任务',
  TASK_DISPATCHED: '派工（PM/真人/PM Agent 工具）',
  TASK_PM_REASSIGNED: '改派执行 Agent',
  TASK_STARTED: '开始干活',
  TASK_SUBMIT_COMPLETE: '申报完成',
  TASK_BLOCKED: '阻塞',
  TASK_REQUEST_NEXT: '要活（申领下一项）',
}

const moduleDlg = ref(false)
const moduleEditId = ref(null)
const moduleForm = ref({ name: '', parentId: 0, tag: '', sortOrder: 0 })
const blueprintDlg = ref(false)
const blueprintForm = ref({ sourceType: 'KB', sourceId: null })

const magResultMdPreviewRenderedHtml = computed(() => {
  const s = magResultMdPreviewMarkdown.value
  if (s == null || !String(s).trim()) {
    return renderMarkdownToSafeHtml('（无内容）')
  }
  return renderMarkdownToSafeHtml(s)
})

function openResultMarkdownPreview(title, markdown) {
  magResultMdPreviewTitle.value = title || 'Markdown 预览'
  magResultMdPreviewMarkdown.value = markdown == null ? '' : String(markdown)
  magResultMdPreviewDlg.value = true
}

const revDlg = ref(false)
const analyzeDlg = ref(false)
const analyzeForm = ref({ changeSummary: '' })
const schedDlg = ref(false)
const schedForm = ref({ id: null, jobKey: '', cronExpr: '0 0 * * * ?', enabled: 1, projectId: null })
const threadDlg = ref(false)
const threadRoom = ref(null)
const threadRoomTitle = computed(() => (threadRoom.value ? `线程 #${threadRoom.value.id}` : '线程'))
const threadMessages = ref([])
const threadMsg = ref({ content: '' })
const threadChatScrollRef = ref(null)

async function refreshAll() {
  const pid = projectId.value
  if (!pid) return
  const p = await magGetProject(pid)
  project.value = p.data
  const m = await magListMembers(pid)
  members.value = m.data
}

async function loadAgents() {
  const res = await magListAgents(projectId.value)
  agents.value = res.data
}

async function loadTasks() {
  const res = await magListTasks(projectId.value)
  tasks.value = res.data
}

async function loadThreads() {
  const res = await magListThreads(projectId.value)
  threads.value = res.data
}

async function loadOrchestrationRuns() {
  const pid = projectId.value
  if (!pid) return
  const res = await magListOrchestrationRuns(pid, { limit: 50 })
  orchestrationRuns.value = res.data || []
}

function notifyFromAlertPayload(alert) {
  if (!alert) return
  const type = alert.alertType
  let title = type || '告警'
  let message = ''
  try {
    const p = alert.payloadJson ? JSON.parse(alert.payloadJson) : {}
    if (p.title) title = String(p.title)
    if (p.message) message = String(p.message)
  } catch {
    /* ignore */
  }
  if (!message && alert.payloadJson) {
    message = String(alert.payloadJson).slice(0, 240)
  }
  if (type === 'PROJECT_ALL_TASKS_DONE') {
    ElNotification({
      title,
      message: message || '项目内全部任务均为已完成，请派发新任务。',
      type: 'success',
      duration: 0,
    })
    return
  }
  const level = alert.level
  if (level === 'ERROR' || level === 'WARN') {
    ElNotification({
      title,
      message: message || '(无说明)',
      type: level === 'ERROR' ? 'error' : 'warning',
      duration: 6000,
    })
  }
}

function setupMagWs() {
  magWs.disconnect()
  const pid = projectId.value
  if (!pid || !user.token) return
  magWs.connect({
    onOpen: () => {
      if (projectId.value) {
        magWs.subscribeProject(projectId.value)
      }
    },
    onMessage: (data) => {
      if (
        data?.event === 'mag.orchestration.run.updated' &&
        Number(data?.projectId) === Number(projectId.value)
      ) {
        loadOrchestrationRuns()
      }
      if (data?.event === 'mag.alert.new' && Number(data?.projectId) === Number(projectId.value)) {
        loadAlerts()
        if (tab.value === 'tasks') {
          loadTasks()
        }
        notifyFromAlertPayload(data.alert)
      }
    },
  })
}

const ORCH_STATUS_LABELS = {
  SUBMITTED: '已提交',
  RUNNING: '执行中',
  SUCCEEDED: '成功',
  FAILED: '失败',
  REJECTED: '未接受',
}

function orchStatusLabel(s) {
  return ORCH_STATUS_LABELS[s] || s || '—'
}

function orchStatusTagType(s) {
  if (s === 'SUCCEEDED') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'REJECTED') return 'warning'
  if (s === 'RUNNING') return 'warning'
  return 'info'
}

function orchRunKindLabel(k) {
  if (k === 'AGENT') return 'Agent'
  if (k === 'THREAD') return '线程'
  return k || '—'
}

async function loadReq() {
  const res = await magGetRequirementDoc(projectId.value)
  reqContent.value = res.data.content || ''
}

async function loadModules() {
  const res = await magListModules(projectId.value)
  modules.value = res.data
}

async function loadAlerts() {
  const res = await magListAlerts(projectId.value)
  alerts.value = res.data
}

async function loadSched() {
  const res = await magListScheduledJobs({ projectId: projectId.value })
  schedJobs.value = res.data
}

watch(tab, (t) => {
  if (t === 'agents') {
    loadLlmChannelsForAgent()
    loadAgents()
  }
  if (t === 'tasks') {
    loadAgents()
    loadModules()
    loadTasks()
  }
  if (t === 'threads') loadThreads()
  if (t === 'orchRuns') loadOrchestrationRuns()
  if (t === 'req') loadReq()
  if (t === 'modules') loadModules()
  if (t === 'alerts') loadAlerts()
  if (t === 'sched') loadSched()
})

watch(projectId, () => {
  refreshAll()
  setupMagWs()
}, { immediate: true })

onMounted(refreshAll)

function agentRoleLabel(value) {
  const o = agentRoleOptions.find((x) => x.value === value)
  return o ? o.label : value || '—'
}

async function loadLlmChannelsForAgent() {
  try {
    const res = await fetchLlmChannels()
    llmChannels.value = res.data || []
  } catch {
    llmChannels.value = []
  }
}

function agentChannelLabel(channelId) {
  if (channelId == null) return '—'
  const c = llmChannels.value.find((x) => x.id === channelId)
  return c ? `${c.channelName}（${c.providerName}）` : `#${channelId}`
}

function openAgent() {
  agentEditingId.value = null
  agentForm.value = { roleType: 'BACKEND', roleTypes: [], name: '', llmChannelId: null }
  loadLlmChannelsForAgent()
  agentDlg.value = true
}

function openEditAgent(row) {
  agentEditingId.value = row.id
  agentForm.value = {
    roleType: row.roleType || 'BACKEND',
    roleTypes: [],
    name: row.name || '',
    llmChannelId: row.llmChannelId ?? null,
  }
  loadLlmChannelsForAgent()
  agentDlg.value = true
}

async function saveAgent() {
  if (!agentForm.value.name?.trim()) {
    ElMessage.warning(agentEditingId.value ? '请输入名称' : '请输入名称前缀')
    return
  }
  const baseName = agentForm.value.name.trim()
  const llmChannelId = agentForm.value.llmChannelId ?? null

  if (agentEditingId.value) {
    const body = {
      roleType: agentForm.value.roleType,
      name: baseName,
      llmChannelId,
      applyLlmChannelId: true,
    }
    await magUpdateAgent(agentEditingId.value, body)
    ElMessage.success('已保存')
    agentDlg.value = false
    loadAgents()
    return
  }

  const picked = agentForm.value.roleTypes || []
  if (picked.length === 0) {
    ElMessage.warning('请至少选择一种 Agent 类型')
    return
  }
  const ordered = [
    ...agentRoleCreateOrder.filter((r) => picked.includes(r)),
    ...picked.filter((r) => !agentRoleCreateOrder.includes(r)),
  ]
  let ok = 0
  let lastErr = ''
  for (const rt of ordered) {
    const suffix = agentRoleNameSuffix[rt] || rt
    const body = {
      roleType: rt,
      name: `${baseName}-${suffix}`,
      llmChannelId,
    }
    try {
      await magCreateAgent(projectId.value, body)
      ok += 1
    } catch (e) {
      lastErr = e?.response?.data?.message || e?.message || String(e)
      break
    }
  }
  if (ok === ordered.length) {
    ElMessage.success(`已创建 ${ok} 个 Agent`)
    agentDlg.value = false
  } else if (ok > 0) {
    ElMessage.warning(`已创建 ${ok}/${ordered.length} 个，未全部成功（详见上方错误提示）`)
    agentDlg.value = false
  }
  // ok===0 时 axios 拦截器已弹出错误，此处不再重复
  loadAgents()
}

function moduleOptionLabel(mod) {
  const p = mod.parentId ? `#${mod.parentId} · ` : ''
  return `${p}${mod.name || ''}`
}

function assigneeAgentLabel(id) {
  if (id == null) return '—'
  const a = agents.value.find((x) => x.id === id)
  return a ? `${a.name} (#${a.id})` : `#${id}`
}

/** 聊天内协调载荷等场景：优先展示 MagAgent.name */
function magAgentName(id) {
  if (id == null) return '—'
  const a = agents.value.find((x) => x.id === id)
  const n = a?.name?.trim()
  if (n) return n
  return `Agent #${id}`
}

async function onDispatchDlgOpened() {
  await Promise.all([loadAgents(), loadModules()])
}

function openDispatch() {
  dispatchForm.value = { title: '', description: '', moduleId: null, assigneeAgentId: null }
  dispatchDlg.value = true
}

async function saveDispatch() {
  if (!dispatchForm.value.title?.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (dispatchForm.value.assigneeAgentId == null) {
    ElMessage.warning('请选择执行 Agent')
    return
  }
  const body = {
    title: dispatchForm.value.title.trim(),
    assigneeAgentId: dispatchForm.value.assigneeAgentId,
  }
  if (dispatchForm.value.description?.trim()) {
    body.description = dispatchForm.value.description.trim()
  }
  if (dispatchForm.value.moduleId != null) {
    body.moduleId = dispatchForm.value.moduleId
  }
  await magDispatchTask(projectId.value, body)
  ElMessage.success('已派工')
  dispatchDlg.value = false
  loadTasks()
}

function openReassign(row) {
  reassignTask.value = row
  reassignForm.value = { assigneeAgentId: row.assigneeAgentId ?? null }
  reassignDlg.value = true
}

async function saveReassign() {
  if (reassignForm.value.assigneeAgentId == null) {
    ElMessage.warning('请选择执行 Agent')
    return
  }
  await magPmReassignTask(reassignTask.value.id, { assigneeAgentId: reassignForm.value.assigneeAgentId })
  ElMessage.success('已改派')
  reassignDlg.value = false
  loadTasks()
}

function flowEventTitle(type) {
  return FLOW_EVENT_LABELS[type] || type || '—'
}

function taskPhaseLabel(state) {
  const m = {
    PENDING: '待开始',
    IN_PROGRESS: '执行中',
    BLOCKED: '阻塞',
    DONE: '已完成',
  }
  return m[state] || state || '—'
}

function taskPhaseTagType(state) {
  if (state === 'DONE') return 'success'
  if (state === 'BLOCKED') return 'danger'
  if (state === 'IN_PROGRESS') return 'warning'
  return 'info'
}

function taskExecutionOutcomeLabel(outcome) {
  const m = { SUCCEEDED: '成功', FAILED: '失败', TRIGGER_REJECTED: '触发被拒' }
  return m[outcome] || outcome || '—'
}

function taskExecutionOutcomeTagType(outcome) {
  if (outcome === 'SUCCEEDED') return 'success'
  if (outcome === 'FAILED') return 'danger'
  if (outcome === 'TRIGGER_REJECTED') return 'warning'
  return 'info'
}

function formatFlowTime(t) {
  if (t == null) return ''
  const s = String(t)
  return s.length > 19 ? s.slice(0, 19) : s
}

function formatFlowDetailJson(raw) {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

function sanitizeMmdLabel(s) {
  if (s == null) return ''
  return String(s).replace(/["\n\r]/g, ' ').replace(/\[/g, '(').replace(/\]/g, ')').slice(0, 96)
}

function buildTaskChainMermaid(events) {
  const lines = ['flowchart LR']
  if (!events.length) {
    lines.push('  empty[暂无已记录事件]')
    return lines.join('\n')
  }
  events.forEach((e, i) => {
    const typ = sanitizeMmdLabel(flowEventTitle(e.eventType))
    const actor = e.actorType === 'AGENT' ? 'Agent' : '人'
    const aid = e.actorAgentId != null ? `#${e.actorAgentId}` : ''
    const ts = sanitizeMmdLabel(formatFlowTime(e.createdAt))
    const label = `${typ}<br/>${actor}${aid}<br/>${ts}`
    lines.push(`  E${i}["${label}"]`)
    if (i > 0) {
      lines.push(`  E${i - 1} --> E${i}`)
    }
  })
  return lines.join('\n')
}

function pipelineHighlightNodeId(state) {
  switch (state) {
    case 'PENDING':
      return 'S1'
    case 'IN_PROGRESS':
      return 'S2'
    case 'BLOCKED':
      return 'SB'
    case 'DONE':
      return 'S4'
    default:
      return 'S1'
  }
}

function buildMainPipelineMermaid(state) {
  const cur = pipelineHighlightNodeId(state)
  return [
    'flowchart TB',
    '  S0["项目经理派工<br/>指定执行 Agent"]',
    '  S1["待执行方开始<br/>PENDING"]',
    '  S2["执行方干活<br/>IN_PROGRESS"]',
    '  SB["阻塞<br/>BLOCKED"]',
    '  S4["已完成<br/>DONE"]',
    '  S0 --> S1',
    '  S1 --> S2',
    '  S2 --> S4',
    '  S2 -. 阻塞 .-> SB',
    '  classDef cur fill:#fff3c4,stroke:#f57c00,stroke-width:3px,color:#333',
    `  class ${cur} cur`,
  ].join('\n')
}

async function renderFlowMermaid() {
  if (!mmdChainEl.value || !mmdPipelineEl.value || !flowTask.value) return
  const mermaid = (await import('mermaid')).default
  mermaid.initialize({ startOnLoad: false, securityLevel: 'loose', theme: 'neutral', fontFamily: 'inherit' })
  const pipelineDef = buildMainPipelineMermaid(flowTask.value.state)
  const chainDef = buildTaskChainMermaid(flowEvents.value)
  const t = Date.now()
  try {
    const { svg } = await mermaid.render(`mmd-pipe-${t}`, pipelineDef)
    mmdPipelineEl.value.innerHTML = svg
  } catch {
    mmdPipelineEl.value.innerHTML = '<p class="flow-mmd-fail">主流程图渲染失败</p>'
  }
  try {
    const { svg } = await mermaid.render(`mmd-chain-${t}`, chainDef)
    mmdChainEl.value.innerHTML = svg
  } catch {
    mmdChainEl.value.innerHTML = '<p class="flow-mmd-fail">事件链图渲染失败</p>'
  }
}

async function onFlowTabChange(name) {
  if (name === 'chart' && flowDlg.value) {
    await nextTick()
    await renderFlowMermaid()
  }
  if (name === 'executionLogs' && flowTask.value) {
    await loadFlowExecutionLogs()
  }
}

function onFlowDlgOpened() {
  onFlowTabChange(flowTab.value)
}

async function loadFlowExecutionLogs() {
  if (!flowTask.value) return
  try {
    const res = await magListTaskExecutionLogs(flowTask.value.id)
    flowExecutionLogs.value = res.data || []
  } catch {
    flowExecutionLogs.value = []
    ElMessage.warning('加载执行记录失败')
  }
}

async function openTaskFlow(row) {
  flowTask.value = row
  flowTab.value = 'chart'
  flowEvents.value = []
  flowExecutionLogs.value = []
  flowDlg.value = true
  try {
    const res = await magListTaskFlowEvents(row.id)
    flowEvents.value = res.data || []
  } catch {
    ElMessage.warning('加载流程事件失败')
    flowEvents.value = []
  }
  await nextTick()
  await renderFlowMermaid()
}

async function startTask(row) {
  await magStartTask(row.id)
  ElMessage.success('已开始')
  loadTasks()
}

async function saveReq() {
  await magSaveRequirementDoc(projectId.value, { content: reqContent.value })
  ElMessage.success('已保存')
}

async function addThread() {
  await magCreateThread(projectId.value, { title: '讨论' })
  ElMessage.success('已创建线程')
  loadThreads()
}

function openModule() {
  moduleEditId.value = null
  moduleForm.value = { name: '', parentId: 0, tag: '', sortOrder: 0 }
  moduleDlg.value = true
}

function editModule(row) {
  moduleEditId.value = row.id
  moduleForm.value = {
    name: row.name,
    parentId: row.parentId,
    tag: row.tag || '',
    sortOrder: row.sortOrder ?? 0,
  }
  moduleDlg.value = true
}

async function saveModule() {
  const pid = projectId.value
  const body = {
    name: moduleForm.value.name,
    parentId: moduleForm.value.parentId || 0,
    tag: moduleForm.value.tag || null,
    sortOrder: moduleForm.value.sortOrder ?? 0,
  }
  if (moduleEditId.value) {
    await magUpdateModule(pid, moduleEditId.value, body)
  } else {
    await magCreateModule(pid, body)
  }
  ElMessage.success('已保存')
  moduleDlg.value = false
  loadModules()
}

async function removeModule(row) {
  await ElMessageBox.confirm(`删除模块「${row.name}」？`, '确认', { type: 'warning' })
  await magDeleteModule(projectId.value, row.id)
  ElMessage.success('已删除')
  loadModules()
}

function openBlueprint() {
  blueprintForm.value = { sourceType: 'KB', sourceId: null }
  blueprintDlg.value = true
}

async function saveBlueprint() {
  if (!blueprintForm.value.sourceId) {
    ElMessage.warning('请填写 KB 条目 ID')
    return
  }
  const res = await magImportBlueprint(projectId.value, {
    sourceType: 'KB',
    sourceId: blueprintForm.value.sourceId,
  })
  ElMessage.success(`已导入 ${res.data?.length ?? 0} 条模块`)
  blueprintDlg.value = false
  loadModules()
}

async function openRevisions() {
  const res = await magListReqRevisions(projectId.value)
  revisions.value = res.data
  revDlg.value = true
}

async function openReqRevisionMarkdownPreview(row) {
  if (row?.id == null) return
  try {
    const res = await magGetRequirementRevision(projectId.value, row.id)
    const md = res.data?.content != null ? String(res.data.content) : ''
    openResultMarkdownPreview(`需求文档 v${row.version} · Markdown`, md)
  } catch {
    ElMessage.warning('加载该版本正文失败')
  }
}

async function loadDiff() {
  if (!diffV1.value || !diffV2.value) return
  const res = await magReqDiff(projectId.value, diffV1.value, diffV2.value)
  diffText.value = res.data.unifiedTextDiff || (res.data.same ? '（内容相同）' : '')
}

function openAnalyze() {
  analyzeForm.value = { changeSummary: '' }
  analyzeDlg.value = true
}

async function saveAnalyze() {
  if (!analyzeForm.value.changeSummary.trim()) return
  const res = await magReqAnalyzeChange(projectId.value, {
    changeSummary: analyzeForm.value.changeSummary,
  })
  ElMessage.success(`traceId: ${res.data.traceId}`)
  analyzeDlg.value = false
}

async function ackAlert(row) {
  await magAckAlert(row.id)
  ElMessage.success('已确认')
  loadAlerts()
}

function openSched() {
  schedForm.value = {
    id: null,
    jobKey: '',
    cronExpr: '0 0 * * * ?',
    enabled: 1,
    projectId: Number(projectId.value) || null,
  }
  schedDlg.value = true
}

async function saveSched() {
  if (!schedForm.value.jobKey.trim() || !schedForm.value.cronExpr.trim()) return
  await magUpsertScheduledJob({
    id: schedForm.value.id || undefined,
    jobKey: schedForm.value.jobKey,
    cronExpr: schedForm.value.cronExpr,
    enabled: schedForm.value.enabled,
    projectId: schedForm.value.projectId || null,
  })
  ElMessage.success('已保存')
  schedDlg.value = false
  loadSched()
}

function threadCoordJsonPayload(raw) {
  if (raw == null) return null
  const s = String(raw).trim()
  if (!s.startsWith('{')) return null
  try {
    const o = JSON.parse(s)
    if (!o || typeof o !== 'object' || o.kind == null) return null
    return o
  } catch {
    return null
  }
}

/** 后端协调线程写入的 JSON content（ASSIGN / BLOCK / REQUEST_NEXT / REQ_CHANGE_ANALYZE 等）→ 可读多行文案；assigneeAgentId、agentId 用 MagAgent.name */
function buildThreadStructuredLines(content) {
  const p = threadCoordJsonPayload(content)
  if (!p) return null
  const kind = String(p.kind || '').toUpperCase()
  if (kind === 'ASSIGN') {
    const title = p.title != null && String(p.title).trim() !== '' ? String(p.title).trim() : '（无标题）'
    const tid = p.taskId != null ? String(p.taskId) : '—'
    return [`派工：${title}`, `任务 ID：${tid}`, `执行人：${magAgentName(p.assigneeAgentId)}`]
  }
  if (kind === 'BLOCK') {
    const lines = [`任务阻塞 · 任务 ID：${p.taskId != null ? String(p.taskId) : '—'}`]
    if (p.reason) lines.push(`原因：${p.reason}`)
    return lines
  }
  if (kind === 'REQUEST_NEXT') {
    return [
      '申领下一项工作',
      `Agent：${magAgentName(p.agentId)}`,
      `任务 ID：${p.taskId != null ? String(p.taskId) : '—'}`,
    ]
  }
  if (kind === 'REQ_CHANGE_ANALYZE') {
    const lines = ['需求变更分析']
    if (p.traceId) lines.push(`追踪 ID：${p.traceId}`)
    if (p.summary) lines.push(`摘要：${p.summary}`)
    return lines
  }
  if (kind === 'A2A_INVOKE') {
    const lines = [
      'Agent2Agent 调用',
      `调用方：${magAgentName(p.callerAgentId)}`,
      `被调用方：${magAgentName(p.calleeAgentId)}`,
    ]
    if (p.instruction) {
      const t = String(p.instruction)
      lines.push(`说明：${t.length > 4000 ? `${t.slice(0, 4000)}…` : t}`)
    }
    return lines
  }
  if (kind === 'ORCH_ENTER') {
    const lines = [
      '编排执行进入',
      `执行 Agent：${magAgentName(p.agentId)}`,
      `触发用户 ID：${p.triggerUserId != null ? String(p.triggerUserId) : '—'}`,
    ]
    if (p.instruction) {
      const t = String(p.instruction)
      lines.push(`说明：${t.length > 4000 ? `${t.slice(0, 4000)}…` : t}`)
    }
    return lines
  }
  return null
}

function setThreadMessagesFromApi(rows) {
  threadMessages.value = (rows || []).map((row) => ({
    ...row,
    displayLines: buildThreadStructuredLines(row.content),
  }))
}

function threadAgentById(id) {
  if (id == null) return null
  return (agents.value || []).find((a) => a.id === id) || null
}

function threadMessageIsUser(m) {
  return String(m.senderType || '').toUpperCase() === 'USER'
}

function threadMessageIsAgent(m) {
  return String(m.senderType || '').toUpperCase() === 'AGENT'
}

function threadMessageIsSystem(m) {
  return String(m.senderType || '').toUpperCase() === 'SYSTEM'
}

function threadSenderMeta(m) {
  const st = String(m.senderType || '').toUpperCase()
  if (st === 'USER') {
    return { title: '我', subtitle: '' }
  }
  if (st === 'AGENT') {
    const a = threadAgentById(m.senderAgentId)
    const idPart = m.senderAgentId != null ? `#${m.senderAgentId}` : ''
    const name = a?.name?.trim() ? a.name.trim() : m.senderAgentId != null ? `Agent ${idPart}` : 'Agent'
    const role = a?.roleType ? agentRoleLabel(a.roleType) : ''
    return { title: name, subtitle: role }
  }
  if (st === 'SYSTEM') {
    return { title: '系统', subtitle: '' }
  }
  return { title: m.senderType || '—', subtitle: '' }
}

function threadAvatarLetter(m) {
  const meta = threadSenderMeta(m)
  const t = meta.title || 'A'
  const ch = t.trim().charAt(0)
  return ch || 'A'
}

async function scrollThreadChatToBottom() {
  await nextTick()
  const el = threadChatScrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function openThreadRoom(row) {
  threadRoom.value = row
  threadMsg.value = { content: '' }
  threadDlg.value = true
}

async function onThreadDlgOpened() {
  if (!threadRoom.value) return
  await loadAgents()
  const res = await magListMessages(threadRoom.value.id)
  setThreadMessagesFromApi(res.data)
  await scrollThreadChatToBottom()
}

async function postThreadMsg() {
  if (!threadRoom.value || !threadMsg.value.content.trim()) return
  await magPostMessage(threadRoom.value.id, {
    senderType: 'USER',
    senderAgentId: null,
    content: threadMsg.value.content,
  })
  threadMsg.value.content = ''
  const res = await magListMessages(threadRoom.value.id)
  setThreadMessagesFromApi(res.data)
  await scrollThreadChatToBottom()
}

function runResultMessage(data) {
  const base = data?.message || (data?.accepted === false ? '编排未接受' : '已接受')
  if (data?.workflowId) {
    return `${base} workflowId=${data.workflowId}`
  }
  return base
}

async function runThreadRow(row) {
  const res = await magRunThread(row.id)
  const data = res.data
  const msg = runResultMessage(data)
  if (data?.accepted === false) {
    ElMessage.warning(msg)
  } else {
    ElMessage.success(msg)
  }
  await loadOrchestrationRuns()
}

</script>

<style scoped>
.mag-dispatch-hint {
  margin-bottom: 12px;
}
.head {
  margin-bottom: 16px;
}
.title-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
}
.title-row h2 {
  margin: 0;
  font-size: 1.25rem;
}
.meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
}
.toolbar.wrap {
  flex-wrap: wrap;
}
.thread-chat {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}
.thread-chat-messages {
  max-height: 420px;
  overflow-y: auto;
  padding: 8px 4px 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
}
.thread-chat-empty {
  text-align: center;
  padding: 32px 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.thread-chat-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}
.thread-chat-row:last-child {
  margin-bottom: 4px;
}
.thread-chat-row.is-user {
  flex-direction: row-reverse;
}
.thread-chat-row.is-system {
  justify-content: center;
}
.thread-chat-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--el-color-success), var(--el-color-success-dark-2));
}
.thread-chat-main {
  min-width: 0;
  max-width: calc(100% - 46px);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.thread-chat-main.user {
  max-width: 85%;
  align-items: flex-end;
}
.thread-chat-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
  font-size: 12px;
  line-height: 1.3;
}
.thread-chat-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.thread-chat-id {
  color: var(--el-text-color-secondary);
  font-family: ui-monospace, monospace;
}
.thread-chat-role {
  color: var(--el-text-color-secondary);
}
.thread-chat-bubble {
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}
.thread-chat-bubble.user {
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-5);
  color: var(--el-text-color-primary);
}
.thread-chat-bubble.agent {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  color: var(--el-text-color-primary);
}
.thread-chat-line {
  line-height: 1.55;
}
.thread-chat-line + .thread-chat-line {
  margin-top: 6px;
}
.thread-chat-line:first-child {
  font-weight: 600;
}
.thread-chat-system-lines {
  display: block;
  margin-top: 6px;
  text-align: left;
  width: 100%;
}
.thread-chat-system-lines .thread-chat-line:first-child {
  font-weight: 600;
}
.thread-chat-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.thread-chat-foot.user {
  flex-direction: row-reverse;
}
.thread-chat-label {
  font-weight: 500;
  color: var(--el-color-primary);
}
.thread-chat-time {
  opacity: 0.85;
}
.thread-chat-system {
  margin: 0 auto;
  max-width: 92%;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--el-fill-color);
  border: 1px dashed var(--el-border-color);
  font-size: 13px;
  text-align: center;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
}
.thread-chat-system-tag {
  display: inline-block;
  margin-right: 8px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}
.thread-chat-system-body {
  display: inline;
}
.thread-chat-composer {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.thread-chat-composer .el-textarea {
  flex: 1;
}
.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
.flow-task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 14px;
}
.flow-task-head-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.task-flow-steps {
  margin: 16px 0 20px;
  padding: 12px 8px;
  background: var(--el-fill-color-blank);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
}
.flow-block-alert {
  margin-bottom: 16px;
}
.flow-chart-caption-main {
  margin-top: 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.mermaid-host-main {
  min-height: 220px;
  margin-bottom: 20px;
}
.flow-ev-title {
  font-weight: 600;
  margin-bottom: 4px;
}
.flow-ev-sum {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.flow-ev-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.flow-detail {
  margin: 8px 0 0;
  padding: 8px;
  font-size: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  overflow-x: auto;
  max-height: 160px;
}
.flow-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin: 0 0 12px;
  line-height: 1.5;
}
.flow-chart-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.flow-chart-box {
  flex: 1 1 280px;
  min-width: 0;
}
.flow-chart-caption {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.mermaid-host {
  overflow-x: auto;
  min-height: 120px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 8px;
  background: var(--el-bg-color);
}
.flow-mmd-fail {
  font-size: 12px;
  color: var(--el-color-warning);
  margin: 8px;
}

/* 列表内纯文本 +「预览」：不在表格里渲染 Markdown */
.result-md-list-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.result-md-list-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.mag-result-md-dialog-body {
  max-height: min(70vh, 560px);
  overflow-y: auto;
  padding-right: 4px;
}

/* Markdown 预览（v-html + DOMPurify） */
.mag-md-body {
  font-size: 14px;
  line-height: 1.65;
  color: var(--el-text-color-primary);
  word-break: break-word;
}
.mag-md-body :deep(h1) {
  font-size: 1.35em;
  margin: 0.75em 0 0.4em;
  font-weight: 600;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 0.25em;
}
.mag-md-body :deep(h2) {
  font-size: 1.2em;
  margin: 0.85em 0 0.4em;
  font-weight: 600;
}
.mag-md-body :deep(h3) {
  font-size: 1.08em;
  margin: 0.75em 0 0.35em;
  font-weight: 600;
}
.mag-md-body :deep(p) {
  margin: 0.5em 0;
}
.mag-md-body :deep(ul),
.mag-md-body :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.5em;
}
.mag-md-body :deep(li) {
  margin: 0.2em 0;
}
.mag-md-body :deep(blockquote) {
  margin: 0.6em 0;
  padding: 0.35em 0.85em;
  border-left: 4px solid var(--el-color-primary-light-5);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}
.mag-md-body :deep(pre) {
  margin: 0.65em 0;
  padding: 12px 14px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  overflow-x: auto;
  font-size: 12px;
  line-height: 1.45;
}
.mag-md-body :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.9em;
}
.mag-md-body :deep(p code),
.mag-md-body :deep(li code) {
  padding: 0.1em 0.35em;
  background: var(--el-fill-color);
  border-radius: 4px;
}
.mag-md-body :deep(pre code) {
  padding: 0;
  background: transparent;
}
.mag-md-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.65em 0;
  font-size: 13px;
}
.mag-md-body :deep(th),
.mag-md-body :deep(td) {
  border: 1px solid var(--el-border-color);
  padding: 6px 10px;
  text-align: left;
}
.mag-md-body :deep(th) {
  background: var(--el-fill-color-light);
}
.mag-md-body :deep(a) {
  color: var(--el-color-primary);
}
.mag-md-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--el-border-color-lighter);
  margin: 1em 0;
}
</style>
