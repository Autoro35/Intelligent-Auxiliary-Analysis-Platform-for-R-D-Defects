import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth'

/**
 * axios 统一封装
 * 后端所有接口都返回 Result{ code, message, data }，此处统一拆包：
 * - code === 200 时直接把 data 交给业务代码
 * - 其余情况弹提示并 reject，业务代码只需处理成功分支
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  // AI 接口会调用大模型，耗时明显高于普通接口
  timeout: 60000
})

// 请求拦截：注入 JWT
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一拆包与错误提示
service.interceptors.response.use(
  (response) => {
    // 文件下载等二进制响应，直接把整个 response 交给调用方
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      redirectToLogin()
      return Promise.reject(new Error(res.message || '登录已过期'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      redirectToLogin()
      return Promise.reject(error)
    }
    const message =
      error.code === 'ECONNABORTED'
        ? '请求超时，请稍后重试'
        : `网络异常：${error.message}`
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/** 登录态失效：清空本地并跳登录页（携带回跳地址） */
function redirectToLogin() {
  clearAuth()
  ElMessage.error('登录已过期，请重新登录')
  const current = window.location.hash.replace(/^#/, '')
  if (!current.startsWith('/login')) {
    window.location.hash = `/login?redirect=${encodeURIComponent(current || '/')}`
  }
}

export default service
