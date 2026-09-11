<template>
  <el-select
    v-if="canListUsers"
    :model-value="modelValue"
    :placeholder="placeholder"
    :style="{ width }"
    clearable
    filterable
    :loading="loading"
    @update:model-value="onChange"
  >
    <el-option v-for="u in options" :key="u.id" :label="userLabel(u)" :value="u.id" />
  </el-select>

  <!-- 用户列表接口仅管理员可访问，其他角色退化为手工填写用户 ID -->
  <el-input-number
    v-else
    :model-value="modelValue"
    :placeholder="placeholder"
    :style="{ width }"
    :min="1"
    :controls="false"
    @update:model-value="onChange"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { listUsers } from '@/api/user'
import { useUserStore } from '@/stores/user'

/**
 * 用户选择器
 * 管理员走下拉选择；非管理员因无用户列表权限，退化为填写用户 ID
 */
defineProps({
  modelValue: { type: Number, default: null },
  placeholder: { type: String, default: '请选择用户' },
  width: { type: String, default: '100%' }
})
const emit = defineEmits(['update:modelValue'])

const userStore = useUserStore()
const canListUsers = computed(() => userStore.isAdmin)

// 模块级缓存，多处使用时只请求一次
const cachedOptions = ref([])
const loading = ref(false)

const options = computed(() => cachedOptions.value)

function userLabel(user) {
  return `${user.nickname || user.username}（${user.username}）`
}

function onChange(value) {
  emit('update:modelValue', value ?? null)
}

async function loadUsers() {
  if (!canListUsers.value || cachedOptions.value.length) return
  loading.value = true
  try {
    const data = await listUsers({ current: 1, size: 200 })
    cachedOptions.value = data.records || []
  } finally {
    loading.value = false
  }
}

onMounted(loadUsers)
</script>
