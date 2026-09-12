<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="按项目名称搜索" clearable @keyup.enter="search" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
        <div style="flex: 1"></div>
        <el-button v-if="userStore.canWrite" type="primary" :icon="Plus" @click="openCreate">新建项目</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe empty-text="暂无项目">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="项目名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row.id)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="项目编码" width="160" show-overflow-tooltip />
        <el-table-column prop="ownerName" label="负责人" width="120" show-overflow-tooltip />
        <el-table-column prop="memberCount" label="成员数" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '进行中' : '已归档' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-button v-if="canManage(row)" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canManage(row)" link type="danger" @click="onDelete(row)">删除</el-button>
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

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dialog.visible" :title="dialog.id ? '编辑项目' : '新建项目'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="项目编码" prop="code">
          <el-input v-model="form.code" placeholder="唯一标识，如 CRM-2026" maxlength="50" />
        </el-form-item>
        <el-form-item label="负责人" prop="ownerId">
          <!-- 用户列表接口仅管理员可访问，非管理员只能沿用当前负责人 -->
          <el-select
            v-if="userStore.isAdmin"
            v-model="form.ownerId"
            placeholder="默认当前登录用户"
            clearable
            filterable
            :loading="ownerLoading"
            style="width: 100%"
          >
            <el-option v-for="u in ownerOptions" :key="u.id" :label="`${u.nickname || u.username}（${u.username}）`" :value="u.id" />
          </el-select>
          <el-input v-else :model-value="form.ownerName" disabled placeholder="当前负责人" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">进行中</el-radio>
            <el-radio :value="0">已归档</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { listProjects, createProject, updateProject, deleteProject } from '@/api/project'
import { listUsers } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10, keyword: '' })

const formRef = ref()
const dialog = reactive({ visible: false, id: null })
const form = reactive({ name: '', code: '', ownerId: null, ownerName: '', status: 1, description: '' })
const rules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入项目编码', trigger: 'blur' }]
}

// 负责人候选：仅管理员能拉取用户列表，非管理员只能默认自己
const ownerLoading = ref(false)
const ownerOptions = ref([])

/** 编辑/删除：管理员或项目负责人（与后端 assertOwner 一致） */
function canManage(row) {
  if (userStore.isAdmin) return true
  return !!row.ownerId && row.ownerId === userStore.userInfo?.id
}

async function load() {
  loading.value = true
  try {
    const data = await listProjects({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined
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
  query.keyword = ''
  search()
}

function goDetail(id) {
  router.push(`/projects/${id}`)
}

async function loadOwnerOptions() {
  if (!userStore.isAdmin || ownerOptions.value.length) return
  ownerLoading.value = true
  try {
    const data = await listUsers({ current: 1, size: 200 })
    ownerOptions.value = data.records || []
  } finally {
    ownerLoading.value = false
  }
}

async function openCreate() {
  dialog.id = null
  Object.assign(form, { name: '', code: '', ownerId: null, ownerName: '', status: 1, description: '' })
  dialog.visible = true
  await loadOwnerOptions()
}

async function openEdit(row) {
  dialog.id = row.id
  Object.assign(form, {
    name: row.name,
    code: row.code,
    ownerId: row.ownerId,
    ownerName: row.ownerName,
    status: row.status,
    description: row.description
  })
  dialog.visible = true
  await loadOwnerOptions()
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    // 只提交后端 ProjectDTO 声明的字段（ownerName 仅用于非管理员的只读展示）
    const payload = {
      name: form.name,
      code: form.code,
      ownerId: form.ownerId || undefined,
      status: form.status,
      description: form.description
    }
    if (dialog.id) {
      await updateProject(dialog.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createProject(payload)
      ElMessage.success('创建成功')
    }
    dialog.visible = false
    load()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除项目「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteProject(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
