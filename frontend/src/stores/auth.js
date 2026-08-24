/**
 *  管理跨頁面共用的資料狀態, token, name, branch...
 */

import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/auth'
import {AUTH_STORAGE_KEY} from '@/api/httpClient.js'

function readStorage() {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  return raw ? JSON.parse(raw) : null
}

// setup store 的函式本體只在第一次呼叫 useAuthStore() 時執行一次，
// 之後都是同一個實例——讀 localStorage 只會發生這一次，不用另外寫 init()。
// mean : 這作為第一個地方呼叫過了, 其他頁面都直接讀取就好

export const useAuthStore = defineStore('auth', () => {
  const saved = readStorage()

  const token = ref(saved?.token ?? null)
  const userCode = ref(saved?.userCode ?? null)
  const userName = ref(saved?.userName ?? null)
  const role = ref(saved?.role ?? null)
  const branchCode = ref(saved?.branchCode ?? null)

  const isAuthenticated = computed(() => !!token.value)

  async function login(code, password) {
    const data = await loginApi(code, password)
    token.value = data.token
    userCode.value = data.userCode
    userName.value = data.userName
    role.value = data.role
    branchCode.value = data.branchCode
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(data))
  }

  function logout() {
    token.value = null
    userCode.value = null
    userName.value = null
    role.value = null
    branchCode.value = null
    localStorage.removeItem(AUTH_STORAGE_KEY)
  }

  return { token, userCode, userName, role, branchCode, isAuthenticated, login, logout }
})
