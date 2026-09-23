import request from './request'

// 管理端-分页获取岗位列表
export const getPositionPage = (params) => {
  return request({
    url: '/admin/position/page',
    method: 'get',
    params,
  })
}

// 管理端-获取岗位详情
export const getPositionDetail = (id) => {
  return request({
    url: `/admin/position/${id}`,
    method: 'get',
  })
}

// 管理端-新增岗位
export const createPosition = (data) => {
  return request({
    url: '/admin/position',
    method: 'post',
    data,
  })
}

// 管理端-编辑岗位
export const updatePosition = (id, data) => {
  return request({
    url: `/admin/position/${id}`,
    method: 'put',
    data,
  })
}

// 管理端-删除岗位
export const deletePosition = (id) => {
  return request({
    url: `/admin/position/${id}`,
    method: 'delete',
  })
}

// 管理端-批量删除岗位
export const batchDeletePositions = (ids) => {
  return request({
    url: '/admin/position/batch',
    method: 'delete',
    data: { ids },
  })
}
