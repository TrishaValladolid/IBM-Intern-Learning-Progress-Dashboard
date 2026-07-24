<script setup>
/**
 * Left sidebar navigation. The item list is derived from the current user's
 * role, so ADMIN and TRAINER see different menus. Active page highlighting is
 * driven by vue-router's `router-link-active` class (Carbon left-border accent).
 *
 * This component only renders links to existing/placeholder routes — it holds
 * no business logic.
 */
import { computed } from 'vue'
import auth from '../services/auth'

// Inline Carbon-style glyphs keyed by name, so the item map stays declarative.
const icons = {
  dashboard: 'M4 4h10v10H4zM18 4h10v10H18zM4 18h10v10H4zM18 18h10v10H18z',
  interns: 'M16 4a5 5 0 1 0 5 5 5 5 0 0 0-5-5zm0 8a3 3 0 1 1 3-3 3 3 0 0 1-3 3zM26 30h-2v-4a4 4 0 0 0-4-4h-8a4 4 0 0 0-4 4v4H6v-4a6 6 0 0 1 6-6h8a6 6 0 0 1 6 6z',
  trainers: 'M11 4a4 4 0 1 0 4 4 4 4 0 0 0-4-4zm0 6a2 2 0 1 1 2-2 2 2 0 0 1-2 2zM17 28h-2v-4a3 3 0 0 0-3-3H9a3 3 0 0 0-3 3v4H4v-4a5 5 0 0 1 5-5h3a5 5 0 0 1 5 5zM24 14a3 3 0 1 0-3-3 3 3 0 0 0 3 3zm4 8h-2a2 2 0 0 0-2-2h-2v-2h2a4 4 0 0 1 4 4z',
  batches: 'M28 6H4a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h24a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2zm0 18H4V8h24zM7 12h8v2H7zm0 5h12v2H7z',
  assignments: 'M25 5h-3V4a2 2 0 0 0-2-2h-8a2 2 0 0 0-2 2v1H7a2 2 0 0 0-2 2v21a2 2 0 0 0 2 2h18a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zM12 4h8v4h-8zm13 24H7V7h3v3h12V7h3z',
  attendance: 'M26 4h-4V2h-2v2h-8V2h-2v2H6a2 2 0 0 0-2 2v20a2 2 0 0 0 2 2h20a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 22H6V12h20zm0-16H6V6h4v2h2V6h8v2h2V6h4zM14 16l-2 2 4 4 6-6-2-2-4 4z',
  progress: 'M6 26h24v2H6zM8 22h4V10H8zm7 0h4V4h-4zm7 0h4v-8h-4z',
  reports: 'M25 5h-3V4a2 2 0 0 0-2-2h-8a2 2 0 0 0-2 2v1H7a2 2 0 0 0-2 2v21a2 2 0 0 0 2 2h18a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zM12 4h8v4h-8zm-2 20v-2h6v2zm12-5H10v-2h12zm0-5H10v-2h12z',
}

const adminItems = [
  { label: 'Dashboard', to: '/admin', icon: 'dashboard' },
  { label: 'Interns', to: '/interns', icon: 'interns' },
  { label: 'Trainers', to: '/trainers', icon: 'trainers' },
  { label: 'Training Batches', to: '/batches', icon: 'batches' },
  { label: 'Assignments and Grades', to: '/assignments', icon: 'assignments' },
  { label: 'Attendance', to: '/attendance', icon: 'attendance' },
  { label: 'Reports', to: '/reports', icon: 'reports' },
]

const trainerItems = [
  { label: 'Dashboard', to: '/trainer', icon: 'dashboard' },
  { label: 'Interns', to: '/interns', icon: 'interns' },
  { label: 'Assignments and Grades', to: '/assignments', icon: 'assignments' },
  { label: 'Attendance', to: '/attendance', icon: 'attendance' },
  { label: 'Progress Tracking', to: '/progress-tracking', icon: 'progress' },
  { label: 'Reports', to: '/reports', icon: 'reports' },
]

const items = computed(() => (auth.isAdmin.value ? adminItems : trainerItems))
</script>

<template>
  <aside class="sidebar">
    <nav class="side-nav" aria-label="Primary">
      <router-link
        v-for="item in items"
        :key="item.to"
        :to="item.to"
        class="side-link"
      >
        <svg class="side-icon" width="18" height="18" viewBox="0 0 32 32" fill="currentColor" aria-hidden="true">
          <path :d="icons[item.icon]" />
        </svg>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 256px;
  flex-shrink: 0;
  background: var(--white);
  border-right: 1px solid var(--border);
}
.side-nav {
  display: flex;
  flex-direction: column;
  padding: var(--sp-01) 0;
}
.side-link {
  display: flex;
  align-items: center;
  gap: var(--sp-02);
  height: 48px;
  padding: 0 var(--sp-03);
  font-size: 14px;
  color: var(--text-secondary);
  text-decoration: none;
  border-left: 3px solid transparent;
  transition:
    color 150ms ease,
    background-color 150ms ease,
    border-color 150ms ease;
}
.side-link:hover {
  color: var(--text);
  background: var(--gray-10);
  text-decoration: none;
}
.side-icon {
  flex-shrink: 0;
  color: var(--text-secondary);
}
/* Active page: Carbon left-border accent + emphasized label. */
.side-link.router-link-active {
  color: var(--text);
  font-weight: 600;
  background: var(--gray-10);
  border-left-color: var(--blue-60);
}
.side-link.router-link-active .side-icon {
  color: var(--blue-60);
}

@media (max-width: 768px) {
  .sidebar {
    width: 64px;
  }
  .side-link span {
    display: none;
  }
  .side-link {
    justify-content: center;
    padding: 0;
  }
}
</style>
