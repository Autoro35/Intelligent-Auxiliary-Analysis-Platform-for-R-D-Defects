import { defineStore } from 'pinia'
import { login as loginApi, getCurrentUser } from '@/api/auth'
import { getToken, setToken, getUser, setUser, clearAuth } from '@/utils/auth'

/** 可执行写操作的角色（访客只读），与后端 @RequireRole 保持一致 */
const WRITE_ROLES = ['ADMIN', 'TESTER', 'DEVELOPER']

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: getUser()
  }),

  getters: {
    isLogin: (state) => !!state.token,
    role: (state) => (state.userInfo && state.userInfo.role) || '',
    nickname: (state) => {
      if (!state.userInfo) return ''
      return state.userInfo.nickname || state.userInfo.username || ''
    },
    isAdmin() {
      return this.role === 'ADMIN'
    },
    /** 是否可执行写操作（新建/编辑/删除/流转） */
    canWrite() {
      return WRITE_ROLES.includes(this.role)
    }
  },

  actions: {
    /** 登录并落地本地存储 */
    async login(form) {
      const data = await loginApi(form)
      this.token = data.token
      this.userInfo = data.user
      setToken(data.token)
      setUser(data.user)
      return data
    },

    /** 用后端数据刷新当前用户信息 */
    async fetchUser() {
      const user = await getCurrentUser()
      this.userInfo = user
      setUser(user)
      return user
    },

    logout() {
      this.token = ''
      this.userInfo = null
      clearAuth()
    }
  }
})
