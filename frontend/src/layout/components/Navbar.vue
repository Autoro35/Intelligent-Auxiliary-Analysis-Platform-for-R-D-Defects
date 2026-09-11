<template>
  <div class="navbar">
    <div class="navbar-left">
      <el-icon class="collapse-btn" :size="20" @click="toggle">
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="navbar-right">
      <el-tag :type="roleTagType" size="small" effect="plain">{{ userStore.role || '-' }}</el-tag>
      <el-dropdown @command="onCommand">
        <span class="user-trigger">
          <el-avatar :size="28" class="user-avatar">{{ avatarText }}</el-avatar>
          <span class="user-name">{{ userStore.nickname }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  collapsed: { type: Boolean, default: false }
})
const emit = defineEmits(['update:collapsed'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentTitle = computed(() => route.meta?.title || '')
const avatarText = computed(() => (userStore.nickname || 'U').slice(0, 1))

const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', TESTER: 'warning', DEVELOPER: 'primary', GUEST: 'info' }
  return map[userStore.role] || 'info'
})

function toggle() {
  emit('update:collapsed', !props.collapsed)
}

async function onCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    } catch (e) {
      return
    }
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding-left: 16px;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #5a5e66;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background-color: #409eff;
  color: #fff;
}

.user-name {
  font-size: 14px;
  color: #303133;
}
</style>
