import request from './request'

export const login = (data) => {
  return request({
    url: '/admin/login',
    method: 'post',
    data
  })
}

export const register = (data) => {
  return request({
    url: '/admin/register',
    method: 'post',
    data
  })
}

export const sendVerifyCode = (data) => {
  return request({
    url: '/admin/forgot/send-code',
    method: 'post',
    data
  })
}

export const resetPassword = (data) => {
  return request({
    url: '/admin/forgot/reset',
    method: 'post',
    data
  })
}

export const getAdminInfo = () => {
  return request({
    url: '/admin/info',
    method: 'get'
  })
}

export const logout = () => {
  // 前端清除token即可，后端无专门登出接口
  localStorage.removeItem('token')
  return Promise.resolve()
}