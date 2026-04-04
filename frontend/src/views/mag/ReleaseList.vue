<template>
  <el-card shadow="never">
    <el-form inline>
      <el-form-item label="项目">
        <el-select v-model="projectId" placeholder="选择项目" filterable style="width: 260px" @change="load">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button v-permission="'mag:release:archive'" type="primary" @click="openCreate">新建归档</el-button>
        <el-button @click="load">刷新</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="rows" border stripe style="margin-top: 16px">
      <el-table-column prop="versionLabel" label="版本" />
      <el-table-column prop="qualityFlag" label="优质" width="80" />
      <el-table-column prop="createdAt" label="时间" width="200" />
    </el-table>
  </el-card>

  <el-dialog v-model="visible" title="归档" width="480px" destroy-on-close>
    <el-form :model="form" label-width="100px">
      <el-form-item label="版本标签" required><el-input v-model="form.versionLabel" /></el-form-item>
      <el-form-item label="快照 JSON"><el-input v-model="form.snapshotJson" type="textarea" :rows="4" /></el-form-item>
      <el-form-item label="优质候选">
        <el-switch v-model="form.qualityFlag" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { magProjectsPage, magListReleases, magCreateRelease } from '@/api/mag'

const projects = ref([])
const projectId = ref(null)
const rows = ref([])
const visible = ref(false)
const form = reactive({ versionLabel: 'v1.0.0', snapshotJson: '{}', qualityFlag: 0 })

async function loadProjects() {
  const res = await magProjectsPage({ pageNum: 1, pageSize: 100 })
  projects.value = res.data.list
  if (!projectId.value && projects.value.length) {
    projectId.value = projects.value[0].id
    load()
  }
}

async function load() {
  if (!projectId.value) return
  const res = await magListReleases(projectId.value)
  rows.value = res.data
}

function openCreate() {
  if (!projectId.value) {
    ElMessage.warning('请先选择项目')
    return
  }
  Object.assign(form, { versionLabel: 'v1.0.0', snapshotJson: '{}', qualityFlag: 0 })
  visible.value = true
}

async function save() {
  await magCreateRelease(projectId.value, { ...form })
  ElMessage.success('已归档')
  visible.value = false
  load()
}

onMounted(loadProjects)
</script>
