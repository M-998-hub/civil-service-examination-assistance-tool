import request from './request'

export const login = (data) => {
  return request({
    url: '/admin/login',
    method: 'post',
    data,
  })
}

export const loginByCode = (data) => {
  return request({
    url: '/admin/login-by-code',
    method: 'post',
    data,
  })
}

export const refreshToken = (refreshToken) => {
  return request({
    url: '/admin/refresh',
    method: 'post',
    data: { refreshToken },
  })
}

export const getCaptcha = () => {
  return request({
    url: '/admin/captcha',
    method: 'get',
  })
}

export const sendLoginCode = (data) => {
  return request({
    url: '/admin/send-login-code',
    method: 'post',
    data,
  })
}

export const register = (data) => {
  return request({
    url: '/admin/register',
    method: 'post',
    data,
  })
}

export const sendRegisterCode = (data) => {
  return request({
    url: '/admin/register/send-code',
    method: 'post',
    data,
  })
}

export const sendVerifyCode = (data) => {
  return request({
    url: '/admin/forgot/send-code',
    method: 'post',
    data,
  })
}

export const resetPassword = (data) => {
  return request({
    url: '/admin/forgot/reset',
    method: 'post',
    data,
  })
}

export const getAdminInfo = () => {
  return request({
    url: '/admin/info',
    method: 'get',
  })
}

export const logout = () => {
  const refreshToken = localStorage.getItem('refreshToken')
  if (refreshToken) {
    request({ url: '/admin/logout', method: 'post', data: { refreshToken } }).catch(() => {})
  }
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  return Promise.resolve()
}
