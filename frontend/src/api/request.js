import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

let isRefreshing = false
let pendingRequests = []

const resolvePending = (newToken) => {
  pendingRequests.forEach((cb) => cb(newToken))
  pendingRequests = []
}

const rejectPending = () => {
  pendingRequests.forEach((cb) => cb(null))
  pendingRequests = []
}

// 请求拦截器：添加 token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：处理错误 + 401 自动刷新
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  async (error) => {
    const originalRequest = error.config
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        if (isRefreshing) {
          return new Promise((resolve) => {
            pendingRequests.push((newToken) => {
              if (newToken) {
                originalRequest.headers.Authorization = `Bearer ${newToken}`
                resolve(service(originalRequest))
              } else {
                resolve(Promise.reject(error))
              }
            })
          })
        }
        originalRequest._retry = true
        isRefreshing = true
        try {
          const res = await axios.post(
            (import.meta.env.VITE_API_BASE_URL || '/api') + '/admin/refresh',
            { refreshToken },
            { timeout: 10000 },
          )
          if (res.data && res.data.code === 200 && res.data.data) {
            const data = res.data.data
            localStorage.setItem('token', data.token)
            localStorage.setItem('refreshToken', data.refreshToken)
            resolvePending(data.token)
            originalRequest.headers.Authorization = `Bearer ${data.token}`
            return service(originalRequest)
          }
        } catch {
          rejectPending()
        }
        isRefreshing = false
      }
      ElMessage.warning('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      window.location.href = '/login'
    } else if (error.code === 'ECONNABORTED' && error.message.includes('timeout')) {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  },
)

export default service
