<template>
  <el-card shadow="never" class="playground-card">
    <div class="toolbar">
      <el-form :inline="true" class="toolbar-form">
        <el-form-item label="通道" required>
          <el-select
            v-model="channelId"
            placeholder="选择已配置的通道"
            filterable
            style="width: 260px"
          >
            <el-option
              v-for="c in channels"
              :key="c.id"
              :label="`${c.channelName} (${c.providerName})`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="覆盖模型">
          <el-input v-model="model" placeholder="可选" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item v-if="user.hasPerm('mag:project:list')" label="MAG 项目">
          <el-select
            v-model="magProjectId"
            placeholder="选择项目"
            filterable
            clearable
            style="width: 280px"
            @visible-change="(v) => v && loadMagProjects()"
          >
            <el-option
              v-for="p in magProjects"
              :key="p.id"
              :label="`${p.name} (#${p.id})`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="user.hasPerm('mag:task:operate') && magProjectId"
          label="项目经理 Agent"
        >
          <el-select
            v-model="magPmAgentId"
            placeholder="选择要触发的 PM Agent"
            filterable
            clearable
            style="width: 300px"
            :disabled="!pmAgentOptions.length"
          >
            <el-option
              v-for="a in pmAgentOptions"
              :key="a.id"
              :label="`${a.name || 'PM'} · #${a.id}`"
              :value="a.id"
            />
          </el-select>
          <span v-if="magProjectId && !pmAgentOptions.length" class="form-item-hint-inline">
            该项目下无已启用且绑定通道的 PM
          </span>
        </el-form-item>
        <el-form-item>
          <el-button @click="clearSession" :disabled="!messages.length">清空会话</el-button>
        </el-form-item>
      </el-form>
      <p v-if="user.hasPerm('mag:task:operate')" class="toolbar-hint">
        在输入框输入 <code>@</code> 可从列表选择 <strong>项目 + 项目经理 Agent</strong>（回车或点击选中后插入，再继续输入说明）；也可在上方工具栏手动选择后，使用
        <code>@项目经理</code> / <code>@PM</code> 触发编排。仅有一个可用 PM 时工具栏会自动选中。提及符号会在发送时从编排说明中去掉。
      </p>
    </div>

    <div ref="chatBodyRef" class="chat-body">
      <div v-if="!messages.length && !loading" class="empty-hint">
        选择通道后可与模型多轮对话。
        <template v-if="user.hasPerm('mag:task:operate')">
          <br />
          输入 <code>@</code> 可选择项目与项目经理 Agent，或先选工具栏项目与 PM 后输入「@项目经理 …」触发编排（不经过上方直连通道）。
        </template>
      </div>
      <div
        v-for="m in messages"
        :key="m.id"
        class="msg-row"
        :class="m.role === 'user' ? 'msg-row--user' : 'msg-row--assistant'"
      >
        <div class="bubble" :class="m.role === 'user' ? 'bubble--user' : 'bubble--assistant'">
          <pre class="bubble-text">{{ m.content }}</pre>
        </div>
        <div class="msg-meta">{{ roleLabel(m) }} · {{ formatTime(m.at) }}</div>
      </div>
      <div v-if="loading" class="msg-row msg-row--assistant">
        <div class="bubble bubble--assistant bubble--pending">
          <span class="dot" /><span class="dot" /><span class="dot" />
          <span class="pending-text">生成中…</span>
        </div>
      </div>
      <div ref="anchorRef" class="anchor" />
    </div>

    <div class="composer">
      <el-mention
        v-if="useMagMentionComposer"
        v-model="draft"
        type="textarea"
        :rows="3"
        :placeholder="composerPlaceholder"
        resize="none"
        prefix="@"
        :options="magMentionFlatOptions"
        :loading="magMentionLoading"
        @search="onMagMentionSearch"
        @select="onMagMentionSelect"
        @keydown.ctrl.enter.prevent="send"
      />
      <el-input
        v-else
        v-model="draft"
        type="textarea"
        :rows="3"
        :placeholder="composerPlaceholder"
        resize="none"
        @keydown.ctrl.enter.prevent="send"
      />
      <div class="composer-actions">
        <el-button type="primary" :loading="loading" :disabled="!canSend" @click="send">发送</el-button>
      </div>
    </div>

    <el-collapse v-if="usageText" class="usage-collapse">
      <el-collapse-item title="本次用量（JSON）" name="usage">
        <pre class="usage-pre">{{ usageText }}</pre>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchLlmChannels, llmChat } from '@/api/llm'
import { magListAgents, magProjectsPage, magRunAgent } from '@/api/mag'
import { useUserStore } from '@/stores/user'

const user = useUserStore()

