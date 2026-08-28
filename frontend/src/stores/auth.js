/**
 *  管理跨頁面共用的資料狀態, token, name, branch...
 */

import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/auth'
import {AUTH_STORAGE_KEY} from '@/api/httpClient.js'

function readStorage() {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  const saved = raw ? JSON.parse(raw) : null
  // 舊格式（單值 role + branchCode）一律丟棄，當成未登入。
  // 它連帶的 token 也是舊格式（claim 是 role 不是 branchRoles），後端解出來會是空權限，
  // 留著只會讓使用者看似已登入、卻每個操作都 403——不如直接請他重新登入。
  if (saved && !saved.branchRoles) {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
  return saved
}

// setup store 的函式本體只在第一次呼叫 useAuthStore() 時執行一次，
// 之後都是同一個實例——讀 localStorage 只會發生這一次，不用另外寫 init()。
// mean : 這作為第一個地方呼叫過了, 其他頁面都直接讀取就好

export const useAuthStore = defineStore('auth', () => {
  const saved = readStorage()

  const token = ref(saved?.token ?? null)
  const userCode = ref(saved?.userCode ?? null)
  const userName = ref(saved?.userName ?? null)
  // { branchCode: [roleCode...] }，與 token 的 branchRoles claim 同一份資料。
  // 沒有單值 role／branchCode：一人可在同一營業所兼多角、也可跨多個營業所，
  // 單值必然是謊話；系統也無「主要營業所」概念。見 docs/.../master/User.md
  const branchRoles = ref(saved?.branchRoles ?? null)

  const isAuthenticated = computed(() => !!token.value)

  async function login(code, password) {
    const data = await loginApi(code, password)
    token.value = data.token
    userCode.value = data.userCode
    userName.value = data.userName
    branchRoles.value = data.branchRoles
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(data))
  }

  function logout() {
    token.value = null
    userCode.value = null
    userName.value = null
    branchRoles.value = null
    localStorage.removeItem(AUTH_STORAGE_KEY)
  }

  // 我在哪些營業所具備 requiredRoles 之中的任一角色，已排序。
  // 頁面拿它當營業所欄位的選項來源，[0] 即預設值（見 User.md「前端營業所選擇器」）。
  // 正常資料下這個過濾是 no-op——一個人跨營業所通常擔任同一職務，故各頁清單一致；
  // 過濾是防禦性的，不讓「甲所業務、乙所庫務」這種人看到選了必然 403 的選項。
  function branchesWithAnyRole(requiredRoles) {
    return Object.entries(branchRoles.value ?? {})
      .filter(([, roles]) => roles.some((r) => requiredRoles.includes(r)))
      .map(([code]) => code)
      .sort()
  }

  return { token, userCode, userName, branchRoles, isAuthenticated, login, logout, branchesWithAnyRole }
})
