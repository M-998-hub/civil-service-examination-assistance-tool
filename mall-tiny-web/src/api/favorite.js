import request from './request'

// 获取收藏列表
export const getFavoriteList = (params) => {
  return request({
    url: '/favorite/list',
    method: 'get',
    params
  })
}

// 添加收藏
export const addFavorite = (positionId) => {
  return request({
    url: `/favorite/add/${positionId}`,
    method: 'post'
  })
}

// 取消收藏
export const removeFavorite = (positionId) => {
  return request({
    url: `/favorite/remove/${positionId}`,
    method: 'delete'
  })
}

// 检查是否已收藏
export const checkFavorite = (positionId) => {
  return request({
    url: `/favorite/check/${positionId}`,
    method: 'get'
  })
}