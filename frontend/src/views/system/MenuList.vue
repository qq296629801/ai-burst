<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-permission="'system:menu:add'" type="primary" @click="openCreate(null)">新增根节点</el-button>
    </div>
    <el-table
      :data="tree"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
      style="width: 100%; margin-top: 16px"
    >
      <el-table-column prop="permName" label="名称" min-width="160" />
      <el-table-column prop="permCode" label="权限标识" min-width="180" />
      <el-table-column prop="permType" label="类型" width="100">
        <template #default="{ row }">{{ typeLabel(row.permType) }}</template>
      </el-table-column>
      <el-table-column prop="path" label="路由路径" />
      <el-table-column prop="component" label="组件" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:menu:add'" link type="primary" @click="openCreate(row)">子项</el-button>
          <el-button v-permission="'system:menu:edit'" link @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'system:menu:delete'" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="visible" :title="dialogTitle" width="520px" destroy-on-close>
    <el-form :model="form" label-width="100px">
      <el-form-item label="父节点 ID">
        <el-input v-model.number="form.parentId" disabled />
      </el-form-item>
      <el-form-item label="权限标识" required>
        <el-input v-model="form.permCode" />
      </el-form-item>
      <el-form-item label="名称" required>
        <el-input v-model="form.permName" />
      </el-form-item>
      <el-form-item label="类型" required>
        <el-select v-model="form.permType" style="width: 100%">
          <el-option :value="1" label="目录" />
          <el-option :value="2" label="菜单" />
          <el-option :value="3" label="按钮" />
          <el-option :value="4" label="接口" />
        </el-select>
      </el-form-item>
      <el-form-item label="路由 path">
        <el-input v-model="form.path" placeholder="如 /system/demo" />
      </el-form-item>
      <el-form-item label="组件路径">
        <el-input v-model="form.component" placeholder="如 system/Demo" />
      </el-form-item>
      <el-form-item label="图标">
        <el-input v-model="form.icon" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" :min="0" />
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
import { fetchMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/system'

const tree = ref([])
const visible = ref(false)
const form = reactive({
  id: null,
  parentId: 0,
  permCode: '',
  permName: '',
  permType: 2,
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  status: 1,
})

const dialogTitle = computed(() => (form.id ? '编辑菜单' : '新增菜单'))

function typeLabel(t) {
  const m = { 1: '目录', 2: '菜单', 3: '按钮', 4: '接口' }
  return m[t] || t
}

async function load() {
  const res = await fetchMenuTree()
  tree.value = res.data
}

function openCreate(parent) {
  Object.assign(form, {
    id: null,
    parentId: parent ? parent.id : 0,
    permCode: '',
    permName: '',
    permType: parent ? 2 : 1,
    path: '',
    component: '',
    icon: '',
    sortOrder: 0,
    status: 1,
  })
  visible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId ?? 0,
    permCode: row.permCode,
    permName: row.permName,
    permType: row.permType,
    path: row.path || '',
    component: row.component || '',
    icon: row.icon || '',
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
  })
  visible.value = true
}

async function save() {
  if (!form.permCode || !form.permName) {
    ElMessage.warning('请填写权限标识与名称')
    return
  }
  const payload = { ...form }
  if (form.id) {
    await updateMenu(payload)
  } else {
    await createMenu(payload)
  }
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除？有子节点时无法删除。', '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
}
</style>
