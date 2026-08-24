<template>
  <div class="login-page">
    <form class="login-card" @submit.prevent="handleLogin">
      <h1 class="login-title">Mock ODS VUE</h1>

      <label class="field account-field">
        <span>使用者帳號</span>
        <input v-model="form.userCode" type="text" autocomplete="username" required/>
      </label>

      <label class="field pwd-field">
        <span>密碼</span>
        <input v-model="form.password" type="password" autocomplete="current-password" required/>
      </label>

      <label class="field remember-field">
        <input type="checkbox" v-model="isRememberAccount"/>
        <span>記住帳號</span>
      </label>

      <button type="submit" class="login-submit" :disabled="loading">
        {{ loading ? '登入中...' : '登入' }}
      </button>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
    </form>
  </div>
</template>

<script setup>
import {reactive, ref, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const savedUserAccount = localStorage.getItem('savedUser') || ''

const loading = ref(false)
const errorMessage = ref('')
const form = reactive({userCode: savedUserAccount, password: ''})
const isRememberAccount = ref(false)

onMounted(() => {
  if (authStore.token) {
    router.replace('/') // 已經有 token 就不用再看到登入頁（例如手動輸網址回 /login）
  }
})

async function handleLogin() {
  errorMessage.value = ''
  loading.value = true
  try {
    await authStore.login(form.userCode, form.password)

    if (isRememberAccount.value) {
      localStorage.setItem('savedUser', JSON.stringify(form.userCode))
    }

    router.replace('/')
  } catch (err) {
    errorMessage.value = err.message || '登入失敗，請稍後再試'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}

.login-card {
  width: 320px;
  padding: 32px;
  background: #fff;
  border: 1px solid #e0e4e8;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.login-title {
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 5vh;
}

.field {
  display: block;
  margin-bottom: 16px;
  font-size: 13px;
  color: #374151;
}

.field span {
  display: block;
  margin-bottom: 4px;
}

.field input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 14px;
}

.field input:focus {
  outline: 2px solid #2563eb;
  outline-offset: -1px;
  border-color: #2563eb;
}

.login-submit {
  width: 100%;
  padding: 10px;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.login-submit:hover:not(:disabled) {
  background: #1d4ed8;
}

.login-submit:disabled {
  background: #93b4f0;
  cursor: not-allowed;
}

.error-message {
  margin-top: 16px;
  padding: 8px 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 4px;
  color: #b91c1c;
  font-size: 13px;
}

.field input[type='checkbox'] {
  width: auto;
  margin-right: 4px;
  cursor: pointer;
}

.remember-field {
  display: flex;
  align-items: center;
  flex-direction: row;
  justify-content: right;
  cursor: pointer;
  user-select: none;
}

.pwd-field {
  margin-bottom: 6px;
}

</style>
