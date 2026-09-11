<template>
  <div class="page-container" v-loading="loading">
    <!-- 总览指标 -->
    <el-row :gutter="16" class="page-card">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="8" :md="6" :lg="3">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>新增 / 关闭趋势</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
            <el-radio-button :value="90">近 90 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <BaseChart :option="trendOption" height="320px" />
    </el-card>

    <el-row :gutter="16" class="page-card">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span>状态分布</span></template>
          <BaseChart :option="pieOption(distribution.status, '状态')" height="300px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span>类型分布</span></template>
          <BaseChart :option="pieOption(distribution.type, '类型')" height="300px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="page-card">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span>优先级分布</span></template>
          <BaseChart :option="barOption(distribution.priority, '优先级')" height="300px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span>严重程度分布</span></template>
          <BaseChart :option="barOption(distribution.severity, '严重程度')" height="300px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header><span>成员工作量</span></template>
      <el-table :data="workload" border stripe empty-text="暂无数据">
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="name" label="成员" min-width="140" show-overflow-tooltip />
        <el-table-column prop="total" label="负责总数" width="100" align="center" sortable />
        <el-table-column prop="processing" label="处理中" width="90" align="center" />
        <el-table-column prop="pendingRetest" label="待复测" width="90" align="center" />
        <el-table-column prop="closed" label="已关闭" width="90" align="center" />
        <el-table-column prop="resolved" label="已解决" width="90" align="center" />
        <el-table-column prop="reopenCount" label="重开次数" width="100" align="center" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import BaseChart from '@/components/BaseChart.vue'
import { getOverview, getDistribution, getTrend, getWorkload } from '@/api/stats'

const loading = ref(false)
const trendDays = ref(30)

const overview = ref({})
const distribution = ref({ status: [], type: [], priority: [], severity: [] })
const trend = ref([])
const workload = ref([])

/** 图表配色：各维度复用同一组颜色，保证同一编码含义全局一致 */
const PIE_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9b59b6', '#1abc9c']

const statCards = computed(() => [
  { key: 'total', label: '缺陷总数', value: overview.value.total ?? 0, color: '#409eff' },
  { key: 'open', label: '未关闭', value: overview.value.open ?? 0, color: '#e6a23c' },
  { key: 'processing', label: '处理中', value: overview.value.processing ?? 0, color: '#f56c6c' },
  { key: 'pendingRetest', label: '待复测', value: overview.value.pendingRetest ?? 0, color: '#9b59b6' },
  { key: 'closed', label: '已关闭', value: overview.value.closed ?? 0, color: '#67c23a' },
  { key: 'rejected', label: '已驳回', value: overview.value.rejected ?? 0, color: '#909399' },
  { key: 'completionRate', label: '关闭率(%)', value: overview.value.completionRate ?? 0, color: '#1abc9c' },
  { key: 'knowledgeCount', label: '知识库条目', value: overview.value.knowledgeCount ?? 0, color: '#606266' }
])

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['新增', '关闭'], right: 0 },
  grid: { left: 40, right: 20, top: 40, bottom: 30 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => item.date)
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      name: '新增',
      type: 'line',
      smooth: true,
      data: trend.value.map((item) => item.created),
      itemStyle: { color: '#409eff' },
      areaStyle: { opacity: 0.12 }
    },
    {
      name: '关闭',
      type: 'line',
      smooth: true,
      data: trend.value.map((item) => item.closed),
      itemStyle: { color: '#67c23a' },
      areaStyle: { opacity: 0.12 }
    }
  ]
}))

function pieOption(items, name) {
  const list = items || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    color: PIE_COLORS,
    series: [
      {
        name,
        type: 'pie',
        radius: ['42%', '68%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}\n{c}' },
        data: list.map((item) => ({ name: item.desc || item.code, value: item.count }))
      }
    ]
  }
}

function barOption(items, name) {
  const list = items || []
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 20, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: list.map((item) => item.desc || item.code),
      axisLabel: { interval: 0, rotate: list.length > 4 ? 20 : 0 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name,
        type: 'bar',
        barMaxWidth: 48,
        data: list.map((item) => item.count),
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top' }
      }
    ]
  }
}

async function loadTrend() {
  trend.value = await getTrend(trendDays.value)
}

async function loadAll() {
  loading.value = true
  try {
    const [overviewData, distributionData, workloadData] = await Promise.all([
      getOverview(),
      getDistribution(),
      getWorkload()
    ])
    overview.value = overviewData || {}
    distribution.value = distributionData || { status: [], type: [], priority: [], severity: [] }
    workload.value = workloadData || []
    await loadTrend()
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 16px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.4;
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
