<template>
  <el-card shadow="never">
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="池项 ID" width="100" />
      <el-table-column prop="projectId" label="项目 ID" width="100" />
      <el-table-column prop="state" label="状态" width="160" />
      <el-table-column prop="payloadJson" label="载荷" show-overflow-tooltip />
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
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { magTodosPage } from '@/api/mag'

const query = reactive({ pageNum: 1, pageSize: 10 })
const rows = ref([])
const total = ref(0)

async function load() {
  const res = await magTodosPage(query)
  total.value = res.data.total
  rows.value = res.data.list
}

onMounted(load)
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
