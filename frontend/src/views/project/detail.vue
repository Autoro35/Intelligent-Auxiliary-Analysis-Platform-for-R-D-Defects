<template>
  <div class="page-container" v-loading="loading">
    <el-page-header class="page-card" @back="router.push('/projects')">
      <template #content>
        <span class="header-title">{{ project.name || '项目详情' }}</span>
        <el-tag v-if="project.status !== undefined" :type="project.status === 1 ? 'success' : 'info'" size="small" style="margin-left: 8px">
          {{ project.status === 1 ? '进行中' : '已归档' }}
        </el-tag>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="page-card">
          <template #header><span>基本信息</span></template>
          <div class="detail-item">
            <div class="detail-label">项目编码</div>
            <div class="detail-value">{{ orDash(project.code) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">负责人</div>
            <div class="detail-value">{{ orDash(project.ownerName) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">成员数</div>
            <div class="detail-value">{{ project.memberCount ?? 0 }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">创建时间</div>
            <div class="detail-value">{{ orDash(project.createTime) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">项目描述</div>
            <div class="detail-value pre-wrap">{{ orDash(project.description) }}</div>
          </div>
          <div class="mt-16">
            <el-button type="primary" :icon="Warning" @click="goDefects">查看该项目缺陷</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-header">
              <span>项目成员</span>
              <el-button type="primary" size="small" :icon="Plus" @click="openAddMember">添加成员</el-button>
            </div>
          </template>

          <el-table :data="members" border stripe empty-text="暂无成员">
            <el-table-column prop="username" label="用户名" min-width="110" show-overflow-tooltip />
            <el-table-column prop="nickname" label="昵称" min-width="110" show-overflow-tooltip />
            <el-table-column label="项目角色" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.roleDesc || row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-button link type="danger" @click="onRemoveMember(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="memberDialog.visible" title="添加项目成员" width="460px" destroy-on-close>
      <el-form ref="memberFormRef" :model="memberForm" :rules="memberRules" label-width="90px">
        <el-form-item label="用户" prop="userId">
          <!-- 用户列表接口仅管理员可访问；非管理员改为手工填写用户 ID -->
          <el-select
            v-if="userStore.isAdmin"
            v-model="memberForm.userId"
            placeholder="请选择用户"
            filterable
            :loading="userLoading"
            style="width: 100%"
          >
            <el-option v-for="u in userOptions" :key="u.id" :label="`${u.nickname || u.username}（${u.username}）`" :value="u.id" />
          </el-select>
          <el-input-number v-else v-model="memberForm.userId" :min="1" :controls="false" placeholder="请输入用户 ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="项目角色" prop="role">
          <el-select v-model="memberForm.role" placeholder="请选择项目内角色" style="width: 100%">
            <el-option v-for="item in PROJECT_ROLE" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMember">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Warning } from '@element-plus/icons-vue'
import { getProject, listMembers, addMember, removeMember } from '@/api/project'
import { listUsers } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { PROJECT_ROLE } from '@/constants/enums'
import { orDash } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const projectId = route.params.id
const loading = ref(false)
const submitting = ref(false)
const project = ref({})
const members = ref([])

const memberFormRef = ref()
const memberDialog = reactive({ visible: false })
const memberForm = reactive({ userId: null, role: 'DEV' })
const memberRules = {
  userId: [{ required: true, message: '请选择或填写用户 ID', trigger: 'change' }],
  role: [{ required: true, message: '请选择项目角色', trigger: 'change' }]
}

const userLoading = ref(false)
const userOptions = ref([])

async function loadDetail() {
  loading.value = true
  try {
    const [projectData, memberData] = await Promise.all([getProject(projectId), listMembers(projectId)])
    project.value = projectData || {}
    members.value = memberData || []
  } finally {
    loading.value = false
  }
}

function goDefects() {
  router.push({ path: '/defects', query: { projectId } })
}

async function openAddMember() {
  memberForm.userId = null
  memberForm.role = 'DEV'
  memberDialog.visible = true
  if (userStore.isAdmin) {
    userLoading.value = true
    try {
      const data = await listUsers({ current: 1, size: 200 })
      userOptions.value = data.records || []
    } finally {
      userLoading.value = false
    }
  }
}

async function submitMember() {
  try {
    await memberFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await addMember(projectId, { userId: memberForm.userId, role: memberForm.role })
    ElMessage.success('添加成功')
    memberDialog.visible = false
    loadDetail()
  } finally {
    submitting.value = false
  }
}

async function onRemoveMember(row) {
  try {
    await ElMessageBox.confirm(`确定将「${row.nickname || row.username}」移出项目吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await removeMember(projectId, row.userId)
  ElMessage.success('已移除')
  loadDetail()
}

onMounted(loadDetail)
</script>

<style scoped>
.header-title {
  font-size: 16px;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
