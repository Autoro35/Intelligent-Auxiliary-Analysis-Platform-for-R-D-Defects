<template>
  <div class="page-container" v-loading="loading">
    <el-page-header class="page-card" @back="router.push('/knowledge')">
      <template #content>
        <span class="header-title">{{ knowledge.title || '知识详情' }}</span>
      </template>
      <template #extra>
        <el-button v-if="userStore.canWrite" :icon="EditPen" @click="openEdit">编辑</el-button>
        <el-button v-if="userStore.canWrite" type="danger" plain :icon="Delete" @click="onDelete">删除</el-button>
      </template>
    </el-page-header>

    <el-card shadow="never" class="page-card">
      <div class="meta-bar">
        <el-tag v-if="knowledge.type" type="primary" effect="plain">{{ knowledge.typeDesc || knowledge.type }}</el-tag>
        <el-tag v-for="tag in (knowledge.tagList || [])" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
        <div style="flex: 1"></div>
        <span class="text-muted">
          浏览 {{ knowledge.viewCount ?? 0 }} 次 · {{ orDash(knowledge.createByName) }} 创建于 {{ orDash(knowledge.createTime) }}
        </span>
      </div>

      <el-divider content-position="left">根因分类</el-divider>
      <div class="pre-wrap">{{ orDash(knowledge.rootCause) }}</div>

      <el-divider content-position="left">解决方案</el-divider>
      <div class="pre-wrap">{{ orDash(knowledge.solution) }}</div>

      <template v-if="knowledge.defectId">
        <el-divider />
        <div>
          来源缺陷：
          <el-link type="primary" @click="router.push(`/defects/${knowledge.defectId}`)">#{{ knowledge.defectId }}</el-link>
        </div>
      </template>
    </el-card>

    <el-dialog v-model="dialog.visible" title="编辑知识" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="缺陷类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="item in DEFECT_TYPE" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="根因分类" prop="rootCause">
          <el-input v-model="form.rootCause" maxlength="100" />
        </el-form-item>
        <el-form-item label="解决方案" prop="solution">
          <el-input v-model="form.solution" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tags" placeholder="多个标签用英文逗号分隔" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, EditPen } from '@element-plus/icons-vue'
import { getKnowledge, updateKnowledge, deleteKnowledge } from '@/api/knowledge'
import { useUserStore } from '@/stores/user'
import { DEFECT_TYPE } from '@/constants/enums'
import { orDash } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const knowledgeId = Number(route.params.id)
const loading = ref(false)
const submitting = ref(false)
const knowledge = ref({})

const formRef = ref()
const dialog = reactive({ visible: false })
const form = reactive({ title: '', type: '', rootCause: '', solution: '', tags: '' })
const rules = {
  title: [{ required: true, message: '请输入知识标题', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    knowledge.value = await getKnowledge(knowledgeId)
  } finally {
    loading.value = false
  }
}

function openEdit() {
  Object.assign(form, {
    title: knowledge.value.title,
    type: knowledge.value.type || '',
    rootCause: knowledge.value.rootCause || '',
    solution: knowledge.value.solution || '',
    tags: knowledge.value.tags || ''
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
    await updateKnowledge(knowledgeId, {
      title: form.title,
      type: form.type || undefined,
      rootCause: form.rootCause || undefined,
      solution: form.solution || undefined,
      tags: form.tags || undefined
    })
    ElMessage.success('保存成功')
    dialog.visible = false
    // 重新拉取会被浏览计数 +1，这里直接本地更新避免虚增
    knowledge.value = { ...knowledge.value, ...form }
  } finally {
    submitting.value = false
  }
}

async function onDelete() {
  try {
    await ElMessageBox.confirm(`确定删除知识「${knowledge.value.title}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteKnowledge(knowledgeId)
  ElMessage.success('删除成功')
  router.push('/knowledge')
}

onMounted(load)
</script>

<style scoped>
.header-title {
  font-size: 16px;
  font-weight: 600;
}

.meta-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