const magProjects = ref([])
const magProjectId = ref(null)
const magAgents = ref([])
const magPmAgentId = ref(null)

/** @type {Map<number, object[]>} */
const magAgentsByProjectCache = new Map()
const magMentionFlatOptions = ref([])
const magMentionOptionsReady = ref(false)
const magMentionLoading = ref(false)

const useMagMentionComposer = computed(
  () => user.hasPerm('mag:task:operate') && user.hasPerm('mag:project:list'),
)

const pmAgentOptions = computed(() => {
  const list = magAgents.value || []
  return list
    .filter(
      (a) =>
        a.roleType === 'PM' &&
        (a.status == null || a.status !== 0) &&
        a.llmChannelId != null,
    )
    .slice()
    .sort((a, b) => Number(a.id) - Number(b.id))
})

const composerPlaceholder = computed(() => {
  const parts = ['输入消息，Ctrl+Enter 发送']
  if (useMagMentionComposer.value) {
    parts.push('输入 @ 选择项目与项目经理 Agent，再写编排说明')
  } else if (user.hasPerm('mag:task:operate')) {
    parts.push('支持 @项目经理 / @PM（须选工具栏项目 + 项目经理 Agent）')
  }
  return parts.join('；')
})

const canSend = computed(() => {
  return user.hasPerm('llm:chat:invoke') || user.hasPerm('mag:task:operate')
})

let idSeq = 0
function nextId() {
  idSeq += 1
  return `m-${idSeq}`
}

const channels = ref([])
const channelId = ref(null)
const model = ref('')
const draft = ref('你好，简单介绍一下你自己。')
const messages = ref([])
const usageText = ref('')
const loading = ref(false)
const chatBodyRef = ref(null)
const anchorRef = ref(null)

async function loadChannels() {
  const res = await fetchLlmChannels()
  channels.value = res.data || []
}

async function loadMagProjects() {
  if (!user.hasPerm('mag:project:list')) return
  magMentionOptionsReady.value = false
  magMentionFlatOptions.value = []
  magAgentsByProjectCache.clear()
  try {
    const res = await magProjectsPage({ pageNum: 1, pageSize: 100 })
    magProjects.value = res.data?.list || []
  } catch {
    magProjects.value = []
  }
}

function isEligiblePmAgent(a) {
  return (
    a &&
    a.roleType === 'PM' &&
    (a.status == null || a.status !== 0) &&
    a.llmChannelId != null
  )
}

async function ensureMagMentionOptions() {
  if (magMentionOptionsReady.value || !user.hasPerm('mag:project:list')) {
    return
  }
  magMentionLoading.value = true
  try {
    if (!magProjects.value.length) {
      await loadMagProjects()
    }
    const opts = []
    for (const p of magProjects.value) {
      let agents = magAgentsByProjectCache.get(p.id)
      if (!agents) {
        try {
          const res = await magListAgents(p.id)
          agents = res.data || []
        } catch {
          agents = []
        }
        magAgentsByProjectCache.set(p.id, agents)
      }
      for (const a of agents) {
        if (!isEligiblePmAgent(a)) continue
        opts.push({
          label: `${p.name} / ${a.name || '项目经理'} (#${a.id})`,
          value: `[mag:${p.id}:${a.id}]`,
          projectId: p.id,
          agentId: a.id,
        })
      }
    }
    opts.sort((x, y) => String(x.label).localeCompare(String(y.label), 'zh-CN'))
    magMentionFlatOptions.value = opts
  } finally {
    magMentionLoading.value = false
    magMentionOptionsReady.value = true
  }
}

/** @param {string} _pattern @param {string} prefix */
function onMagMentionSearch(_pattern, prefix) {
  if (prefix !== '@') return
  void ensureMagMentionOptions()
}

/** @param {object} option */
function onMagMentionSelect(option) {
  if (option?.projectId != null && option?.agentId != null) {
    magProjectId.value = option.projectId
    magPmAgentId.value = option.agentId
    void loadMagAgents(option.projectId)
  }
}

async function loadMagAgents(projectId) {
  if (!projectId || !user.hasPerm('mag:project:list')) {
    magAgents.value = []
    return
  }
  try {
    const res = await magListAgents(projectId)
    magAgents.value = res.data || []
  } catch {
    magAgents.value = []
  }
}

watch(magProjectId, (id) => {
  magPmAgentId.value = null
  loadMagAgents(id)
})

/** 项目下仅一个可用 PM 时自动选中；换项目或列表变化时校验当前选择仍有效 */
watch(
  pmAgentOptions,
  (opts) => {
    if (!opts.length) {
      magPmAgentId.value = null
      return
    }
    if (opts.length === 1) {
      magPmAgentId.value = opts[0].id
      return
    }
    const cur = magPmAgentId.value
    if (cur != null && opts.some((a) => a.id === cur)) {
      return
    }
    magPmAgentId.value = null
  },
  { deep: true },
)

