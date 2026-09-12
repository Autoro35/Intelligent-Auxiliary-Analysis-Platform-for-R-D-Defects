import request from './request'

/** 用户分页列表（仅管理员） */
export function listUsers(params) {
  return request.get('/users', { params })
}
