<template>
  <div class="page-container">
    <el-alert
      type="info"
      :closable="false"
      class="page-card"
      title="新增成员请点「新建用户」并直接指定角色；对已有账号（如自行注册的访客）可在此调整角色，用户重新登录后生效。"
    />

    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索用户名 / 昵称" clearable style="width: 200px" @keyup.enter="search" />
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 140px">
          <el-option v-for="item in ROLE" :key="item.code" :label="item.label" :value="item.code" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
        <div style="flex: 1"></div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建用户</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe empty-text="暂无用户">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" min-width="130" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ orDash(row.email) }}</template>
        </el-table-column>
        <el-table-column label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ row.roleDesc || row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="isSelf(row)">
              <el-tag size="small" type="info" effect="plain">当前登录账号</el-tag>
            </template>
            <template v-else>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openPassword(row)">重置密码</el-button>
              <el-button link type="danger" @click="onDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <!-- 新建用户 -->
    <el-dialog v-model="createDialog.visible" title="新建用户" width="520px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="4-20 位，登录账号" maxlength="20" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" placeholder="选填，默认与用户名相同" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option v-for="item in ROLE" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
          <div class="form-tip">直接指定角色，用户首次登录即具备对应权限</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户 -->
    <el-dialog v-model="dialog.visible" title="编辑用户" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名">
          <el-input :model-value="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option v-for="item in ROLE" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
          <div class="form-tip">角色变更后需用户重新登录才生效（角色编码在登录令牌中）</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
          <div class="form-tip">禁用后该账号将无法登录</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="passwordDialog.visible" title="重置密码" width="460px" destroy-on-close>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="用户">
          <el-input :model-value="passwordDialog.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { listUsers, createUser, updateUser, resetUserPassword, deleteUser } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { ROLE } from '@/constants/enums'
import { orDash } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10, keyword: '', role: null, status: null })

const createFormRef = ref()
const createDialog = reactive({ visible: false })
const createForm = reactive({ username: '', password: '', nickname: '', email: '', role: 'TESTER' })
const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度需在 4-20 位之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const formRef = ref()
const dialog = reactive({ visible: false, id: null })
const form = reactive({ username: '', nickname: '', email: '', role: 'GUEST', status: 1 })
const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const passwordFormRef = ref()
const passwordDialog = reactive({ visible: false, id: null, username: '' })
const passwordForm = reactive({ password: '', confirmPassword: '' })
const passwordRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function roleTagType(role) {
  const map = { ADMIN: 'danger', TESTER: 'warning', DEVELOPER: 'primary', GUEST: 'info' }
  return map[role] || 'info'
}

function isSelf(row) {
  return row.id === userStore.userInfo?.id
}

async function load() {
  loading.value = true
  try {
    const data = await listUsers({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      role: query.role || undefined,
      status: query.status === null || query.status === '' ? undefined : query.status
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  Object.assign(query, { current: 1, keyword: '', role: null, status: null })
  load()
}

function openCreate() {
  Object.assign(createForm, { username: '', password: '', nickname: '', email: '', role: 'TESTER' })
  createDialog.visible = true
}

async function submitCreate() {
  try {
    await createFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await createUser({
      username: createForm.username,
      password: createForm.password,
      nickname: createForm.nickname || undefined,
      email: createForm.email || undefined,
      role: createForm.role
    })
    ElMessage.success('创建成功')
    createDialog.visible = false
    load()
  } finally {
    submitting.value = false
  }
}

function openEdit(row) {
  dialog.id = row.id
  Object.assign(form, {
    username: row.username,
    nickname: row.nickname || '',
    email: row.email || '',
    role: row.role,
    status: row.status
  })
  dialog.visible = true
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await updateUser(dialog.id, {
      nickname: form.nickname,
      email: form.email || undefined,
      role: form.role,
      status: form.status
    })
    ElMessage.success('保存成功')
    dialog.visible = false
    load()
  } finally {
    submitting.value = false
  }
}

function openPassword(row) {
  passwordDialog.id = row.id
  passwordDialog.username = row.username
  passwordForm.password = ''
  passwordForm.confirmPassword = ''
  passwordDialog.visible = true
}

async function submitPassword() {
  try {
    await passwordFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await resetUserPassword(passwordDialog.id, passwordForm.password)
    ElMessage.success('密码已重置')
    passwordDialog.visible = false
  } finally {
    submitting.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
