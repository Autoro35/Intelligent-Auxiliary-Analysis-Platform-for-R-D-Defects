<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>标签云</span>
          <span class="text-muted">共 {{ tags.length }} 个标签，点击可快速筛选</span>
        </div>
      </template>
      <div v-if="tags.length" class="tag-cloud">
        <el-tag
          v-for="tag in tags"
          :key="tag.name"
          :type="query.tag === tag.name ? 'primary' : 'info'"
          :effect="query.tag === tag.name ? 'dark' : 'plain'"
          class="tag-item"
          @click="filterByTag(tag.name)"
        >
          {{ tag.name }} × {{ tag.count }}
        </el-tag>
      </div>
      <el-empty v-else description="暂无标签" :image-size="60" />
    </el-card>

    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索标题/根因/解决方案" clearable style="width: 240px" @keyup.enter="search" />
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 160px">
          <el-option v-for="item in DEFECT_TYPE" :key="item.code" :label="item.label" :value="item.code" />
        </el-select>
        <el-input v-model="query.tag" placeholder="按标签筛选" clearable style="width: 160px" @keyup.enter="search" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
        <div style="flex: 1"></div>
        <el-button v-if="userStore.canWrite" type="primary" :icon="Plus" @click="openCreate">新增知识</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe empty-text="暂无知识条目">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/knowledge/${row.id}`)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">{{ row.typeDesc || orDash(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="rootCause" label="根因分类" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ orDash(row.rootCause) }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="tag in (row.tagList || [])" :key="tag" size="small" effect="plain" class="mr-4">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" align="center" />
        <el-table-column prop="createByName" label="创建人" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ orDash(row.createByName) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/knowledge/${row.id}`)">详情</el-button>
            <el-button v-if="userStore.canWrite" link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialog.visible" :title="dialog.id ? '编辑知识' : '新增知识'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入知识标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="缺陷类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="item in DEFECT_TYPE" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="根因分类" prop="rootCause">
          <el-input v-model="form.rootCause" maxlength="100" placeholder="如：缺少联合索引" />
        </el-form-item>
        <el-form-item label="解决方案" prop="solution">
          <el-input v-model="form.solution" type="textarea" :rows="5" placeholder="请描述解决思路与具体做法" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tags" placeholder="多个标签用英文逗号分隔，如：数据库,性能" maxlength="255" />
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
import { listKnowledge, createKnowledge, updateKnowledge, deleteKnowledge, listTags } from '@/api/knowledge'
import { useUserStore } from '@/stores/user'
import { DEFECT_TYPE } from '@/constants/enums'
import { orDash } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const tags = ref([])

const query = reactive({ current: 1, size: 10, keyword: '', type: null, tag: '' })

const formRef = ref()
const dialog = reactive({ visible: false, id: null })
const form = reactive({ title: '', type: '', rootCause: '', solution: '', tags: '' })
const rules = {
  title: [{ required: true, message: '请输入知识标题', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await listKnowledge({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      tag: query.tag || undefined
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  tags.value = await listTags()
}

function search() {
  query.current = 1
  load()
}

function reset() {
  Object.assign(query, { current: 1, keyword: '', type: null, tag: '' })
  load()
}

function filterByTag(name) {
  query.tag = query.tag === name ? '' : name
  search()
}

function openCreate() {
  dialog.id = null
  Object.assign(form, { title: '', type: '', rootCause: '', solution: '', tags: '' })
  dialog.visible = true
}

function openEdit(row) {
  dialog.id = row.id
  Object.assign(form, {
    title: row.title,
    type: row.type || '',
    rootCause: row.rootCause || '',
    solution: row.solution || '',
    tags: row.tags || ''
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
    const payload = {
      title: form.title,
      type: form.type || undefined,
      rootCause: form.rootCause || undefined,
      solution: form.solution || undefined,
      tags: form.tags || undefined
    }
    if (dialog.id) {
      await updateKnowledge(dialog.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createKnowledge(payload)
      ElMessage.success('创建成功')
    }
    dialog.visible = false
    await Promise.all([load(), loadTags()])
  } finally {
    submitting.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除知识「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteKnowledge(row.id)
  ElMessage.success('删除成功')
  await Promise.all([load(), loadTags()])
}

onMounted(async () => {
  await Promise.all([load(), loadTags()])
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  cursor: pointer;
}

.mr-4 {
  margin-right: 4px;
}
</style>
