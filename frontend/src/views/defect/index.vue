<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-select v-model="query.projectId" placeholder="全部项目" clearable filterable style="width: 200px">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="item in DEFECT_STATUS" :key="item.code" :label="item.label" :value="item.code" />
        </el-select>
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 140px">
          <el-option v-for="item in DEFECT_TYPE" :key="item.code" :label="item.label" :value="item.code" />
        </el-select>
        <el-select v-model="query.priority" placeholder="全部优先级" clearable style="width: 130px">
          <el-option v-for="item in DEFECT_PRIORITY" :key="item.code" :label="item.label" :value="item.code" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="按标题搜索" clearable style="width: 180px" @keyup.enter="search" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
        <div style="flex: 1"></div>
        <el-button v-if="userStore.canWrite" type="primary" :icon="Plus" @click="goCreate">新建缺陷</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <!-- 空态区分两种原因：没有可见项目（数据权限导致） vs 有项目但当前筛选无结果 -->
        <template #empty>
          <el-empty :image-size="90" :description="emptyDescription">
            <el-button v-if="userStore.canWrite && projects.length" type="primary" :icon="Plus" @click="goCreate">新建缺陷</el-button>
          </el-empty>
        </template>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="缺陷标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row.id)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">{{ row.typeDesc || row.type }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="dictTag(DEFECT_PRIORITY, row.priority)" size="small">{{ row.priorityDesc || row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="严重程度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictTag(DEFECT_SEVERITY, row.severity)" size="small" effect="plain">
              {{ row.severityDesc || row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictTag(DEFECT_STATUS, row.status)" size="small">{{ row.statusDesc || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="处理人" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ orDash(row.assigneeName) }}</template>
        </el-table-column>
        <el-table-column prop="reporterName" label="提交人" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ orDash(row.reporterName) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-button v-if="userStore.canWrite" link type="primary" @click="goEdit(row.id)">编辑</el-button>
            <el-button v-if="userStore.canWrite" link type="danger" @click="onDelete(row)">删除</el-button>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { listDefects, deleteDefect } from '@/api/defect'
import { listProjects } from '@/api/project'
import { useUserStore } from '@/stores/user'
import { DEFECT_STATUS, DEFECT_TYPE, DEFECT_PRIORITY, DEFECT_SEVERITY, dictTag } from '@/constants/enums'
import { orDash } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const projects = ref([])

const query = reactive({
  current: 1,
  size: 10,
  projectId: route.query.projectId ? Number(route.query.projectId) : null,
  status: null,
  type: null,
  priority: null,
  keyword: ''
})

/** 是否处于筛选状态，用于区分「筛选无结果」与「确实没有数据」 */
const hasFilter = computed(() =>
  !!(query.status || query.type || query.priority || query.keyword || query.projectId)
)

/**
 * 空态文案：缺陷可见范围由「可访问项目」决定。
 * 没有任何可访问项目时，空列表是数据权限的正常结果，必须与「筛选无结果」区分开。
 */
const emptyDescription = computed(() => {
  if (!projects.value.length) {
    return userStore.isAdmin
      ? '暂无缺陷。请先创建项目并添加成员，再提交缺陷'
      : '你还没有加入任何项目，因此看不到缺陷。请联系项目负责人或管理员将你添加为项目成员'
  }
  return hasFilter.value ? '当前筛选条件下没有缺陷，可尝试重置查询条件' : '暂无缺陷，可点击「新建缺陷」提交'
})

async function load() {
  loading.value = true
  try {
    const data = await listDefects({
      current: query.current,
      size: query.size,
      projectId: query.projectId || undefined,
      status: query.status || undefined,
      type: query.type || undefined,
      priority: query.priority || undefined,
      keyword: query.keyword || undefined
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadProjects() {
  const data = await listProjects({ current: 1, size: 200 })
  projects.value = data.records || []
}

function search() {
  query.current = 1
  load()
}

function reset() {
  Object.assign(query, { current: 1, projectId: null, status: null, type: null, priority: null, keyword: '' })
  load()
}

function goCreate() {
  router.push('/defects/create')
}

function goDetail(id) {
  router.push(`/defects/${id}`)
}

function goEdit(id) {
  router.push(`/defects/${id}/edit`)
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除缺陷「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteDefect(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  await Promise.all([load(), loadProjects()])
})
</script>
