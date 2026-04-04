<template>
  <el-card shadow="never">
    <el-form inline>
      <el-form-item label="项目">
        <el-select v-model="projectId" placeholder="选择项目" filterable style="width: 260px" @change="onProjectChange">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">刷新</el-button>
      </el-form-item>
      <el-form-item label="WS">
        <el-tag :type="ws.connected ? 'success' : 'info'" size="small">
          {{ ws.connected ? '已连接' : '未连接' }}
        </el-tag>
      </el-form-item>
    </el-form>
    <el-descriptions v-if="snapshot" title="任务按状态统计" :column="2" border style="margin-top: 16px">
      <el-descriptions-item
        v-for="(cnt, st) in snapshot.taskCountByState"
        :key="st"
        :label="st"
      >
        {{ cnt }}
      </el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { magProjectsPage, magDashboardSnapshot } from '@/api/mag'
import { useMagWebSocket } from '@/composables/useMagWebSocket'

const projects = ref([])
const projectId = ref(null)
const snapshot = ref(null)
const ws = useMagWebSocket()

async function loadProjects() {
  const res = await magProjectsPage({ pageNum: 1, pageSize: 100 })
  projects.value = res.data.list
  if (!projectId.value && projects.value.length) {
    projectId.value = projects.value[0].id
    load()
    connectWsForProject()
  }
}

async function load() {
  if (!projectId.value) return
  const res = await magDashboardSnapshot(projectId.value)
  snapshot.value = res.data
}

function connectWsForProject() {
  ws.disconnect()
  ws.connect({
    onOpen: () => {
      if (projectId.value) {
        ws.subscribeProject(projectId.value)
      }
    },
    onMessage: (data) => {
      if (
        data?.event === 'task.state.changed' &&
        Number(data?.projectId) === Number(projectId.value)
      ) {
        load()
      }
    },
  })
}

function onProjectChange() {
  load()
  connectWsForProject()
}

onMounted(loadProjects)
onUnmounted(() => ws.disconnect())
</script>
