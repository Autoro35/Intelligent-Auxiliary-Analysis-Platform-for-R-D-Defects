<template>
  <div class="page-container" v-loading="loading">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>AI 能力自检</span>
          <div>
            <el-button :icon="Refresh" :loading="loading" @click="load">重新检测</el-button>
            <el-button type="primary" :icon="RefreshRight" :loading="rebuilding" @click="onRebuild">重建向量索引</el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        class="mb-16"
        title="说明：未配置 DeepSeek API Key 或 Chroma 未启动时，AI 接口会自动降级，功能依然可用，响应中会标明数据来源。"
      />

      <el-descriptions :column="2" border>
        <el-descriptions-item label="DeepSeek 已配置">
          <el-tag :type="status.deepseekConfigured ? 'success' : 'info'" size="small">
            {{ status.deepseekConfigured ? '是' : '否（降级为本地规则引擎）' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="DeepSeek 连通性">
          <el-tag :type="status.deepseekReachable ? 'success' : 'danger'" size="small">
            {{ status.deepseekReachable ? '可用' : '不可用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="使用模型">{{ orDash(status.model) }}</el-descriptions-item>
        <el-descriptions-item label="当前检索方式">
          <el-tag :type="status.retrievalMode === 'VECTOR' ? 'success' : 'warning'" size="small">
            {{ status.retrievalMode === 'VECTOR' ? '向量检索（Chroma）' : '全文索引（MySQL，向量库不可用时降级）' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Chroma 连通性">
          <el-tag :type="status.chromaReachable ? 'success' : 'danger'" size="small">
            {{ status.chromaReachable ? '可用' : '不可用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="向量集合">{{ orDash(status.chromaCollection) }}</el-descriptions-item>
        <el-descriptions-item label="向量数量">
          {{ status.vectorCount === -1 ? '不可用' : (status.vectorCount ?? 0) }}
        </el-descriptions-item>
        <el-descriptions-item label="知识库条目">{{ status.knowledgeCount ?? 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header><span>接口权限说明</span></template>
      <el-table :data="apiDocs" border stripe>
        <el-table-column prop="method" label="方法" width="90" align="center" />
        <el-table-column prop="path" label="路径" min-width="220" />
        <el-table-column prop="desc" label="说明" min-width="240" />
        <el-table-column prop="roles" label="可用角色" width="220" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, RefreshRight } from '@element-plus/icons-vue'
import { aiStatus, aiRebuildIndex } from '@/api/ai'
import { orDash } from '@/utils/format'

const loading = ref(false)
const rebuilding = ref(false)
const status = ref({})

const apiDocs = [
  { method: 'POST', path: '/api/ai/classify', desc: '自动分类 + 优先级判定', roles: '管理员 / 测试 / 开发' },
  { method: 'POST', path: '/api/ai/recommend', desc: 'RAG 根因/方案推荐 Top3', roles: '所有登录用户（含访客）' },
  { method: 'POST', path: '/api/ai/complete-description', desc: '缺陷描述补全', roles: '管理员 / 测试 / 开发' },
  { method: 'GET', path: '/api/ai/status', desc: 'AI 能力状态自检', roles: '管理员' },
  { method: 'POST', path: '/api/ai/index/rebuild', desc: '全量重建向量索引', roles: '管理员' }
]

async function load() {
  loading.value = true
  try {
    status.value = await aiStatus()
  } finally {
    loading.value = false
  }
}

async function onRebuild() {
  rebuilding.value = true
  try {
    const count = await aiRebuildIndex()
    ElMessage.success(`向量索引重建完成，成功写入 ${count} 条`)
    await load()
  } finally {
    rebuilding.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