const MAG_MENTION_TOKEN_RE = /@\[mag:(\d+):(\d+)\]\s*/g
const LEGACY_PM_TRIGGER_RE = /@\s*(项目经理\s*agent|项目经理\s*[Aa]gent|项目经理|PM)\b/gi

const defaultPmInstruction =
  '请根据当前项目任务与模块情况执行项目经理职责（可先 list_project_tasks、按需派工）。'

/**
 * @returns {null | { mode: 'mag_token', projectId: number, agentId: number, instruction: string }
 *   | { mode: 'legacy', instruction: string }
 *   | { error: string }}
 */
function extractPmRunPayload(rawText) {
  const tokens = []
  let m
  const reTok = new RegExp(MAG_MENTION_TOKEN_RE.source, 'g')
  while ((m = reTok.exec(rawText)) !== null) {
    tokens.push({ projectId: Number(m[1]), agentId: Number(m[2]) })
  }
  if (tokens.length) {
    const first = tokens[0]
    if (tokens.some((t) => t.projectId !== first.projectId || t.agentId !== first.agentId)) {
      return { error: '同一条消息中多条 @ 指向不同项目或 Agent，请只保留一条。' }
    }
    const instruction = rawText
      .replace(new RegExp(MAG_MENTION_TOKEN_RE.source, 'g'), ' ')
      .replace(/\s+/g, ' ')
      .trim()
    return {
      mode: 'mag_token',
      projectId: first.projectId,
      agentId: first.agentId,
      instruction: instruction.length > 0 ? instruction : defaultPmInstruction,
    }
  }
  if (rawText.search(LEGACY_PM_TRIGGER_RE) < 0) {
    return null
  }
  const instruction = rawText.replace(LEGACY_PM_TRIGGER_RE, ' ').replace(/\s+/g, ' ').trim()
  return {
    mode: 'legacy',
    instruction: instruction.length > 0 ? instruction : defaultPmInstruction,
  }
}

function roleLabel(m) {
  if (m.role === 'user') return '我'
  if (m.kind === 'mag_pm_run') return '项目经理 Agent（编排）'
  return '助手'
}

