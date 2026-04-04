<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-permission="'mag:project:manage'" type="primary" @click="openCreate">新建项目</el-button>
    </div>
    <el-table :data="rows" border stripe style="width: 100%; margin-top: 16px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="agentCount" label="Agent 数" width="96" />
      <el-table-column prop="currentRequirementVersion" label="需求版本" width="100" />
      <el-table-column prop="lastActivityAt" label="最近活动" width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">{{ row.status === 1 ? '进行中' : '已归档' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="goWorkspace(row)">工作台</el-button>
          <el-button
            v-permission="'mag:project:manage'"
            link
            type="danger"
            @click="archive(row)"
          >
            归档
          </el-button>
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

  <el-dialog v-model="visible" title="新建项目" width="440px" destroy-on-close>
    <el-form :model="form" label-width="72px">
      <el-form-item label="名称" required>
        <el-input v-model="form.name" maxlength="128" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { magProjectsPage, magCreateProject, magArchiveProject } from '@/api/mag'

const router = useRouter()
const query = reactive({ pageNum: 1, pageSize: 10 })
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const form = reactive({ name: '' })

async function load() {
  const res = await magProjectsPage(query)
  total.value = res.data.total
  rows.value = res.data.list
}

function openCreate() {
  form.name = ''
  visible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  await magCreateProject({ name: form.name.trim() })
  ElMessage.success('已创建')
  visible.value = false
  load()
}

function goWorkspace(row) {
  router.push({ name: 'MagProjectWorkspace', params: { projectId: String(row.id) } })
}

async function archive(row) {
  await ElMessageBox.confirm(`归档项目「${row.name}」？`, '确认', { type: 'warning' })
  await magArchiveProject(row.id)
  ElMessage.success('已归档')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
