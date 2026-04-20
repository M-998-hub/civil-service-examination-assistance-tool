import request from './request'

// 一键匹配推荐岗位
export const recommend = () => {
  return request({
    url: '/match/recommend',
    method: 'post'
  })
}