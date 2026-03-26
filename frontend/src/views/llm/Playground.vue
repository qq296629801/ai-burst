<template>
  <el-card shadow="never">
    <el-form label-width="100px" class="form">
      <el-form-item label="通道" required>
        <el-select v-model="channelId" placeholder="选择已配置的通道" filterable style="width: 100%">
          <el-option v-for="c in channels" :key="c.id" :label="`${c.channelName} (${c.providerName})`" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="覆盖模型">
        <el-input v-model="model" placeholder="可选，不填则用通道默认模型" />
      </el-form-item>
      <el-form-item label="用户消息" required>
        <el-input v-model="userMessage" type="textarea" :rows="6" placeholder="输入要发送的内容" />
      </el-form-item>
      <el-form-item>
        <el-button v-permission="'llm:chat:invoke'" type="primary" :loading="loading" @click="send">发送</el-button>
        <el-button @click="clearReply">清空回复</el-button>
      </el-form-item>
      <el-form-item label="回复">
        <el-input v-model="reply" type="textarea" :rows="10" readonly placeholder="调用成功后展示模型输出" />
      </el-form-item>
      <el-form-item v-if="usageText" label="用量">
        <pre class="usage">{{ usageText }}</pre>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchLlmChannels, llmChat } from '@/api/llm'

const channels = ref([])
const channelId = ref(null)
const model = ref('')
const userMessage = ref('你好，简单介绍一下你自己。')
const reply = ref('')
const usageText = ref('')
const loading = ref(false)

async function loadChannels() {
  const res = await fetchLlmChannels()
  channels.value = res.data || []
}

function clearReply() {
  reply.value = ''
  usageText.value = ''
}

async function send() {
  if (!channelId.value) {
    ElMessage.warning('请选择通道')
    return
  }
  if (!userMessage.value?.trim()) {
    ElMessage.warning('请输入消息')
    return
  }
  loading.value = true
  clearReply()
  try {
    const body = {
      channelId: channelId.value,
      messages: [{ role: 'user', content: userMessage.value.trim() }],
      temperature: 0.7,
    }
    if (model.value?.trim()) {
      body.model = model.value.trim()
    }
    const res = await llmChat(body)
    const data = res.data
    reply.value = data?.content || ''
    usageText.value = data?.usage ? JSON.stringify(data.usage, null, 2) : ''
    if (!reply.value) {
      ElMessage.warning('返回内容为空，请检查模型或上游响应')
    }
  } catch (_) {
    /* http 已提示 */
  } finally {
    loading.value = false
  }
}

onMounted(loadChannels)
</script>

<style scoped>
.form {
  max-width: 920px;
}
.usage {
  margin: 0;
  font-size: 12px;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  white-space: pre-wrap;
}
</style>
