import request from './request'

/**
 * 自动分类与优先级判定
 * @param {{defectId?:Number, title?:String, description?:String, module?:String}} data
 */
export function aiClassify(data) {
  return request.post('/ai/classify', data)
}

/**
 * RAG 根因/方案推荐 Top3
 * @param {{defectId?:Number, title?:String, description?:String, type?:String}} data
 */
export function aiRecommend(data) {
  return request.post('/ai/recommend', data)
}

/**
 * 缺陷描述补全
 * @param {{defectId?:Number, title?:String, description?:String, type?:String, module?:String, environment?:String}} data
 */
export function aiCompleteDescription(data) {
  return request.post('/ai/complete-description', data)
}

/** AI 能力状态自检（仅管理员） */
export function aiStatus() {
  return request.get('/ai/status')
}

/** 全量重建向量索引（仅管理员） */
export function aiRebuildIndex() {
  return request.post('/ai/index/rebuild')
}
