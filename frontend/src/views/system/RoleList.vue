<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-permission="'system:role:add'" type="primary" @click="openCreate">新增角色</el-button>
    </div>
    <el-table :data="rows" border stripe style="width: 100%; margin-top: 16px">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="roleCode" label="编码" />
      <el-table-column prop="roleName" label="名称" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-permission="'system:role:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-permission="'system:role:delete'"
            link
            type="danger"
            :disabled="row.id === 1"
            @click="remove(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="visible" :title="form.id ? '编辑角色' : '新增角色'" width="520px" destroy-on-close>
    <el-form :model="form" label-width="88px">
      <el-form-item label="编码" required>
        <el-input v-model="form.roleCode" :disabled="form.id === 1" />
      </el-form-item>
      <el-form-item label="名称" required>
        <el-input v-model="form.roleName" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" />
      </el-form-item>
      <el-form-item label="权限">
        <el-tree
          ref="treeRef"
          :data="permTree"
          show-checkbox
          node-key="id"
          default-expand-all
          :props="{ label: 'permName', children: 'children' }"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchRoles, createRole, updateRole, deleteRole, fetchMenuTree } from '@/api/system'

const rows = ref([])
const permTree = ref([])
const visible = ref(false)
const treeRef = ref()
const form = reactive({
  id: null,
  roleCode: '',
  roleName: '',
  remark: '',
  permissionIds: [],
})

async function load() {
  const res = await fetchRoles()
  rows.value = res.data
}

async function loadTree() {
  const res = await fetchMenuTree()
  permTree.value = res.data
}

function openCreate() {
  Object.assign(form, { id: null, roleCode: '', roleName: '', remark: '', permissionIds: [] })
  visible.value = true
  nextTick(() => treeRef.value?.setCheckedKeys([]))
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    roleCode: row.roleCode,
    roleName: row.roleName,
    remark: row.remark || '',
    permissionIds: row.permissionIds || [],
  })
  visible.value = true
  nextTick(() => treeRef.value?.setCheckedKeys(form.permissionIds))
}

function collectCheckedIds() {
  const t = treeRef.value
  if (!t) return []
  const a = t.getCheckedKeys(false)
  const b = t.getHalfCheckedKeys()
  return [...new Set([...a, ...b])]
}

async function save() {
  if (!form.roleCode || !form.roleName) {
    ElMessage.warning('请填写编码与名称')
    return
  }
  const permissionIds = collectCheckedIds()
  const payload = {
    id: form.id,
    roleCode: form.roleCode,
    roleName: form.roleName,
    remark: form.remark,
    permissionIds,
  }
  if (form.id) {
    await updateRole(payload)
  } else {
    await createRole(payload)
  }
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除该角色？', '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => {
  loadTree()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
}
</style>
