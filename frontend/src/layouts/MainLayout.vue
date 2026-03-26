<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">AI Burst</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#1a1a2e"
        text-color="#cfd3dc"
        active-text-color="#409eff"
      >
        <sidebar-menu :items="user.menus" />
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="spacer" />
        <span class="who">{{ user.user?.nickname || user.user?.username }}</span>
        <el-button link type="primary" @click="onLogout">退出</el-button>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import SidebarMenu from '@/components/SidebarMenu.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

async function onLogout() {
  await user.logout()
  ElMessage.success('已退出')
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #1a1a2e;
}
.brand {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.05em;
}
.header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
}
.spacer {
  flex: 1;
}
.who {
  margin-right: 12px;
  color: #606266;
  font-size: 14px;
}
.main {
  background: #f5f7fa;
}
</style>
