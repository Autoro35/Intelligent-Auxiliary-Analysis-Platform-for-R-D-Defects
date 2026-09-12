import request from './request'

/** 知识库分页列表（支持 keyword/type/tag） */
export function listKnowledge(params) {
  return request.get('/knowledge', { params })
}

/** 知识详情（会累加浏览次数） */
export function getKnowledge(id) {
  return request.get(`/knowledge/${id}`)
}

/** 新建知识 */
export function createKnowledge(data) {
  return request.post('/knowledge', data)
}

/** 编辑知识 */
export function updateKnowledge(id, data) {
  return request.put(`/knowledge/${id}`, data)
}

/** 删除知识 */
export function deleteKnowledge(id) {
  return request.delete(`/knowledge/${id}`)
}

/** 标签云聚合 */
export function listTags() {
  return request.get('/knowledge/tags')
}

/** 从缺陷一键沉淀为知识 */
export function precipitateFromDefect(defectId, data) {
  return request.post(`/knowledge/from-defect/${defectId}`, data || {})
}
