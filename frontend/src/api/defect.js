import request from './request'

/** 缺陷分页列表（支持 projectId/status/type/priority/assigneeId/keyword） */
export function listDefects(params) {
  return request.get('/defects', { params })
}

/** 缺陷详情 */
export function getDefect(id) {
  return request.get(`/defects/${id}`)
}

/** 新建缺陷 */
export function createDefect(data) {
  return request.post('/defects', data)
}

/** 编辑缺陷 */
export function updateDefect(id, data) {
  return request.put(`/defects/${id}`, data)
}

/** 删除缺陷 */
export function deleteDefect(id) {
  return request.delete(`/defects/${id}`)
}

/** 状态流转（action: ASSIGN/START/RESOLVE/CLOSE/REJECT/REOPEN） */
export function transitionDefect(id, data) {
  return request.post(`/defects/${id}/transition`, data)
}

/** 评论列表 */
export function listComments(id) {
  return request.get(`/defects/${id}/comments`)
}

/** 新增评论 */
export function addComment(id, data) {
  return request.post(`/defects/${id}/comments`, data)
}

/** 操作日志 */
export function listLogs(id) {
  return request.get(`/defects/${id}/logs`)
}

/** 附件列表 */
export function listAttachments(defectId) {
  return request.get(`/defects/${defectId}/attachments`)
}

/** 上传附件 */
export function uploadAttachment(defectId, file) {
  const form = new FormData()
  form.append('file', file)
  return request.post(`/defects/${defectId}/attachments`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 删除附件 */
export function deleteAttachment(id) {
  return request.delete(`/attachments/${id}`)
}

/** 下载附件（返回原始 response，便于读取响应头文件名） */
export function downloadAttachment(id) {
  return request.get(`/attachments/${id}/download`, { responseType: 'blob' })
}
