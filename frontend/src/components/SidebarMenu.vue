<template>
  <template v-for="item in items" :key="item.id">
    <el-sub-menu v-if="item.permType === 1 && item.children?.length" :index="'d-' + item.id">
      <template #title>
        <span>{{ item.permName }}</span>
      </template>
      <sidebar-menu :items="item.children" />
    </el-sub-menu>
    <el-menu-item v-else-if="item.permType === 2 && item.path" :index="item.path">
      <span>{{ item.permName }}</span>
    </el-menu-item>
  </template>
</template>

<script setup>
defineOptions({ name: 'SidebarMenu' })

defineProps({
  items: {
    type: Array,
    default: () => [],
  },
})
</script>
