/**
 * 登录态本地存储
 * token 与用户信息存 localStorage，刷新页面后仍保持登录
 */
const TOKEN_KEY = 'defect_platform_token'
const USER_KEY = 'defect_platform_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    // 本地数据被破坏时直接丢弃，避免卡死登录流程
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
