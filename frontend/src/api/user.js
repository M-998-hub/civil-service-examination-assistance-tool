import request from './request'

// 获取所有用户（分页，取全部）
export function getUserList(keyword, pageSize, pageNum) {
  return request({
    url: '/admin/list',
    method: 'get',
    params: { keyword, pageSize: pageSize || 10, pageNum: pageNum || 1 },
  })
}

// 更新用户信息
export function updateUser(id, data) {
  return request({
    url: `/admin/update/${id}`,
    method: 'post',
    data,
  })
}

// 更新用户状态
export function updateUserStatus(id, status) {
  return request({
    url: `/admin/updateStatus/${id}`,
    method: 'post',
    params: { status },
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: `/admin/delete/${id}`,
    method: 'post',
  })
}
// 获取用户的角色列表
export function getAdminRoles(adminId) {
  return request({
    url: `/admin/role/${adminId}`,
    method: 'get',
  })
}
