<script setup>
/**
 * Top navigation bar for the authenticated shell.
 *
 * Carries only the three things the requirement list calls for on the top bar:
 * the system title/logo, the logged-in user's name + role, and a logout button.
 * All page navigation lives in the Sidebar, not here.
 *
 * Extracted from the previous inline header in App.vue.
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import auth from '../services/auth'

const router = useRouter()

const dashboardPath = computed(() => (auth.isAdmin.value ? '/admin' : '/trainer'))
const roleLabel = computed(() =>
  auth.isAdmin.value ? 'Program Coordinator' : auth.isTrainer.value ? 'Trainer' : ''
)

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <!-- System title / logo -->
      <router-link :to="dashboardPath" class="brand">
        <span class="brand-mark">IBM</span>
        <span class="brand-divider" aria-hidden="true"></span>
        <span class="brand-name">Learning Progress Dashboard</span>
      </router-link>

      <!-- Right: logged-in user identity + logout -->
      <div class="header-actions">
        <div class="user-chip" v-if="auth.state.user">
          <span class="user-name">{{ auth.state.user.fullName }}</span>
          <span class="user-role">{{ roleLabel }}</span>
        </div>
        <button
          class="icon-btn"
          type="button"
          title="Log out"
          aria-label="Log out"
          @click="logout"
        >
          <svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor" aria-hidden="true">
            <path d="M6 30h12a2 2 0 0 0 2-2v-3h-2v3H6V4h12v3h2V4a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v24a2 2 0 0 0 2 2Z"/>
            <path d="M20.6 20.6 24.2 17H10v-2h14.2l-3.6-3.6L22 10l6 6-6 6-1.4-1.4z"/>
          </svg>
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--white);
  border-bottom: 1px solid var(--border);
}
.header-inner {
  height: 48px;
  padding: 0 var(--sp-03);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-04);
}

/* Brand / logo */
.brand {
  display: flex;
  align-items: center;
  gap: var(--sp-02);
  flex-shrink: 0;
  text-decoration: none;
}
.brand:hover {
  text-decoration: none;
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
  font-weight: 400;
  color: var(--text-secondary);
}

/* Right-side actions */
.header-actions {
  display: flex;
  align-items: center;
  gap: var(--sp-02);
  flex-shrink: 0;
}
.user-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.2;
}
.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}
.user-role {
  font-size: 11px;
  color: var(--text-secondary);
}
.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: transparent;
  border: none;
  color: var(--text);
  cursor: pointer;
  transition: background-color 150ms ease;
}
.icon-btn:hover {
  background: var(--gray-10);
}
.icon-btn:focus-visible {
  outline: 2px solid var(--blue-60);
  outline-offset: -2px;
}

@media (max-width: 768px) {
  .header-inner {
    gap: var(--sp-02);
    padding: 0 var(--sp-02);
  }
  .brand-name {
    display: none;
  }
}
</style>
