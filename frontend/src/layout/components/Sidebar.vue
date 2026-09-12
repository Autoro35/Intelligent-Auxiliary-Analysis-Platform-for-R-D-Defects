<template>
  <div class="sidebar">
    <div class="sidebar-logo" :title="appTitle">
      <el-icon :size="22" color="#409eff"><Monitor /></el-icon>
      <span v-show="!collapsed" class="sidebar-logo-text">缺陷管理平台</span>
    </div>

    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      :collapse-transition="false"
      background-color="#001529"
      text-color="rgba(255,255,255,0.75)"
      active-text-color="#fff"
      router
    >
      <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { layoutChildren } from '@/router/routes'

defineProps({
  collapsed: { type: Boolean, default: false }
})

const route = useRoute()
const userStore = useUserStore()
const appTitle = import.meta.env.VITE_APP_TITLE || '研发缺陷管理平台'

/**
 * 由路由表推导侧边栏菜单：过滤 hidden 路由，并按当前用户角色过滤 roles
 */
const menus = computed(() =>
  layoutChildren
    .filter((item) => !item.meta?.hidden)
    .filter((item) => {
      const roles = item.meta?.roles
      // 未声明 roles 表示所有登录用户可见
      return !roles || !roles.length || roles.includes(userStore.role)
    })
    .map((item) => ({
      path: `/${item.path}`,
      title: item.meta?.title || item.name,
      icon: item.meta?.icon || 'Menu'
    }))
)

/** 详情页等隐藏路由通过 meta.activeMenu 保持父菜单高亮 */
const activeMenu = computed(() => route.meta?.activeMenu || route.path)
</script>

<style scoped>
.sidebar {
  height: 100%;
  overflow-y: auto;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 60px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  background-color: #002140;
}

.sidebar-logo-text {
  overflow: hidden;
}

.sidebar :deep(.el-menu) {
  border-right: none;
}

.sidebar :deep(.el-menu-item.is-active) {
  background-color: #1890ff !important;
}
</style>
