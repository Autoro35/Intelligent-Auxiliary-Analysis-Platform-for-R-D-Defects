import request from './request'

/** 项目分页列表 */
export function listProjects(params) {
  return request.get('/projects', { params })
}

/** 项目详情 */
export function getProject(id) {
  return request.get(`/projects/${id}`)
}

/** 新建项目 */
export function createProject(data) {
  return request.post('/projects', data)
}

/** 编辑项目 */
export function updateProject(id, data) {
  return request.put(`/projects/${id}`, data)
}

/** 删除项目 */
export function deleteProject(id) {
  return request.delete(`/projects/${id}`)
}

/** 项目成员列表 */
export function listMembers(projectId) {
  return request.get(`/projects/${projectId}/members`)
}

/** 添加项目成员 */
export function addMember(projectId, data) {
  return request.post(`/projects/${projectId}/members`, data)
}

/** 移除项目成员 */
export function removeMember(projectId, userId) {
  return request.delete(`/projects/${projectId}/members/${userId}`)
}
