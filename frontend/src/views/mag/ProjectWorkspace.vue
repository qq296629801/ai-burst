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
          <el-table-column label="编排" width="120">
            <template #default="{ row }">
              <el-button
                v-permission="'mag:task:operate'"
                link
                type="primary"
                size="small"
                @click="runAgentRow(row)"
              >
                触发 run
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
        <div class="toolbar">
          <el-button v-permission="'mag:task:operate'" type="primary" size="small" @click="openTask">
            新建任务
          </el-button>
        </div>
        <el-table :data="tasks" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="state" label="状态" width="140" />
          <el-table-column label="操作" width="280">
            <template #default="{ row }">
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
              <el-button
                v-permission="'mag:task:operate'"
                v-if="row.state === 'IN_PROGRESS'"
                link
                type="primary"
                size="small"
                @click="submitDone(row)"
              >
                申报完成
              </el-button>
              <el-button
                v-permission="'mag:task:operate'"
                v-if="row.state !== 'DONE' && row.state !== 'BLOCKED'"
                link
                type="warning"
                size="small"
                @click="openBlock(row)"
              >
                阻塞
              </el-button>
              <el-button
                v-permission="'mag:task:operate'"
                link
                type="primary"
                size="small"
                @click="openRequestNext(row)"
              >
                要活
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
      <el-tab-pane label="需求池" name="pool">
        <el-table :data="pool" border stripe size="small">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="state" label="状态" width="180">
            <template #default="{ row }">{{ poolStateLabel(row.state) }}</template>
          </el-table-column>
          <el-table-column prop="payloadJson" label="载荷" show-overflow-tooltip />
          <el-table-column label="操作" width="320">
            <template #default="{ row }">
              <template v-if="row.state === 'PENDING_USER'">
                <el-button
                  v-permission="'mag:pool:decide'"
                  link
                  type="primary"
                  size="small"
                  @click="decide(row, 'APPROVE_AS_IS')"
                >
                  原样通过
                </el-button>
                <el-button
                  v-permission="'mag:pool:decide'"
                  link
                  type="primary"
                  size="small"
                  @click="decide(row, 'APPROVE_WITH_CHANGE')"
                >
                  变更通过
                </el-button>
                <el-button
                  v-permission="'mag:pool:decide'"
                  link
                  type="danger"
                  size="small"
                  @click="decide(row, 'REJECT')"
                >
                  拒绝
                </el-button>
                <el-button
                  v-permission="'mag:pool:decide'"
                  link
                  size="small"
                  @click="decide(row, 'DEFER')"
                >
                  延期
                </el-button>
              </template>
              <el-button
                v-permission="'mag:req:edit'"
                v-if="row.state !== 'CLOSED_BY_PRODUCT'"
                link
                type="warning"
                size="small"
                @click="openProductClose(row)"
              >
                产品结案
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="归档" name="releases">
        <el-button v-permission="'mag:release:archive'" type="primary" size="small" @click="openRelease">
          新建归档
        </el-button>
        <el-table :data="releases" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="versionLabel" label="版本" />
          <el-table-column prop="qualityFlag" label="优质" width="80" />
          <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
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
          <el-table-column prop="resultSummary" label="结果/错误" min-width="120" show-overflow-tooltip />
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
      <el-tab-pane v-if="user.hasPerm('mag:audit:fetch:view')" label="外网审计" name="fetchAudit">
        <el-button size="small" @click="loadFetchAudit">刷新</el-button>
        <el-table :data="fetchAudits" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="normalizedUrl" label="URL" show-overflow-tooltip />
          <el-table-column prop="httpStatus" label="HTTP" width="80" />
          <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="PM协助" name="pmAssist">
        <div class="toolbar">
          <el-button v-permission="'mag:agent:manage'" type="primary" size="small" @click="openPmAssist">
            登记协助
          </el-button>
          <el-button size="small" @click="loadPmAssist">刷新</el-button>
        </div>
        <el-table :data="pmAssists" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="problemType" label="类型" width="120" />
          <el-table-column prop="rootCauseSummary" label="根因摘要" show-overflow-tooltip />
          <el-table-column prop="resolved" label="已解决" width="80" />
          <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="改进" name="improvements">
        <div class="toolbar wrap">
          <el-select v-model="improveAgentId" placeholder="选择 Agent" filterable style="width: 220px" clearable>
            <el-option v-for="a in agents" :key="a.id" :label="`${a.name} (#${a.id})`" :value="a.id" />
          </el-select>
          <el-button size="small" :disabled="!improveAgentId" @click="loadImprovements">加载日志</el-button>
          <el-button
            v-permission="'mag:agent:manage'"
            type="primary"
            size="small"
            :disabled="!improveAgentId"
            @click="openImprovement"
          >
            追加记录
          </el-button>
        </div>
        <el-table :data="improvements" border stripe size="small" style="margin-top: 8px">
          <el-table-column prop="changeType" label="类型" width="120" />
          <el-table-column prop="summary" label="摘要" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="180" />
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

  <el-dialog v-model="agentDlg" :title="agentEditingId ? '编辑 Agent' : '新建 Agent'" width="520px" destroy-on-close>
    <el-form label-width="108px">
      <el-form-item label="类型" required>
        <el-select v-model="agentForm.roleType" placeholder="选择类型" style="width: 100%">
          <el-option
            v-for="opt in agentRoleOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="名称" required><el-input v-model="agentForm.name" /></el-form-item>
      <el-form-item label="大模型通道">
        <el-select
          v-model="agentForm.llmChannelId"
          placeholder="选择本人名下的通道（触发编排必填）"
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
        <div class="form-hint">须与「大模型 → 通道配置」中当前账号创建的通道一致，否则触发 run 会报未绑定/无权限。</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="agentDlg = false">取消</el-button>
      <el-button type="primary" @click="saveAgent">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="taskDlg" title="任务" width="480px">
    <el-form label-width="72px">
      <el-form-item label="标题" required><el-input v-model="taskForm.title" /></el-form-item>
      <el-form-item label="说明"><el-input v-model="taskForm.description" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="taskDlg = false">取消</el-button>
      <el-button type="primary" @click="saveTask">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="relDlg" title="归档" width="480px">
    <el-form label-width="100px">
      <el-form-item label="版本标签" required><el-input v-model="relForm.versionLabel" /></el-form-item>
      <el-form-item label="快照 JSON"><el-input v-model="relForm.snapshotJson" type="textarea" :rows="4" /></el-form-item>
      <el-form-item label="优质候选">
        <el-switch v-model="relForm.qualityFlag" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="relDlg = false">取消</el-button>
      <el-button type="primary" @click="saveRelease">保存</el-button>
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
      <el-form-item label="来源类型" required>
        <el-select v-model="blueprintForm.sourceType" style="width: 100%">
          <el-option label="归档 ARCHIVE" value="ARCHIVE" />
          <el-option label="知识库 KB" value="KB" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源 ID" required><el-input v-model.number="blueprintForm.sourceId" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="blueprintDlg = false">取消</el-button>
      <el-button type="primary" @click="saveBlueprint">导入</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="blockDlg" title="任务阻塞" width="440px">
    <el-form label-width="100px">
      <el-form-item label="原因" required><el-input v-model="blockForm.reason" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="阻塞 Agent">
        <el-input v-model.number="blockForm.blockedByAgentId" placeholder="可选" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="blockDlg = false">取消</el-button>
      <el-button type="primary" @click="saveBlock">确认阻塞</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="reqNextDlg" title="要活 request-next" width="440px">
    <el-form label-width="100px">
      <el-form-item label="Agent" required>
        <el-select v-model="reqNextForm.agentId" filterable style="width: 100%">
          <el-option v-for="a in agents" :key="a.id" :label="`${a.name} (#${a.id})`" :value="a.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reqNextDlg = false">取消</el-button>
      <el-button type="primary" @click="saveRequestNext">提交</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="revDlg" title="需求版本" width="720px">
    <el-table :data="revisions" border size="small" max-height="360">
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="authorUserId" label="作者" width="100" />
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column prop="contentPreview" label="预览" show-overflow-tooltip />
    </el-table>
  </el-dialog>

  <el-dialog v-model="analyzeDlg" title="变更影响分析" width="520px">
    <el-input v-model="analyzeForm.changeSummary" type="textarea" :rows="5" placeholder="变更摘要" />
    <template #footer>
      <el-button @click="analyzeDlg = false">取消</el-button>
      <el-button type="primary" @click="saveAnalyze">提交分析</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="productCloseDlg" title="产品结案" width="480px">
    <el-input v-model="productCloseForm.conclusionSummary" type="textarea" :rows="4" placeholder="结论摘要" />
    <el-input
      v-model="productCloseForm.payloadExtensionJson"
      type="textarea"
      :rows="3"
      style="margin-top: 8px"
      placeholder="可选扩展 JSON"
    />
    <template #footer>
      <el-button @click="productCloseDlg = false">取消</el-button>
      <el-button type="primary" @click="saveProductClose">确认</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="pmAssistDlg" title="PM 协助记录" width="520px">
    <el-form label-width="120px">
      <el-form-item label="问题类型"><el-input v-model="pmAssistForm.problemType" /></el-form-item>
      <el-form-item label="根因摘要" required><el-input v-model="pmAssistForm.rootCauseSummary" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="处理动作"><el-input v-model="pmAssistForm.actionTaken" type="textarea" :rows="2" /></el-form-item>
      <el-form-item label="协助 Agent IDs JSON">
        <el-input v-model="pmAssistForm.assistedAgentIdsJson" placeholder='如 [1,2]' />
      </el-form-item>
      <el-form-item label="已解决">
        <el-switch v-model="pmAssistForm.resolved" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pmAssistDlg = false">取消</el-button>
      <el-button type="primary" @click="savePmAssist">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="improveDlg" title="改进记录" width="480px">
    <el-form label-width="88px">
      <el-form-item label="类型" required><el-input v-model="improveForm.changeType" /></el-form-item>
      <el-form-item label="摘要" required><el-input v-model="improveForm.summary" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="详情 JSON"><el-input v-model="improveForm.detailJson" type="textarea" :rows="2" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="improveDlg = false">取消</el-button>
      <el-button type="primary" @click="saveImprovement">保存</el-button>
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

  <el-dialog v-model="threadDlg" :title="threadRoomTitle" width="640px" @opened="onThreadDlgOpened">
    <div class="toolbar wrap" style="margin-bottom: 8px">
      <el-input v-model="threadMsg.content" type="textarea" :rows="2" placeholder="发送 USER 消息内容" style="flex: 1" />
      <el-button v-permission="'mag:task:operate'" type="primary" size="small" @click="postThreadMsg">发送</el-button>
    </div>
    <el-scrollbar max-height="320">
      <div v-for="m in threadMessages" :key="m.id" class="msg-line">
        <span class="who">{{ m.senderType }}</span>
        <span class="body">{{ m.content }}</span>
      </div>
    </el-scrollbar>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  magGetProject,
  magListMembers,
  magListAgents,
  magCreateAgent,
  magUpdateAgent,
  magListTasks,
  magCreateTask,
  magStartTask,
  magSubmitComplete,
  magGetRequirementDoc,
  magSaveRequirementDoc,
  magListRequirementPool,
  magDecidePoolItem,
  magListReleases,
  magCreateRelease,
  magListThreads,
  magCreateThread,
  magListModules,
  magCreateModule,
  magUpdateModule,
  magDeleteModule,
  magImportBlueprint,
  magTaskBlock,
  magTaskRequestNext,
  magListPmAssist,
  magCreatePmAssist,
  magListImprovements,
  magAppendImprovement,
  magListAlerts,
  magAckAlert,
  magListScheduledJobs,
  magUpsertScheduledJob,
  magListReqRevisions,
  magReqDiff,
  magReqAnalyzeChange,
  magProductClosePoolItem,
  magListFetchAudit,
  magListMessages,
  magPostMessage,
  magRunThread,
  magRunAgent,
  magListOrchestrationRuns,
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
const pool = ref([])
const releases = ref([])
const threads = ref([])
const reqContent = ref('')
const modules = ref([])
const alerts = ref([])
const fetchAudits = ref([])
const pmAssists = ref([])
const improvements = ref([])
const schedJobs = ref([])
const improveAgentId = ref(null)
const revisions = ref([])
const diffV1 = ref(1)
const diffV2 = ref(2)
const diffText = ref('')
const orchestrationRuns = ref([])

