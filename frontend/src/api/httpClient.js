import axios from 'axios'

// 與 stores/auth.js 共用同一把 localStorage key。
// 這裡刻意不 import stores/auth.js 或 router：
// stores/auth.js 要呼叫 api/auth.js 來登入，api/auth.js 要靠 httpClient.js，
// 如果 httpClient.js 反過來依賴 store 或 router，會繞成循環引用。
export const AUTH_STORAGE_KEY = 'wms_auth'

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

instance.interceptors.request.use((config) => {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  const token = raw ? JSON.parse(raw).token : null
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => (response.status === 204 ? null : response.data),
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      // JwtInterceptor 對「沒帶 token / token 失效過期」只 setStatus(401)，沒有 body；
      // 用「401 又沒有 body」判斷為登入逾時，強制清空並整頁導回登入頁。
      // 登入帳密錯誤（AUTH_BAD_CREDENTIALS）也是 401，但那是 GlobalExceptionHandler
      // 處理過的業務錯誤，一定有 ErrorResponse body，不會落到這個分支。
      if (status === 401 && !data) {
        localStorage.removeItem(AUTH_STORAGE_KEY)
        window.location.href = '/login'
        return Promise.reject(new Error('登入已逾時，請重新登入'))
      }

      // data 是後端 ErrorResponse：{ httpStatusCode, message, errorCode, timestamp, path }
      return Promise.reject(data || new Error(`請求失敗 (${status})`))
    }

    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error('請求逾時，請稍後再試'))
    }
    return Promise.reject(new Error('網路連線失敗，請確認後端是否啟動'))
  }
)

export function httpGet(url, params) {
  return instance.get(url, { params })
}
export function httpPost(url, data) {
  return instance.post(url, data)
}
export function httpPut(url, data) {
  return instance.put(url, data)
}
export function httpDelete(url) {
  return instance.delete(url)
}
