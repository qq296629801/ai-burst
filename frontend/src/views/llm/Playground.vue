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
        <el-form-item>
          <el-button @click="clearSession" :disabled="!messages.length">清空会话</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div ref="chatBodyRef" class="chat-body">
      <div v-if="!messages.length && !loading" class="empty-hint">
        选择通道后输入消息并发送，支持多轮对话。
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
        <div class="msg-meta">{{ roleLabel(m.role) }} · {{ formatTime(m.at) }}</div>
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
      <el-input
        v-model="draft"
        type="textarea"
        :rows="3"
        placeholder="输入消息，Ctrl+Enter 发送"
        resize="none"
        @keydown.ctrl.enter.prevent="send"
      />
      <div class="composer-actions">
        <el-button v-permission="'llm:chat:invoke'" type="primary" :loading="loading" @click="send">
          发送
        </el-button>
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
import { nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchLlmChannels, llmChat } from '@/api/llm'

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

function roleLabel(role) {
  return role === 'user' ? '我' : '助手'
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

async function send() {
  if (loading.value) {
    return
  }
  if (!channelId.value) {
    ElMessage.warning('请选择通道')
    return
  }
  const text = draft.value?.trim()
  if (!text) {
    ElMessage.warning('请输入消息')
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

onMounted(loadChannels)
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