const agentRoleOptions = [
  { value: 'PM', label: 'PM' },
  { value: 'PRODUCT', label: '产品（PRODUCT）' },
  { value: 'BACKEND', label: '后端（BACKEND）' },
  { value: 'FRONTEND', label: '前端（FRONTEND）' },
  { value: 'TEST', label: '测试（TEST）' },
  { value: 'VERIFY', label: '核查（VERIFY）' },
]

const POOL_STATE_LABELS = {
  PENDING_USER: '待用户拍板',
  USER_CONFIRMED_OK: '用户确认（原样）',
  USER_CONFIRMED_CHANGE: '用户确认（有变更）',
  USER_REJECTED: '已拒绝',
  CLOSED: '已关闭（延期等）',
  CLOSED_BY_PRODUCT: '产品结案',
}

function poolStateLabel(s) {
  return POOL_STATE_LABELS[s] || s || '—'
}

const agentDlg = ref(false)
const agentEditingId = ref(null)
const llmChannels = ref([])
const agentForm = ref({ roleType: 'BACKEND', name: '', llmChannelId: null })
const taskDlg = ref(false)
const taskForm = ref({ title: '', description: '' })
const relDlg = ref(false)
const relForm = ref({ versionLabel: '', snapshotJson: '{}', qualityFlag: 0 })
const moduleDlg = ref(false)
const moduleEditId = ref(null)
const moduleForm = ref({ name: '', parentId: 0, tag: '', sortOrder: 0 })
const blueprintDlg = ref(false)
const blueprintForm = ref({ sourceType: 'ARCHIVE', sourceId: null })
const blockDlg = ref(false)
const blockTask = ref(null)
const blockForm = ref({ reason: '', blockedByAgentId: null })
const reqNextDlg = ref(false)
const reqNextTask = ref(null)
const reqNextForm = ref({ agentId: null })
const revDlg = ref(false)
const analyzeDlg = ref(false)
const analyzeForm = ref({ changeSummary: '' })
const productCloseDlg = ref(false)
const productCloseRow = ref(null)
const productCloseForm = ref({ conclusionSummary: '', payloadExtensionJson: '' })
const pmAssistDlg = ref(false)
const pmAssistForm = ref({
  problemType: '',
  rootCauseSummary: '',
  actionTaken: '',
  assistedAgentIdsJson: '',
  resolved: 0,
})
const improveDlg = ref(false)
const improveForm = ref({ changeType: '', summary: '', detailJson: '' })
const schedDlg = ref(false)
const schedForm = ref({ id: null, jobKey: '', cronExpr: '0 0 * * * ?', enabled: 1, projectId: null })
const threadDlg = ref(false)
const threadRoom = ref(null)
const threadRoomTitle = computed(() => (threadRoom.value ? `线程 #${threadRoom.value.id}` : '线程'))
const threadMessages = ref([])
const threadMsg = ref({ content: '' })

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

