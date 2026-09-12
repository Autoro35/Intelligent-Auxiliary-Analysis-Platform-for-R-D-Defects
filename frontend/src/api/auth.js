import request from './request'

/** 登录，返回 { token, user } */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 注册 */
export function register(data) {
  return request.post('/auth/register', data)
}

/** 获取当前登录用户信息 */
export function getCurrentUser() {
  return request.get('/auth/me')
}
