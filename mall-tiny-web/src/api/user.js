import request from './request'

// 获取所有用户（分页，取全部）
export function getUserList(keyword) {
  return request({
    url: '/admin/list',
    method: 'get',
    params: { keyword, pageSize: 100, pageNum: 1 }
  })
}

// 获取用户的角色列表
export function getAdminRoles(adminId) {
  return request({
    url: `/admin/role/${adminId}`,
    method: 'get'
  })
}
