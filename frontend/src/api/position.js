import request from './request'

// 获取岗位列表
export const getPositionList = (params) => {
  return request({
    url: '/position/list',
    method: 'get',
    params,
  })
}

// 获取岗位详情
export const getPositionDetail = (id) => {
  return request({
    url: `/position/${id}`,
    method: 'get',
  })
}

// 筛选岗位
export const filterPositions = (data) => {
  return request({
    url: '/position/filter',
    method: 'post',
    data,
  })
}

// 获取报录比数据
export const getPositionStats = (params) => {
  return request({
    url: '/position/stats',
    method: 'get',
    params,
  })
}
