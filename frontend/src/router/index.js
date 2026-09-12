import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { routes } from './routes'

const router = createRouter({
  // hash 模式部署时无需服务端做 SPA 回退配置
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, from, next) => {
  const title = to.meta && to.meta.title
  document.title = title ? `${title} - 研发缺陷管理平台` : '研发缺陷管理平台'

  const hasToken = !!getToken()

  if (to.path === '/login') {
    // 已登录再访问登录页，直接回首页
    return hasToken ? next('/dashboard') : next()
  }
  if (!hasToken) {
    return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }

  // 角色校验：不在允许列表中的角色拦回看板
  const roles = to.meta && to.meta.roles
  if (roles && roles.length) {
    const userStore = useUserStore()
    if (!roles.includes(userStore.role)) {
      return next('/dashboard')
    }
  }
  return next()
})

export default router
