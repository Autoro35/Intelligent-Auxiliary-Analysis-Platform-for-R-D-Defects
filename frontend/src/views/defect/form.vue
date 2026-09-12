<template>
  <div class="page-container" v-loading="loading">
    <el-page-header class="page-card" @back="router.back()">
      <template #content>
        <span class="header-title">{{ isEdit ? `编辑缺陷 #${defectId}` : '新建缺陷' }}</span>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-header">
              <span>缺陷信息</span>
              <div>
                <el-button size="small" :icon="MagicStick" :loading="aiClassifying" @click="onAiClassify">
                  AI 智能判定
                </el-button>
                <el-button size="small" :icon="EditPen" :loading="aiCompleting" @click="onAiComplete">
                  AI 补全描述
                </el-button>
              </div>
            </div>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="所属项目" prop="projectId">
              <el-select v-model="form.projectId" placeholder="请选择项目" filterable style="width: 100%">
                <el-option v-for="p in projects" :key="p.id" :label="`${p.name}（${p.code}）`" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="缺陷标题" prop="title">
              <el-input v-model="form.title" placeholder="一句话描述问题现象" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item label="缺陷描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="4" placeholder="描述问题现象，可分点说明" />
            </el-form-item>

            <el-row :gutter="16">
              <el-col :xs="24" :sm="8">
                <el-form-item label="缺陷类型" prop="type">
                  <el-select v-model="form.type" placeholder="请选择" clearable style="width: 100%">
                    <el-option v-for="item in DEFECT_TYPE" :key="item.code" :label="item.label" :value="item.code" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="优先级" prop="priority">
                  <el-select v-model="form.priority" placeholder="请选择" clearable style="width: 100%">
                    <el-option v-for="item in DEFECT_PRIORITY" :key="item.code" :label="item.label" :value="item.code" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="严重程度" prop="severity">
                  <el-select v-model="form.severity" placeholder="请选择" clearable style="width: 100%">
                    <el-option v-for="item in DEFECT_SEVERITY" :key="item.code" :label="item.label" :value="item.code" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- AI 分类判定结论 -->
            <el-alert v-if="aiClassifyResult" class="mb-16" :closable="false" type="success" show-icon>
              <template #title>
                <span>
                  AI 判定结果：{{ aiClassifyResult.typeDesc }} / {{ aiClassifyResult.priorityDesc }} /
                  {{ aiClassifyResult.severityDesc }}
                  <el-tag size="small" :type="aiClassifyResult.source === 'LLM' ? 'success' : 'warning'" style="margin-left: 8px">
                    {{ aiClassifyResult.source === 'LLM' ? '大模型' : '本地规则' }}
                  </el-tag>
                  <el-tag size="small" type="info" style="margin-left: 4px">
                    置信度 {{ (aiClassifyResult.confidence * 100).toFixed(0) }}%
                  </el-tag>
                </span>
              </template>
              <div class="ai-reason">{{ aiClassifyResult.reason }}</div>
            </el-alert>

            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item label="所属模块" prop="module">
                  <el-input v-model="form.module" placeholder="如：订单中心" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="运行环境" prop="environment">
                  <el-input v-model="form.environment" placeholder="如：Chrome 120 / Windows 11" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="复现步骤" prop="reproduceSteps">
              <el-input v-model="form.reproduceSteps" type="textarea" :rows="3" placeholder="1. 打开…&#10;2. 点击…&#10;3. 观察…" />
            </el-form-item>
            <el-form-item label="预期结果" prop="expectedResult">
              <el-input v-model="form.expectedResult" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="实际结果" prop="actualResult">
              <el-input v-model="form.actualResult" type="textarea" :rows="2" />
            </el-form-item>

            <el-divider content-position="left">解决信息（可后补）</el-divider>
            <el-form-item label="根因分类" prop="rootCause">
              <el-input v-model="form.rootCause" placeholder="如：缺少联合索引" />
            </el-form-item>
            <el-form-item label="解决方案" prop="solution">
              <el-input v-model="form.solution" type="textarea" :rows="3" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submit">
                {{ isEdit ? '保存修改' : '提交缺陷' }}
              </el-button>
              <el-button @click="router.back()">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：AI 辅助区 -->
      <el-col :xs="24" :lg="8">
        <el-card v-if="aiCompletedPreview" shadow="never" class="page-card">
          <template #header>
            <div class="card-header">
              <span>AI 生成的完整描述</span>
              <el-button size="small" type="primary" @click="applyCompletedDescription">采用</el-button>
            </div>
          </template>
          <div class="pre-wrap text-muted">{{ aiCompletedPreview }}</div>
        </el-card>

        <el-card v-if="aiQuestions.length" shadow="never" class="page-card">
          <template #header><span>AI 建议补充</span></template>
          <ul class="question-list">
            <li v-for="(q, index) in aiQuestions" :key="index">{{ q }}</li>
          </ul>
        </el-card>

        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-header">
              <span>相似历史缺陷推荐</span>
              <el-button size="small" :icon="Search" :loading="aiRecommending" @click="onAiRecommend">检索</el-button>
            </div>
          </template>

          <div v-if="recommend" class="mb-8">
            <el-tag size="small" type="info">检索方式：{{ retrievalModeText }}</el-tag>
            <el-tag v-if="recommend.source" size="small" :type="recommend.source === 'LLM' ? 'success' : 'warning'" style="margin-left: 4px">
              {{ recommend.source === 'LLM' ? '大模型' : recommend.source === 'RULE' ? '本地规则' : '无候选' }}
            </el-tag>
          </div>

          <el-empty v-if="recommend && !recommend.recommendations.length" description="知识库中暂无相似缺陷" :image-size="70" />

          <div v-for="item in (recommend ? recommend.recommendations : [])" :key="item.knowledgeId" class="recommend-item">
            <div class="recommend-title">
              <el-link type="primary" @click="openKnowledge(item.knowledgeId)">{{ item.title }}</el-link>
              <el-tag v-if="item.score !== null && item.score !== undefined" size="small" type="info">
                {{ item.score }}
              </el-tag>
            </div>
            <div class="recommend-row"><span class="text-muted">根因：</span>{{ orDash(item.rootCause) }}</div>
            <div class="recommend-row"><span class="text-muted">方案：</span>{{ orDash(item.solution) }}</div>
            <div class="recommend-row text-muted">推荐理由：{{ item.reason }}</div>
          </div>

          <el-divider v-if="recommend && recommend.summary" />

          <div v-if="recommend && recommend.summary">
            <div class="mb-8"><strong>AI 综合建议</strong></div>
            <div class="pre-wrap text-muted">{{ recommend.summary }}</div>
            <div v-if="recommend.rootCauseGuess" class="mt-16">
              <div class="mb-8"><strong>根因推测</strong></div>
              <div class="pre-wrap text-muted">{{ recommend.rootCauseGuess }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { EditPen, MagicStick, Search } from '@element-plus/icons-vue'
import { createDefect, getDefect, updateDefect } from '@/api/defect'
import { listProjects } from '@/api/project'
import { aiClassify, aiCompleteDescription, aiRecommend } from '@/api/ai'
import { DEFECT_TYPE, DEFECT_PRIORITY, DEFECT_SEVERITY } from '@/constants/enums'
import { orDash } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const defectId = route.params.id ? Number(route.params.id) : null
const isEdit = computed(() => !!defectId)

const loading = ref(false)
const submitting = ref(false)
const projects = ref([])

const formRef = ref()
const form = reactive({
  projectId: null,
  title: '',
  description: '',
  type: '',
  priority: '',
  severity: '',
  module: '',
  environment: '',
  reproduceSteps: '',
  expectedResult: '',
  actualResult: '',
  solution: '',
  rootCause: ''
})

const rules = {
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  title: [{ required: true, message: '请输入缺陷标题', trigger: 'blur' }]
}

// ---- AI 能力 ----
const aiClassifying = ref(false)
const aiCompleting = ref(false)
const aiRecommending = ref(false)
const aiClassifyResult = ref(null)
const aiQuestions = ref([])
const aiCompletedPreview = ref('')
const recommend = ref(null)

const retrievalModeText = computed(() => {
  const map = { VECTOR: '向量检索', FULLTEXT: '全文索引', KEYWORD: '关键词匹配', NONE: '无' }
  return map[recommend.value?.retrievalMode] || recommend.value?.retrievalMode || '-'
})

/** 组装 AI 接口入参：优先用表单当前内容，标题为空时不发请求 */
function buildAiPayload() {
  if (!form.title && !form.description) {
    ElMessage.warning('请先填写缺陷标题或描述，AI 才能进行分析')
    return null
  }
  return {
    title: form.title || undefined,
    description: form.description || undefined,
    type: form.type || undefined,
    module: form.module || undefined,
    environment: form.environment || undefined
  }
}

async function onAiClassify() {
  const payload = buildAiPayload()
  if (!payload) return
  aiClassifying.value = true
  try {
    const data = await aiClassify(payload)
    aiClassifyResult.value = data
    // 回填建议值
    form.type = data.type
    form.priority = data.priority
    form.severity = data.severity
    ElMessage.success('AI 判定完成，已回填类型/优先级/严重程度')
  } finally {
    aiClassifying.value = false
  }
}

async function onAiComplete() {
  const payload = buildAiPayload()
  if (!payload) return
  aiCompleting.value = true
  try {
    const data = await aiCompleteDescription(payload)
    // 分字段建议直接回填：这几个字段服务端只产出对应内容，覆盖是安全的
    const allowedFields = ['reproduceSteps', 'expectedResult', 'actualResult', 'environment']
    for (const item of data.suggestions || []) {
      if (allowedFields.includes(item.field) && item.content) {
        form[item.field] = item.content
      }
    }
    // 整合后的完整描述不直接覆盖用户输入，放右侧预览由用户决定是否采用
    aiCompletedPreview.value = data.completedDescription || ''
    aiQuestions.value = data.questions || []
    ElMessage.success('AI 补全完成，请检查右侧建议后确认')
  } finally {
    aiCompleting.value = false
  }
}

async function onAiRecommend() {
  const payload = buildAiPayload()
  if (!payload) return
  aiRecommending.value = true
  try {
    recommend.value = await aiRecommend(payload)
  } finally {
    aiRecommending.value = false
  }
}

/** 用户确认后才把 AI 生成的完整描述写入描述字段 */
function applyCompletedDescription() {
  if (!aiCompletedPreview.value) return
  form.description = aiCompletedPreview.value
  ElMessage.success('已采用 AI 生成的完整描述')
}

function openKnowledge(id) {
  // 新窗口打开，避免丢失当前表单内容
  const href = `${window.location.origin}${window.location.pathname}#/knowledge/${id}`
  window.open(href, '_blank')
}

// ---- 数据加载与提交 ----

async function loadProjects() {
  const data = await listProjects({ current: 1, size: 200 })
  projects.value = data.records || []
}

async function loadDefect() {
  const data = await getDefect(defectId)
  Object.keys(form).forEach((key) => {
    if (data[key] !== null && data[key] !== undefined) {
      form[key] = data[key]
    }
  })
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDefect(defectId, { ...form })
      ElMessage.success('保存成功')
    } else {
      const created = await createDefect({ ...form })
      ElMessage.success('提交成功')
      router.replace(`/defects/${created.id}`)
      return
    }
    router.push(`/defects/${defectId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await loadProjects()
    if (isEdit.value) {
      await loadDefect()
    } else if (route.query.projectId) {
      form.projectId = Number(route.query.projectId)
    }
  } finally {
    loading.value = false
  }
})
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

.ai-reason {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.6;
}

.question-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.9;
  color: #606266;
}

.recommend-item {
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.recommend-item:last-child {
  border-bottom: none;
}

.recommend-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.recommend-row {
  font-size: 13px;
  line-height: 1.7;
}
</style>
