<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-permission="'llm:channel:add'" type="primary" @click="openCreate">新增通道</el-button>
    </div>
    <el-table :data="rows" border stripe style="width: 100%; margin-top: 16px">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="channelName" label="名称" />
      <el-table-column prop="providerName" label="厂商" />
      <el-table-column prop="protocol" label="协议" width="120" />
      <el-table-column prop="baseUrl" label="Base URL" min-width="220" show-overflow-tooltip />
      <el-table-column prop="defaultModel" label="默认模型" />
      <el-table-column prop="status" label="状态" width="88">
        <template #default="{ row }">{{ row.status === 1 ? '启用' : '禁用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'llm:channel:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'llm:channel:delete'" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="visible" :title="form.id ? '编辑通道' : '新增通道'" width="560px" destroy-on-close>
    <el-form :model="form" label-width="108px">
      <el-form-item label="厂商" required>
        <el-select v-model="form.providerCode" filterable placeholder="选择厂商" style="width: 100%" @change="onProviderChange">
          <el-option v-for="p in providers" :key="p.code" :label="p.name" :value="p.code">
            <span>{{ p.name }}</span>
            <span class="opt-sub">{{ p.code }}</span>
          </el-option>
        </el-select>
        <div v-if="providerHint" class="hint">{{ providerHint }}</div>
      </el-form-item>
      <el-form-item label="通道名称" required>
        <el-input v-model="form.channelName" />
      </el-form-item>
      <el-form-item label="Base URL" required>
        <el-input v-model="form.baseUrl" placeholder="可从厂商默认带出，可按实际修改" />
      </el-form-item>
      <el-form-item :label="form.id ? 'API Key' : 'API Key'" :required="!form.id">
        <el-input v-model="form.apiKey" type="password" show-password :placeholder="form.id ? '留空则不修改' : '必填'" />
      </el-form-item>
      <el-form-item label="默认模型">
        <el-input v-model="form.defaultModel" placeholder="如 deepseek-chat、gpt-4o-mini" />
      </el-form-item>
      <el-form-item label="扩展 JSON">
        <el-input v-model="form.extraJson" type="textarea" :rows="2" placeholder="可选" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchLlmProviders,
  fetchLlmChannels,
  createLlmChannel,
  updateLlmChannel,
  deleteLlmChannel,
} from '@/api/llm'

const rows = ref([])
const providers = ref([])
const visible = ref(false)
const form = reactive({
  id: null,
  providerCode: '',
  channelName: '',
  baseUrl: '',
  apiKey: '',
  defaultModel: '',
  extraJson: '',
  status: 1,
})

const providerHint = computed(() => {
  const p = providers.value.find((x) => x.code === form.providerCode)
  return p?.docHint || ''
})

async function loadProviders() {
  const res = await fetchLlmProviders()
  providers.value = res.data || []
}

async function load() {
  const res = await fetchLlmChannels()
  rows.value = res.data || []
}

function onProviderChange(code) {
  const p = providers.value.find((x) => x.code === code)
  if (p && p.defaultBaseUrl) {
    form.baseUrl = p.defaultBaseUrl
  }
}

function openCreate() {
  Object.assign(form, {
    id: null,
    providerCode: '',
    channelName: '',
    baseUrl: '',
    apiKey: '',
    defaultModel: '',
    extraJson: '',
    status: 1,
  })
  visible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    providerCode: row.providerCode,
    channelName: row.channelName,
    baseUrl: row.baseUrl,
    apiKey: '',
    defaultModel: row.defaultModel || '',
    extraJson: row.extraJson || '',
    status: row.status,
  })
  visible.value = true
}

async function save() {
  if (!form.providerCode || !form.channelName || !form.baseUrl) {
    ElMessage.warning('请填写厂商、名称与 Base URL')
    return
  }
  if (!form.id && !form.apiKey) {
    ElMessage.warning('请填写 API Key')
    return
  }
  const payload = {
    id: form.id,
    providerCode: form.providerCode,
    channelName: form.channelName,
    baseUrl: form.baseUrl,
    apiKey: form.apiKey || undefined,
    defaultModel: form.defaultModel || undefined,
    extraJson: form.extraJson || undefined,
    status: form.status,
  }
  if (form.id) {
    await updateLlmChannel(payload)
  } else {
    await createLlmChannel(payload)
  }
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除该通道？', '提示', { type: 'warning' })
  await deleteLlmChannel(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => {
  loadProviders()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
}
.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
.opt-sub {
  float: right;
  color: #909399;
  font-size: 12px;
}
</style>
