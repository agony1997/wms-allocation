import {createRouter, createWebHistory} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import HomeView from '@/views/HomeView.vue'
import BranchView from "@/views/BranchView.vue";

const routes = [
  // /api/branches 需要 token，這裡不擋的話會先渲染整頁、再被 httpClient 的 401 攔截彈回登入
  { path: '/branch', name: 'branch', component: BranchView, meta: { requiresAuth: true, title: '營業所管理' } },
  { path: '/login', name: 'login', component: LoginView, meta: { requiresAuth: false, title: '登入' } },
  { path: '/', name: 'home', component: HomeView, meta: { requiresAuth: true, title: '首頁' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  // 刻意寫在 callback 裡面才呼叫 useAuthStore()，不要搬到檔案最上層：
  // router/index.js 會在 main.js 執行 app.use(createPinia()) 之前就被 import 並求值，
  // 那時候還沒有作用中的 Pinia instance，呼叫 useAuthStore() 會丟例外。
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.token) {
    return '/login'
  }
  if (to.path === '/login' && authStore.token) {
    return '/'
  }
  return true
})

export default router
