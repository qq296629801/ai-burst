<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="关键词" clearable style="width: 200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新建</el-button>
    </div>
    <el-table :data="rows" border stripe style="margin-top: 16px">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="source" label="来源" width="140" />
      <el-table-column prop="keywords" label="关键词" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="removeRow(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="total"
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      @current-change="load"
    />
  </el-card>

  <el-dialog v-model="visible" :title="editingId ? '编辑知识库' : '知识库条目'" width="520px" destroy-on-close>
    <el-form :model="form" label-width="72px">
      <el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="正文" required><el-input v-model="form.body" type="textarea" :rows="8" /></el-form-item>
      <el-form-item label="关键词"><el-input v-model="form.keywords" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { magKbPage, magKbCreate, magKbGet, magKbUpdate, magKbDelete } from '@/api/mag'

const keyword = ref('')
const query = reactive({ pageNum: 1, pageSize: 10 })
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const editingId = ref(null)
const form = reactive({ title: '', body: '', keywords: '' })

async function load() {
  const res = await magKbPage({ ...query, keyword: keyword.value || undefined })
  total.value = res.data.total
  rows.value = res.data.list
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { title: '', body: '', keywords: '' })
  visible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  const res = await magKbGet(row.id)
  Object.assign(form, {
    title: res.data.title || '',
    body: res.data.body || '',
    keywords: res.data.keywords || '',
  })
  visible.value = true
}

async function removeRow(row) {
  await ElMessageBox.confirm(`删除知识库条目「${row.title}」？`, '确认', { type: 'warning' })
  await magKbDelete(row.id)
  ElMessage.success('已删除')
  load()
}

async function save() {
  if (!form.title.trim() || !form.body.trim()) {
    ElMessage.warning('请填写标题与正文')
    return
  }
  if (editingId.value) {
    await magKbUpdate(editingId.value, { ...form })
  } else {
    await magKbCreate({ ...form })
  }
  ElMessage.success('已保存')
  visible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
