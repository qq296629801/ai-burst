<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.username" placeholder="用户名" clearable style="width: 200px" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option :value="1" label="正常" />
        <el-option :value="0" label="禁用" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-permission="'system:user:add'" type="success" @click="openCreate">新增</el-button>
    </div>
    <el-table :data="rows" border stripe style="width: 100%; margin-top: 16px">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="status" label="状态" width="88">
        <template #default="{ row }">{{ row.status === 1 ? '正常' : '禁用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button v-permission="'system:user:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'system:user:resetPwd'" link @click="openPwd(row)">重置密码</el-button>
          <el-button v-permission="'system:user:delete'" link type="danger" @click="remove(row)">删除</el-button>
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

  <el-dialog v-model="visible" :title="form.id ? '编辑用户' : '新增用户'" width="480px" destroy-on-close>
    <el-form :model="form" label-width="88px">
      <el-form-item label="用户名" required>
        <el-input v-model="form.username" :disabled="!!form.id" />
      </el-form-item>
      <el-form-item :label="form.id ? '新密码' : '密码'" :required="!form.id">
        <el-input v-model="form.password" type="password" show-password placeholder="留空则不修改" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
          <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="pwdVisible" title="重置密码" width="400px">
    <el-input v-model="pwdForm.password" type="password" show-password placeholder="至少 6 位" />
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" @click="savePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchUsers, createUser, updateUser, deleteUser, resetUserPassword, fetchRoles } from '@/api/system'

const query = reactive({ username: '', status: null, pageNum: 1, pageSize: 10 })
const rows = ref([])
const total = ref(0)
const roleOptions = ref([])
const visible = ref(false)
const pwdVisible = ref(false)
const form = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  status: 1,
  roleIds: [],
})
const pwdForm = reactive({ id: null, password: '' })

async function load() {
  const res = await fetchUsers(query)
  total.value = res.data.total
  rows.value = res.data.list
}

async function loadRoles() {
  const res = await fetchRoles()
  roleOptions.value = res.data.map((r) => ({
    id: r.id,
    roleName: r.roleName,
  }))
}

function openCreate() {
  Object.assign(form, { id: null, username: '', password: '', nickname: '', status: 1, roleIds: [] })
  visible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname,
    status: row.status,
    roleIds: [...(row.roleIds || [])],
  })
  visible.value = true
}

function openPwd(row) {
  pwdForm.id = row.id
  pwdForm.password = ''
  pwdVisible.value = true
}

async function save() {
  if (!form.username) {
    ElMessage.warning('请填写用户名')
    return
  }
  if (!form.id && !form.password) {
    ElMessage.warning('请填写密码')
    return
  }
  const payload = {
    id: form.id,
    username: form.username,
    password: form.password || undefined,
    nickname: form.nickname,
    status: form.status,
    roleIds: form.roleIds,
  }
  if (form.id) {
    await updateUser(payload)
  } else {
    await createUser(payload)
  }
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

async function savePwd() {
  if (!pwdForm.password || pwdForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  await resetUserPassword(pwdForm.id, pwdForm.password)
  ElMessage.success('已重置')
  pwdVisible.value = false
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => {
  loadRoles()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
