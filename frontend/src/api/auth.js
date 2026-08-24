import { httpPost } from '@/api/httpClient'

// 對應 POST /api/auth/login（baseURL 已經是 /api，這裡不用再加 /api 前綴）
export function login(userCode, password) {
  return httpPost('/auth/login', { userCode, password })
}
