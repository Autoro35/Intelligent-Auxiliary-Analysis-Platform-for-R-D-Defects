<template>
  <div ref="chartRef" :style="{ width: '100%', height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * ECharts 通用容器
 * 只负责实例生命周期与尺寸自适应，图表内容完全由外部传入的 option 决定
 */
const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '300px' }
})

const chartRef = ref()
let chart = null

function render() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  // notMerge=true，避免切换数据维度时残留上一次的系列
  chart.setOption(props.option, true)
}

function handleResize() {
  if (chart) chart.resize()
}

onMounted(async () => {
  await nextTick()
  render()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(() => props.option, render, { deep: true })
</script>