async function loadPool() {
  const res = await magListRequirementPool(projectId.value)
  pool.value = res.data
}

async function loadReleases() {
  const res = await magListReleases(projectId.value)
  releases.value = res.data
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

async function loadFetchAudit() {
  const res = await magListFetchAudit(projectId.value)
  fetchAudits.value = res.data
}

async function loadPmAssist() {
  const res = await magListPmAssist(projectId.value)
  pmAssists.value = res.data
}

async function loadImprovements() {
  if (!improveAgentId.value) return
  const res = await magListImprovements(projectId.value, improveAgentId.value)
  improvements.value = res.data
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
  if (t === 'tasks') loadTasks()
  if (t === 'pool') loadPool()
  if (t === 'releases') loadReleases()
  if (t === 'threads') loadThreads()
  if (t === 'orchRuns') loadOrchestrationRuns()
  if (t === 'req') loadReq()
  if (t === 'modules') loadModules()
  if (t === 'alerts') loadAlerts()
  if (t === 'fetchAudit') loadFetchAudit()
  if (t === 'pmAssist') loadPmAssist()
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
  agentForm.value = { roleType: 'BACKEND', name: '', llmChannelId: null }
  loadLlmChannelsForAgent()
  agentDlg.value = true
}

function openEditAgent(row) {
  agentEditingId.value = row.id
  agentForm.value = {
    roleType: row.roleType || 'BACKEND',
    name: row.name || '',
    llmChannelId: row.llmChannelId ?? null,
  }
  loadLlmChannelsForAgent()
  agentDlg.value = true
}

async function saveAgent() {
  if (!agentForm.value.name?.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  const body = {
    roleType: agentForm.value.roleType,
    name: agentForm.value.name.trim(),
    llmChannelId: agentForm.value.llmChannelId ?? null,
  }
  if (agentEditingId.value) {
    await magUpdateAgent(agentEditingId.value, { ...body, applyLlmChannelId: true })
    ElMessage.success('已保存')
  } else {
    await magCreateAgent(projectId.value, body)
    ElMessage.success('已创建')
  }
  agentDlg.value = false
  loadAgents()
}

function openTask() {
  taskForm.value = { title: '', description: '' }
  taskDlg.value = true
}

async function saveTask() {
  if (!taskForm.value.title.trim()) return
  await magCreateTask(projectId.value, {
    title: taskForm.value.title,
    description: taskForm.value.description,
  })
  ElMessage.success('已创建')
  taskDlg.value = false
  loadTasks()
}

async function startTask(row) {
  await magStartTask(row.id)
  ElMessage.success('已开始')
  loadTasks()
}

async function submitDone(row) {
  await magSubmitComplete(row.id, { rowVersion: row.rowVersion })
  ElMessage.success('已申报')
  loadTasks()
}

async function saveReq() {
  await magSaveRequirementDoc(projectId.value, { content: reqContent.value })
  ElMessage.success('已保存')
}

async function decide(row, decision) {
  await magDecidePoolItem(row.id, { decision })
  ElMessage.success('已处理')
  loadPool()
}

function openRelease() {
  relForm.value = { versionLabel: 'v1.0.0', snapshotJson: '{}', qualityFlag: 0 }
  relDlg.value = true
}

async function saveRelease() {
  await magCreateRelease(projectId.value, relForm.value)
  ElMessage.success('已归档')
  relDlg.value = false
  loadReleases()
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
  blueprintForm.value = { sourceType: 'ARCHIVE', sourceId: null }
  blueprintDlg.value = true
}

async function saveBlueprint() {
  if (!blueprintForm.value.sourceId) {
    ElMessage.warning('请填写来源 ID')
    return
  }
  const res = await magImportBlueprint(projectId.value, blueprintForm.value)
  ElMessage.success(`已导入 ${res.data?.length ?? 0} 条模块`)
  blueprintDlg.value = false
  loadModules()
}

function openBlock(row) {
  blockTask.value = row
  blockForm.value = { reason: '', blockedByAgentId: null }
  blockDlg.value = true
}

async function saveBlock() {
  if (!blockForm.value.reason.trim()) return
  await magTaskBlock(blockTask.value.id, {
    reason: blockForm.value.reason,
    blockedByAgentId: blockForm.value.blockedByAgentId || null,
  })
  ElMessage.success('已标记阻塞')
  blockDlg.value = false
  loadTasks()
}

async function openRequestNext(row) {
  if (!agents.value.length) {
    await loadAgents()
  }
  reqNextTask.value = row
  reqNextForm.value = { agentId: agents.value[0]?.id ?? null }
  reqNextDlg.value = true
}

async function saveRequestNext() {
  if (!reqNextForm.value.agentId) {
    ElMessage.warning('请选择 Agent')
    return
  }
  await magTaskRequestNext(reqNextTask.value.id, { agentId: reqNextForm.value.agentId })
  ElMessage.success('已写入协调线程')
  reqNextDlg.value = false
}

async function openRevisions() {
  const res = await magListReqRevisions(projectId.value)
  revisions.value = res.data
  revDlg.value = true
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

function openProductClose(row) {
  productCloseRow.value = row
  productCloseForm.value = { conclusionSummary: '', payloadExtensionJson: '' }
  productCloseDlg.value = true
}

async function saveProductClose() {
  if (!productCloseForm.value.conclusionSummary.trim()) return
  await magProductClosePoolItem(productCloseRow.value.id, {
    conclusionSummary: productCloseForm.value.conclusionSummary,
    payloadExtensionJson: productCloseForm.value.payloadExtensionJson || undefined,
  })
  ElMessage.success('已结案')
  productCloseDlg.value = false
  loadPool()
}

async function ackAlert(row) {
  await magAckAlert(row.id)
  ElMessage.success('已确认')
  loadAlerts()
}

function openPmAssist() {
  pmAssistForm.value = {
    problemType: '',
    rootCauseSummary: '',
    actionTaken: '',
    assistedAgentIdsJson: '',
    resolved: 0,
  }
  pmAssistDlg.value = true
}

async function savePmAssist() {
  if (!pmAssistForm.value.rootCauseSummary.trim()) return
  await magCreatePmAssist(projectId.value, pmAssistForm.value)
  ElMessage.success('已登记')
  pmAssistDlg.value = false
  loadPmAssist()
}

function openImprovement() {
  improveForm.value = { changeType: '', summary: '', detailJson: '' }
  improveDlg.value = true
}

async function saveImprovement() {
  if (!improveForm.value.changeType.trim() || !improveForm.value.summary.trim()) return
  await magAppendImprovement(projectId.value, improveAgentId.value, improveForm.value)
  ElMessage.success('已追加')
  improveDlg.value = false
  loadImprovements()
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

function openThreadRoom(row) {
  threadRoom.value = row
  threadMsg.value = { content: '' }
  threadDlg.value = true
}

async function onThreadDlgOpened() {
  if (!threadRoom.value) return
  const res = await magListMessages(threadRoom.value.id)
  threadMessages.value = res.data
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
  threadMessages.value = res.data
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

async function runAgentRow(row) {
  const res = await magRunAgent(row.id)
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
.msg-line {
  padding: 6px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 13px;
}
.msg-line .who {
  display: inline-block;
  min-width: 64px;
  color: var(--el-text-color-secondary);
}
.msg-line .body {
  white-space: pre-wrap;
  word-break: break-all;
}
.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
</style>
