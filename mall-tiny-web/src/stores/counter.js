import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getAdminInfo } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const clearToken = () => {
    token.value = ''
    localStorage.removeItem('token')
    userInfo.value = null
  }

  const fetchUserInfo = async () => {
    try {
      const res = await getAdminInfo()
      userInfo.value = res.data
      return res.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    }
  }

  const logout = () => {
    clearToken()
  }

  return {
    token,
    userInfo,
    setToken,
    clearToken,
    fetchUserInfo,
    logout
  }
})
