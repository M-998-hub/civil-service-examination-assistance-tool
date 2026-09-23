import request from './request'

// 创建角色
export function createRole(data) {
  return request({
    url: '/role/create',
    method: 'post',
    data,
  })
}

// 获取所有角色列表
export function getRoleList() {
  return request({
    url: '/role/list',
    method: 'get',
  })
}

// 获取角色已拥有的资源ID列表
export function getRoleResources(roleId) {
  return request({
    url: `/role/resource/${roleId}`,
    method: 'get',
  })
}

// 给角色分配资源
export function assignResources(data) {
  return request({
    url: '/role/resource/assign',
    method: 'post',
    data,
  })
}

// 给用户分配角色
export function grantRole(data) {
  return request({
    url: '/role/grant',
    method: 'post',
    data,
  })
}

// 更新角色
export function updateRole(id, data) {
  return request({
    url: `/role/update/${id}`,
    method: 'post',
    data,
  })
}

// 删除角色
export function deleteRole(id) {
  return request({
    url: `/role/delete/${id}`,
    method: 'post',
  })
}

// 撤销用户角色
export function revokeUserRole(data) {
  return request({
    url: '/role/revoke',
    method: 'delete',
    data,
  })
}