function formatTime(ts) {
  const d = new Date(ts)
  return d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function clearSession() {
  messages.value = []
  usageText.value = ''
}

function scrollToBottom() {
  nextTick(() => {
    anchorRef.value?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  })
}

watch(
  () => messages.value.length,
  () => scrollToBottom(),
)
watch(loading, (v) => {
  if (v) scrollToBottom()
})

function buildApiMessages() {
  return messages.value.map((m) => ({ role: m.role, content: m.content }))
}

function formatMagRunReply(data) {
  if (!data || typeof data !== 'object') {
    return '（无返回）'
  }
  const lines = []
  if (data.accepted != null) lines.push(`accepted: ${data.accepted}`)
  if (data.workflowId != null) lines.push(`workflowId: ${data.workflowId}`)
  if (data.message != null && String(data.message).trim()) lines.push(`message: ${data.message}`)
  return lines.length ? lines.join('\n') : JSON.stringify(data, null, 2)
}

async function send() {
  if (loading.value) {
    return
  }
  if (!canSend.value) {
    ElMessage.warning('无对话或 MAG 编排权限')
    return
  }
  const text = draft.value?.trim()
  if (!text) {
    ElMessage.warning('请输入消息')
    return
  }

  const pmPayload = extractPmRunPayload(text)
  if (pmPayload) {
    if (pmPayload.error) {
      ElMessage.warning(pmPayload.error)
      return
    }
    if (!user.hasPerm('mag:task:operate')) {
      ElMessage.warning('触发项目经理编排需要权限 mag:task:operate')
      return
    }
    let runProjectId
    let runAgentId
    let pmLabel
    if (pmPayload.mode === 'mag_token') {
      runProjectId = pmPayload.projectId
      runAgentId = pmPayload.agentId
      const hit = magMentionFlatOptions.value.find(
        (o) => o.projectId === runProjectId && o.agentId === runAgentId,
      )
      pmLabel = hit ? hit.label : `Agent #${runAgentId}`
    } else {
      if (!magProjectId.value) {
        ElMessage.warning('请先在工具栏选择 MAG 项目，再使用 @项目经理 触发编排')
        return
      }
      if (magPmAgentId.value == null) {
        ElMessage.warning('请选择工具栏中的「项目经理 Agent」再发送')
        return
      }
      runProjectId = magProjectId.value
      runAgentId = magPmAgentId.value
      const pm = magAgents.value.find((a) => a.id === runAgentId)
      if (!pm || pm.roleType !== 'PM') {
        ElMessage.warning('所选项目经理 Agent 无效，请重新选择')
        return
      }
      pmLabel = pm.name || String(pm.id)
    }
    const userMsg = { id: nextId(), role: 'user', content: text, at: Date.now() }
    messages.value = [...messages.value, userMsg]
    draft.value = ''
    usageText.value = ''
    loading.value = true
    try {
      const res = await magRunAgent(runAgentId, { instruction: pmPayload.instruction })
      const data = res.data
      const content = formatMagRunReply(data)
      messages.value = [
        ...messages.value,
        {
          id: nextId(),
          role: 'assistant',
          kind: 'mag_pm_run',
          content:
            `已提交项目经理 Agent「${pmLabel}」编排（project #${runProjectId}）\n\n` +
            content,
          at: Date.now(),
        },
      ]
      if (data && data.accepted === false) {
        ElMessage.warning(data.message || '编排未接受（如 Temporal 未启用）')
      } else {
        ElMessage.success('已触发项目经理 Agent 编排')
      }
    } catch (_) {
      messages.value = [
        ...messages.value,
        {
          id: nextId(),
          role: 'assistant',
          kind: 'mag_pm_run',
          content: '项目经理编排请求失败，请查看顶部提示或网络面板。',
          at: Date.now(),
        },
      ]
    } finally {
      loading.value = false
      scrollToBottom()
    }
    return
  }

  if (!user.hasPerm('llm:chat:invoke')) {
    ElMessage.warning('直连对话需要权限 llm:chat:invoke；或使用 @项目经理 触发编排')
    return
  }
  if (!channelId.value) {
    ElMessage.warning('请选择通道')
    return
  }
  const userMsg = { id: nextId(), role: 'user', content: text, at: Date.now() }
  messages.value = [...messages.value, userMsg]
  draft.value = ''
  usageText.value = ''
  loading.value = true
  try {
    const body = {
      channelId: channelId.value,
      messages: buildApiMessages(),
      temperature: 0.7,
    }
    if (model.value?.trim()) {
      body.model = model.value.trim()
    }
    const res = await llmChat(body)
    const data = res.data
    const content = data?.content ?? ''
    usageText.value = data?.usage ? JSON.stringify(data.usage, null, 2) : ''
    messages.value = [
      ...messages.value,
      { id: nextId(), role: 'assistant', content: content || '（空回复）', at: Date.now() },
    ]
    if (!content) {
      ElMessage.warning('返回内容为空，请检查模型或上游响应')
    }
  } catch (_) {
    messages.value = [
      ...messages.value,
      {
        id: nextId(),
        role: 'assistant',
        content: '请求失败，请查看顶部提示或网络面板。',
        at: Date.now(),
      },
    ]
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  loadChannels()
  loadMagProjects()
})
</script>

<style scoped>
.playground-card {
  max-width: 920px;
}

.toolbar {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 8px;
  margin-bottom: 12px;
}

.toolbar-form {
  margin-bottom: 0;
}

.toolbar-form :deep(.el-form-item) {
  margin-bottom: 8px;
}

.toolbar-hint {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.toolbar-hint code {
  font-size: 12px;
  padding: 0 4px;
  background: var(--el-fill-color);
  border-radius: 4px;
}

.form-item-hint-inline {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.chat-body {
  min-height: 320px;
  max-height: min(560px, calc(100vh - 320px));
  overflow-y: auto;
  padding: 8px 4px 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  margin-bottom: 12px;
}

.empty-hint {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  text-align: center;
  padding: 48px 16px;
}

.msg-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 14px;
  max-width: 100%;
}

.msg-row--user {
  align-items: flex-end;
}

.msg-row--assistant {
  align-items: flex-start;
}

.bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.55;
  word-break: break-word;
}

.bubble-text {
  margin: 0;
  font-family: inherit;
  font-size: 14px;
  white-space: pre-wrap;
}

.bubble--user {
  background: var(--el-color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble--assistant {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-bottom-left-radius: 4px;
}

.bubble--pending {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary-light-5);
  animation: bounce 1.2s infinite ease-in-out;
}

.dot:nth-child(2) {
  animation-delay: 0.15s;
}

.dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.pending-text {
  font-size: 13px;
  margin-left: 4px;
}

.msg-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  padding: 0 4px;
}

.anchor {
  height: 1px;
  width: 100%;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
}

.usage-collapse {
  margin-top: 12px;
}

.usage-pre {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  background: var(--el-fill-color-light);
  padding: 8px;
  border-radius: 4px;
}
</style>
