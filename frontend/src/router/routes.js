/**
 * 路由表定义
 * 抽成独立模块，供 router（注册）与 Sidebar（生成菜单）共用，避免循环依赖
 *
 * meta.title     页面标题（顶栏面包屑、浏览器标题、侧边栏文字）
 * meta.icon      侧边栏图标（Element Plus 图标组件名）
 * meta.hidden    是否在侧边栏隐藏（详情页/编辑页等）
 * meta.activeMenu 隐藏页面在侧边栏保持高亮的菜单路径
 * meta.roles     允许访问的角色；不配置表示所有登录用户可访问
 */

/** 主框架下的业务页面 */
export const layoutChildren = [
  {
    path: 'dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '统计看板', icon: 'DataAnalysis' }
  },
  {
    path: 'projects',
    name: 'ProjectList',
    component: () => import('@/views/project/index.vue'),
    meta: { title: '项目管理', icon: 'Folder' }
  },
  {
    path: 'projects/:id',
    name: 'ProjectDetail',
    component: () => import('@/views/project/detail.vue'),
    meta: { title: '项目详情', hidden: true, activeMenu: '/projects' }
  },
  {
    path: 'defects',
    name: 'DefectList',
    component: () => import('@/views/defect/index.vue'),
    meta: { title: '缺陷管理', icon: 'Warning' }
  },
  {
    path: 'defects/create',
    name: 'DefectCreate',
    component: () => import('@/views/defect/form.vue'),
    meta: {
      title: '新建缺陷',
      hidden: true,
      activeMenu: '/defects',
      roles: ['ADMIN', 'TESTER', 'DEVELOPER']
    }
  },
  {
    path: 'defects/:id/edit',
    name: 'DefectEdit',
    component: () => import('@/views/defect/form.vue'),
    meta: {
      title: '编辑缺陷',
      hidden: true,
      activeMenu: '/defects',
      roles: ['ADMIN', 'TESTER', 'DEVELOPER']
    }
  },
  {
    path: 'defects/:id',
    name: 'DefectDetail',
    component: () => import('@/views/defect/detail.vue'),
    meta: { title: '缺陷详情', hidden: true, activeMenu: '/defects' }
  },
  {
    path: 'knowledge',
    name: 'KnowledgeList',
    component: () => import('@/views/knowledge/index.vue'),
    meta: { title: '知识库', icon: 'Collection' }
  },
  {
    path: 'knowledge/:id',
    name: 'KnowledgeDetail',
    component: () => import('@/views/knowledge/detail.vue'),
    meta: { title: '知识详情', hidden: true, activeMenu: '/knowledge' }
  },
  {
    path: 'users',
    name: 'UserList',
    component: () => import('@/views/user/index.vue'),
    meta: { title: '用户管理', icon: 'UserFilled', roles: ['ADMIN'] }
  },
  {
    path: 'ai-status',
    name: 'AiStatus',
    component: () => import('@/views/ai/status.vue'),
    meta: { title: 'AI 能力状态', icon: 'MagicStick', roles: ['ADMIN'] }
  },
  {
    path: 'profile',
    name: 'Profile',
    component: () => import('@/views/profile/index.vue'),
    meta: { title: '个人中心', hidden: true }
  }
]

/** 完整路由表 */
export const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: layoutChildren
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true }
  }
]
