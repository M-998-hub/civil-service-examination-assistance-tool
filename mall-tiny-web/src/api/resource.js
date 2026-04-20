import request from './request'

// 获取资源树（按分类分组）
export function getResourceTree() {
  return request({
    url: '/resource/tree',
    method: 'get'
  })
}
