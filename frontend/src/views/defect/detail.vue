<template>
  <div class="page-container" v-loading="loading">
    <el-page-header class="page-card" @back="router.push('/defects')">
      <template #content>
        <span class="header-title">缺陷 #{{ defect.id }}</span>
        <el-tag v-if="defect.status" :type="dictTag(DEFECT_STATUS, defect.status)" size="small" style="margin-left: 8px">
          {{ defect.statusDesc || defect.status }}
        </el-tag>
      </template>
      <template #extra>
        <el-button v-if="userStore.canWrite" :icon="EditPen" @click="router.push(`/defects/${defect.id}/edit`)">编辑</el-button>
        <el-button v-if="userStore.canWrite" :icon="Collection" :loading="precipitating" @click="onPrecipitate">沉淀为知识</el-button>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-header">
              <span>缺陷信息</span>
              <div v-if="userStore.canWrite" class="action-group">
                <el-button
                  v-for="action in actions"
                  :key="action.action"
                  :type="action.type || 'primary'"
                  size="small"
                  @click="openTransition(action)"
                >
                  {{ action.label }}
                </el-button>
                <span v-if="!actions.length" class="text-muted">当前状态无可执行流转</span>
              </div>
            </div>
          </template>

          <h3 class="defect-title">{{ defect.title }}</h3>

          <el-descriptions :column="2" border class="mb-16">
            <el-descriptions-item label="所属项目">{{ orDash(projectName) }}</el-descriptions-item>
            <el-descriptions-item label="所属模块">{{ orDash(defect.module) }}</el-descriptions-item>
            <el-descriptions-item label="缺陷类型">{{ defect.typeDesc || orDash(defect.type) }}</el-descriptions-item>
            <el-descriptions-item label="运行环境">{{ orDash(defect.environment) }}</el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="dictTag(DEFECT_PRIORITY, defect.priority)" size="small">
                {{ defect.priorityDesc || defect.priority }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="严重程度">
              <el-tag :type="dictTag(DEFECT_SEVERITY, defect.severity)" size="small" effect="plain">
                {{ defect.severityDesc || defect.severity }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="提交人">{{ orDash(defect.reporterName) }}</el-descriptions-item>
            <el-descriptions-item label="当前处理人">{{ orDash(defect.assigneeName) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ orDash(defect.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="重新打开次数">{{ defect.reopenCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="解决时间">{{ orDash(defect.resolvedTime) }}</el-descriptions-item>
            <el-descriptions-item label="关闭时间">{{ orDash(defect.closedTime) }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-item">
            <div class="detail-label">缺陷描述</div>
            <div class="detail-value pre-wrap">{{ orDash(defect.description) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">复现步骤</div>
            <div class="detail-value pre-wrap">{{ orDash(defect.reproduceSteps) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">预期结果</div>
            <div class="detail-value pre-wrap">{{ orDash(defect.expectedResult) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">实际结果</div>
            <div class="detail-value pre-wrap">{{ orDash(defect.actualResult) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">根因分类</div>
            <div class="detail-value">{{ orDash(defect.rootCause) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">解决方案</div>
            <div class="detail-value pre-wrap">{{ orDash(defect.solution) }}</div>
          </div>
        </el-card>

        <!-- 评论 -->
        <el-card shadow="never" class="page-card">
          <template #header><span>评论（{{ comments.length }}）</span></template>

          <div v-if="userStore.canWrite" class="comment-editor">
            <el-input v-model="commentContent" type="textarea" :rows="3" placeholder="输入评论内容…" maxlength="500" show-word-limit />
            <div class="comment-actions">
              <el-button type="primary" size="small" :loading="commentSubmitting" @click="submitComment">发表评论</el-button>
            </div>
          </div>
          <el-alert v-else type="info" :closable="false" class="mb-16" title="访客角色仅可查看，不能发表评论" />

          <el-empty v-if="!comments.length" description="暂无评论" :image-size="70" />
          <div v-for="item in comments" :key="item.id" class="comment-item">
            <div class="comment-head">
              <span class="comment-user">{{ item.nickname || item.username }}</span>
              <span class="text-muted">{{ item.createTime }}</span>
            </div>
            <div class="pre-wrap">{{ item.content }}</div>
          </div>
        </el-card>

        <!-- 附件 -->
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-header">
              <span>附件（{{ attachments.length }}）</span>
              <el-upload
                v-if="userStore.canWrite"
                :show-file-list="false"
                :http-request="customUpload"
                :loading="uploading"
              >
                <el-button size="small" :icon="Upload" :loading="uploading">上传附件</el-button>
              </el-upload>
            </div>
          </template>

          <el-table :data="attachments" border stripe empty-text="暂无附件">
            <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
            <el-table-column label="大小" width="110" align="center">
              <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column prop="uploaderName" label="上传人" width="120" show-overflow-tooltip />
            <el-table-column prop="createTime" label="上传时间" width="170" />
            <el-table-column label="操作" width="130" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="download(row)">下载</el-button>
                <el-button v-if="userStore.canWrite" link type="danger" @click="removeAttachment(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 操作日志 -->
        <el-card shadow="never">
          <template #header><span>操作日志</span></template>
          <el-timeline v-if="logs.length">
            <el-timeline-item
              v-for="log in logs"
              :key="log.id"
              :timestamp="log.createTime"
              placement="top"
            >
              <div>
                <strong>{{ log.operatorName }}</strong>
                <span class="text-muted"> 执行了 </span>
                <el-tag size="small" effect="plain">{{ log.actionDesc || log.action }}</el-tag>
                <span v-if="log.fromStatus" class="text-muted">
                  ：{{ log.fromStatusDesc || log.fromStatus }} → {{ log.toStatusDesc || log.toStatus }}
                </span>
              </div>
              <div v-if="log.remark" class="text-muted">{{ log.remark }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无操作日志" :image-size="70" />
        </el-card>
      </el-col>

      <!-- 右侧：AI 推荐 -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-header">
              <span>相似历史缺陷推荐</span>
              <el-button size="small" :icon="MagicStick" :loading="aiLoading" @click="onAiRecommend">AI 检索</el-button>
            </div>
          </template>

          <div v-if="recommend" class="mb-8">
            <el-tag size="small" type="info">检索方式：{{ retrievalModeText }}</el-tag>
            <el-tag
              v-if="recommend.source"
              size="small"
              :type="recommend.source === 'LLM' ? 'success' : recommend.source === 'RULE' ? 'warning' : 'info'"
              style="margin-left: 4px"
            >
              {{ recommend.source === 'LLM' ? '大模型' : recommend.source === 'RULE' ? '本地规则' : '无候选' }}
            </el-tag>
          </div>

          <el-empty v-if="!recommend" description="点击「AI 检索」获取推荐" :image-size="70" />
          <el-empty
            v-else-if="!recommend.recommendations.length"
            description="知识库中暂无相似缺陷"
            :image-size="70"
          />

          <div v-for="(item, index) in (recommend ? recommend.recommendations : [])" :key="item.knowledgeId" class="recommend-item">
            <div class="recommend-title">
              <span>
                <el-tag size="small" type="primary" effect="plain">{{ index + 1 }}</el-tag>
                <el-link type="primary" style="margin-left: 6px" @click="router.push(`/knowledge/${item.knowledgeId}`)">
                  {{ item.title }}
                </el-link>
              </span>
              <el-tag v-if="item.score !== null && item.score !== undefined" size="small" type="info">{{ item.score }}</el-tag>
            </div>
            <div class="recommend-row"><span class="text-muted">根因：</span>{{ orDash(item.rootCause) }}</div>
            <div class="recommend-row"><span class="text-muted">方案：</span>{{ orDash(item.solution) }}</div>
            <div class="recommend-row text-muted">推荐理由：{{ item.reason }}</div>
            <div v-if="item.tagList && item.tagList.length" class="mt-16">
              <el-tag v-for="tag in item.tagList" :key="tag" size="small" effect="plain" class="mr-4">{{ tag }}</el-tag>
            </div>
          </div>

          <template v-if="recommend && recommend.summary">
            <el-divider />
            <div class="mb-8"><strong>AI 综合建议</strong></div>
            <div class="pre-wrap text-muted">{{ recommend.summary }}</div>
            <div v-if="recommend.rootCauseGuess" class="mt-16">
              <div class="mb-8"><strong>根因推测</strong></div>
              <div class="pre-wrap text-muted">{{ recommend.rootCauseGuess }}</div>
            </div>
            <div v-if="recommend.suggestedSolution" class="mt-16">
              <div class="mb-8"><strong>建议解决方案</strong></div>
              <div class="pre-wrap text-muted">{{ recommend.suggestedSolution }}</div>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <!-- 流转弹窗 -->
    <el-dialog v-model="transitionDialog.visible" :title="`缺陷流转：${transitionDialog.label}`" width="520px" destroy-on-close>
      <el-form ref="transitionFormRef" :model="transitionForm" :rules="transitionRules" label-width="90px">
        <el-form-item v-if="transitionDialog.needAssignee" label="处理人" prop="assigneeId">
          <UserSelect v-model="transitionForm.assigneeId" placeholder="请选择处理人" />
        </el-form-item>
        <el-form-item v-if="transitionDialog.needSolution" label="解决方案" prop="solution">
          <el-input v-model="transitionForm.solution" type="textarea" :rows="4" placeholder="请填写解决方案，标记解决时必填" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="transitionForm.remark" type="textarea" :rows="2" placeholder="选填，会记录到操作日志" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transitionDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="transitionSubmitting" @click="submitTransition">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, EditPen, MagicStick, Upload } from '@element-plus/icons-vue'
import {
  getDefect,
  listComments,
  addComment,
  listLogs,
  transitionDefect,
  listAttachments,
  uploadAttachment,
  deleteAttachment,
  downloadAttachment
} from '@/api/defect'
import { getProject } from '@/api/project'
import { precipitateFromDefect } from '@/api/knowledge'
import { aiRecommend } from '@/api/ai'
import UserSelect from '@/components/UserSelect.vue'
import { useUserStore } from '@/stores/user'
import { DEFECT_STATUS, DEFECT_PRIORITY, DEFECT_SEVERITY, dictTag, availableActions } from '@/constants/enums'
import { formatFileSize, orDash, parseFileName } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defectId = Number(route.params.id)

const loading = ref(false)
const defect = ref({})
const projectName = ref('')
const comments = ref([])
const logs = ref([])
const attachments = ref([])

const actions = computed(() => availableActions(defect.value.status))

// ---- 评论 ----
const commentContent = ref('')
const commentSubmitting = ref(false)

async function submitComment() {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await addComment(defectId, { content: commentContent.value.trim() })
    commentContent.value = ''
    ElMessage.success('评论成功')
    comments.value = await listComments(defectId)
  } finally {
    commentSubmitting.value = false
  }
}

// ---- 状态流转 ----
const transitionFormRef = ref()
const transitionSubmitting = ref(false)
const transitionDialog = reactive({ visible: false, label: '', action: '', needAssignee: false, needSolution: false })
const transitionForm = reactive({ assigneeId: null, solution: '', remark: '' })
const transitionRules = computed(() => ({
  assigneeId: transitionDialog.needAssignee
    ? [{ required: true, message: '请选择处理人', trigger: 'change' }]
    : [],
  solution: transitionDialog.needSolution
    ? [{ required: true, message: '请填写解决方案', trigger: 'blur' }]
    : []
}))

function openTransition(action) {
  transitionDialog.label = action.label
  transitionDialog.action = action.action
  transitionDialog.needAssignee = !!action.needAssignee
  transitionDialog.needSolution = !!action.needSolution
  transitionForm.assigneeId = null
  transitionForm.solution = defect.value.solution || ''
  transitionForm.remark = ''
  transitionDialog.visible = true
}

async function submitTransition() {
  try {
    await transitionFormRef.value.validate()
  } catch (e) {
    return
  }
  transitionSubmitting.value = true
  try {
    await transitionDefect(defectId, {
      action: transitionDialog.action,
      assigneeId: transitionForm.assigneeId || undefined,
      solution: transitionForm.solution || undefined,
      remark: transitionForm.remark || undefined
    })
    ElMessage.success('流转成功')
    transitionDialog.visible = false
    await loadDetail()
  } finally {
    transitionSubmitting.value = false
  }
}

// ---- 附件 ----
const uploading = ref(false)

async function customUpload(options) {
  uploading.value = true
  try {
    await uploadAttachment(defectId, options.file)
    ElMessage.success('上传成功')
    attachments.value = await listAttachments(defectId)
  } finally {
    uploading.value = false
  }
}

async function download(row) {
  const response = await downloadAttachment(row.id)
  const blob = new Blob([response.data], { type: row.contentType || 'application/octet-stream' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = parseFileName(response.headers['content-disposition'], row.fileName)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

async function removeAttachment(row) {
  try {
    await ElMessageBox.confirm(`确定删除附件「${row.fileName}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteAttachment(row.id)
  ElMessage.success('删除成功')
  attachments.value = await listAttachments(defectId)
}

// ---- 沉淀为知识 ----
const precipitating = ref(false)

async function onPrecipitate() {
  try {
    await ElMessageBox.confirm('将基于本缺陷的类型、根因与解决方案生成一条知识库条目，确定继续？', '沉淀为知识', {
      type: 'info'
    })
  } catch (e) {
    return
  }
  precipitating.value = true
  try {
    const knowledge = await precipitateFromDefect(defectId, {})
    ElMessage.success('沉淀成功')
    router.push(`/knowledge/${knowledge.id}`)
  } finally {
    precipitating.value = false
  }
}

// ---- AI 推荐 ----
const aiLoading = ref(false)
const recommend = ref(null)

const retrievalModeText = computed(() => {
  const map = { VECTOR: '向量检索', FULLTEXT: '全文索引', KEYWORD: '关键词匹配', NONE: '无' }
  return map[recommend.value?.retrievalMode] || recommend.value?.retrievalMode || '-'
})

async function onAiRecommend() {
  aiLoading.value = true
  try {
    recommend.value = await aiRecommend({ defectId })
  } finally {
    aiLoading.value = false
  }
}

// ---- 加载 ----
async function loadDetail() {
  loading.value = true
  try {
    const data = await getDefect(defectId)
    defect.value = data || {}
    // 项目名单独获取（缺陷详情里只有 projectId）
    if (data.projectId) {
      try {
        const project = await getProject(data.projectId)
        projectName.value = project.name
      } catch (e) {
        projectName.value = ''
      }
    }
    const [commentData, logData, attachmentData] = await Promise.all([
      listComments(defectId),
      listLogs(defectId),
      listAttachments(defectId)
    ])
    comments.value = commentData || []
    logs.value = logData || []
    attachments.value = attachmentData || []
  } finally {
    loading.value = false
  }
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
  gap: 12px;
  flex-wrap: wrap;
}

.action-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.defect-title {
  margin: 0 0 16px;
  font-size: 18px;
}

.comment-editor {
  margin-bottom: 16px;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.comment-item {
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 13px;
}

.comment-user {
  font-weight: 600;
  color: #303133;
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

.mr-4 {
  margin-right: 4px;
}
</style>
