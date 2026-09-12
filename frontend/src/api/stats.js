import request from './request'

/** 统计总览 */
export function getOverview() {
  return request.get('/stats/overview')
}

/** 分布统计（状态/类型/优先级/严重程度） */
export function getDistribution() {
  return request.get('/stats/distribution')
}

/** 新增/关闭趋势，默认近 30 天 */
export function getTrend(days = 30) {
  return request.get('/stats/trend', { params: { days } })
}

/** 成员工作量 */
export function getWorkload() {
  return request.get('/stats/workload')
}
