<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import auth from '../services/auth'
import BaseInput from '../components/BaseInput.vue'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')

const usernameError = ref('')
const passwordError = ref('')
const formError = ref('') // invalid-credentials / server errors
const loading = ref(false)

function validate() {
  usernameError.value = username.value.trim() ? '' : 'Username is required.'
  passwordError.value = password.value ? '' : 'Password is required.'
  return !usernameError.value && !passwordError.value
}

async function onSubmit() {
  formError.value = ''
  if (!validate()) return

  loading.value = true
  try {
    const user = await auth.login(username.value.trim(), password.value)
    // Honor a "redirect" query (set by the router guard) when it targets the
    // user's role; otherwise go to the role's default dashboard.
    const target = user.role === 'ADMIN' ? '/admin' : '/trainer'
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.replace(redirect || target)
  } catch (e) {
    formError.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="brand">
        <span class="brand-mark">IBM</span>
        <span class="brand-divider" aria-hidden="true"></span>
        <span class="brand-name">Learning Progress Dashboard</span>
      </div>

      <h1 class="login-title">Log in</h1>
      <p class="login-subtitle">Sign in to manage training programs and track progress.</p>

      <p v-if="formError" class="form-banner" role="alert">{{ formError }}</p>

      <form class="login-form" novalidate @submit.prevent="onSubmit">
        <BaseInput
          v-model="username"
          label="Username"
          placeholder="Enter your username"
          autocomplete="username"
          :error="usernameError"
          :disabled="loading"
        />
        <BaseInput
          v-model="password"
          label="Password"
          type="password"
          placeholder="Enter your password"
          autocomplete="current-password"
          :error="passwordError"
          :disabled="loading"
        />

        <button type="submit" class="btn btn--primary login-btn" :disabled="loading">
          <span v-if="!loading">Log in</span>
          <span v-else class="loading-inline">
            <span class="spinner" aria-hidden="true"></span>
            Signing in…
          </span>
        </button>
      </form>
    </div>

    <footer class="login-footer">© IBM — Learning Progress Dashboard</footer>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--sp-03);
  padding: var(--sp-04);
  background: var(--blue-10);
}

.login-panel {
  width: 100%;
  max-width: 400px;
  background: var(--white);
  border: 1px solid var(--border);
  border-top: 3px solid var(--blue-60);
  padding: var(--sp-05);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--sp-02);
  margin-bottom: var(--sp-04);
}
.brand-mark {
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.02em;
  color: var(--text);
}
.brand-divider {
  width: 1px;
  height: 20px;
  background: var(--border);
}
.brand-name {
  font-size: 14px;
  color: var(--text-secondary);
}

.login-title {
  font-size: 28px;
  font-weight: 400;
  color: var(--text);
  margin-bottom: var(--sp-01);
}
.login-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: var(--sp-04);
}

.form-banner {
  background: #fff1f1;
  border-left: 3px solid var(--support-error);
  color: var(--support-error);
  font-size: 13px;
  padding: var(--sp-02);
  margin-bottom: var(--sp-03);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--sp-03);
}

.login-btn {
  width: 100%;
  justify-content: center;
  margin-top: var(--sp-01);
}

.loading-inline {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-01);
}
.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-top-color: var(--white);
  border-radius: 50%;
  animation: spin 700ms linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.login-footer {
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 480px) {
  .login-panel {
    padding: var(--sp-04);
    border: none;
    border-top: 3px solid var(--blue-60);
  }
}
</style>
