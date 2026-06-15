import request from './request'

const STORAGE_KEY = 'deepseek_api_key'

export function getApiKey() {
  return localStorage.getItem(STORAGE_KEY) || ''
}

export function setApiKey(key) {
  localStorage.setItem(STORAGE_KEY, key)
}

export function removeApiKey() {
  localStorage.removeItem(STORAGE_KEY)
}

export function maskApiKey(key) {
  if (!key || key.length <= 7) return '***'
  return key.substring(0, 3) + '***' + key.substring(key.length - 4)
}

export function hasApiKey() {
  return !!getApiKey()
}

function getKeyOrThrow() {
  const key = getApiKey()
  if (!key) {
    const err = new Error('not configured')
    err.code = 'NO_API_KEY'
    throw err
  }
  return key
}

export const analyzeMatch = (data) => {
  const key = getKeyOrThrow()
  return request({
    url: '/ai/analyze',
    method: 'post',
    data: { ...data, apiKey: key }
  })
}

export const aiAsk = (data) => {
  const key = getKeyOrThrow()
  return request({
    url: '/ai/ask',
    method: 'post',
    data: { ...data, apiKey: key }
  })
}
