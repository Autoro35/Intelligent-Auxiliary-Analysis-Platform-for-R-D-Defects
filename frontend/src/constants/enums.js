/**
 * 全局枚举字典
 * 所有 code 与后端 com.defect.platform.common.constant 下的枚举严格一致，禁止前端自造值
 */

/** 缺陷类型 */
export const DEFECT_TYPE = [
  { code: 'FUNCTIONAL', label: '功能Bug' },
  { code: 'PERFORMANCE', label: '性能问题' },
  { code: 'UI', label: 'UI异常' },
  { code: 'COMPATIBILITY', label: '兼容性问题' },
  { code: 'OPTIMIZATION', label: '建议优化' }
]

/** 缺陷优先级 */
export const DEFECT_PRIORITY = [
  { code: 'URGENT', label: '紧急', tag: 'danger' },
  { code: 'HIGH', label: '高', tag: 'warning' },
  { code: 'MEDIUM', label: '中', tag: 'primary' },
  { code: 'LOW', label: '低', tag: 'info' }
]

/** 缺陷严重程度 */
export const DEFECT_SEVERITY = [
  { code: 'BLOCKER', label: '致命', tag: 'danger' },
  { code: 'CRITICAL', label: '严重', tag: 'warning' },
  { code: 'MAJOR', label: '一般', tag: 'primary' },
  { code: 'MINOR', label: '轻微', tag: 'info' }
]

/** 缺陷状态 */
export const DEFECT_STATUS = [
  { code: 'NEW', label: '新建', tag: 'info' },
  { code: 'ASSIGNED', label: '已分配', tag: 'primary' },
  { code: 'PROCESSING', label: '处理中', tag: 'warning' },
  { code: 'PENDING_RETEST', label: '待复测', tag: 'warning' },
  { code: 'CLOSED', label: '已关闭', tag: 'success' },
  { code: 'REJECTED', label: '已驳回', tag: 'danger' }
]

/** 系统全局角色 */
export const ROLE = [
  { code: 'ADMIN', label: '管理员' },
  { code: 'TESTER', label: '测试' },
  { code: 'DEVELOPER', label: '开发' },
  { code: 'GUEST', label: '访客' }
]

/** 项目内角色 */
export const PROJECT_ROLE = [
  { code: 'OWNER', label: '负责人' },
  { code: 'DEV', label: '开发' },
  { code: 'TESTER', label: '测试' },
  { code: 'VIEWER', label: '访客' }
]

/**
 * 缺陷状态机：与后端 DefectServiceImpl.applyTransition 严格对应
 * 键为当前状态，值为该状态下允许执行的动作
 */
export const DEFECT_TRANSITIONS = {
  NEW: [{ action: 'ASSIGN', label: '分配处理人', needAssignee: true, type: 'primary' }],
  ASSIGNED: [{ action: 'START', label: '开始处理', type: 'primary' }],
  PROCESSING: [{ action: 'RESOLVE', label: '标记解决', needSolution: true, type: 'primary' }],
  PENDING_RETEST: [
    { action: 'CLOSE', label: '关闭缺陷', type: 'success' },
    { action: 'REJECT', label: '驳回缺陷', type: 'danger' }
  ],
  REJECTED: [{ action: 'REOPEN', label: '重新打开', type: 'warning' }],
  CLOSED: []
}

/** 按 code 取字典项 */
export function dictItem(dict, code) {
  return dict.find((item) => item.code === code) || { code, label: code }
}

/** 按 code 取中文名（找不到时原样返回，与后端 descOf 行为一致） */
export function dictLabel(dict, code) {
  if (!code) return ''
  return dictItem(dict, code).label
}

/** 按 code 取标签色 */
export function dictTag(dict, code) {
  return dictItem(dict, code).tag || 'info'
}

/** 当前状态可执行的动作列表 */
export function availableActions(status) {
  return DEFECT_TRANSITIONS[status] || []
}
