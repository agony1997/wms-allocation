<template>
  <div class="home-page">
    <header class="home-header">
      <span class="home-title">Mock ODS VUE</span>
      <button type="button" class="logout-btn" @click="handleLogout">登出</button>
    </header>

    <table class="info-table">
      <tbody>
        <tr><th>使用者代號</th><td>{{ authStore.userCode }}</td></tr>
        <tr><th>姓名</th><td>{{ authStore.userName }}</td></tr>
        <tr><th>角色</th><td>{{ authStore.role }}</td></tr>
        <tr><th>營業所</th><td>{{ authStore.branchCode }}</td></tr>
      </tbody>
    </table>

    <p class="home-hint">
      骨架與登入流程已打通。之後的業務頁面依領域放在 src/views/，
      API 呼叫依領域放在 src/api/，慣例見 docs/conventions/frontend.md。
    </p>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.home-page {
  max-width: 480px;
  margin: 0 auto;
  padding: 24px;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.home-title {
  font-size: 18px;
  font-weight: 600;
}

.logout-btn {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #dc2626;
  color: #dc2626;
  border-radius: 4px;
  font-size: 13px;
}

.logout-btn:hover {
  background: #fef2f2;
}

.info-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border: 1px solid #e0e4e8;
  border-radius: 8px;
  overflow: hidden;
}

.info-table th,
.info-table td {
  padding: 10px 16px;
  text-align: left;
  border-bottom: 1px solid #e0e4e8;
  font-size: 14px;
}

.info-table tr:last-child th,
.info-table tr:last-child td {
  border-bottom: none;
}

.info-table th {
  width: 120px;
  color: #6b7280;
  font-weight: 500;
}

.home-hint {
  margin-top: 16px;
  color: #6b7280;
  font-size: 13px;
}
</style>
