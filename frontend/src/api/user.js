import request from './request'

/** 用户分页列表（仅管理员），支持 keyword/role/status 过滤 */
export function listUsers(params) {
  return request.get('/users', { params })
}

/** 管理员直接建号（可同时指定角色，仅管理员） */
export function createUser(data) {
  return request.post('/users', data)
}

/** 更新用户信息（昵称/邮箱/角色/状态，仅管理员） */
export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

/** 重置用户密码（仅管理员） */
export function resetUserPassword(id, password) {
  return request.put(`/users/${id}/password`, { password })
}

/** 删除用户（仅管理员，逻辑删除） */
export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}
