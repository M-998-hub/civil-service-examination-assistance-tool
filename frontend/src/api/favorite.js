import request from './request'

// 获取我的收藏列表（分页）
export const getFavoriteList = (params) => {
  return request({
    url: '/favorite/my/page',
    method: 'get',
    params,
  })
}

// 添加收藏
export const addFavorite = (positionId) => {
  return request({
    url: `/favorite/add/${positionId}`,
    method: 'post',
  })
}

// 取消收藏
export const removeFavorite = (positionId) => {
  return request({
    url: `/favorite/remove/${positionId}`,
    method: 'post',
  })
}

// 检查是否已收藏
export const checkFavorite = (positionId) => {
  return request({
    url: `/favorite/isFavorite/${positionId}`,
    method: 'get',
  })
}
