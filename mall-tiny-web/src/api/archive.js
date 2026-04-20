import request from './request'

// 获取当前用户档案
export const getMyArchive = () => {
  return request({
    url: '/user/archive/my',
    method: 'get'
  })
}

// 保存当前用户档案
export const saveMyArchive = (data) => {
  return request({
    url: '/user/archive/save',
    method: 'post',
    data
  })
}