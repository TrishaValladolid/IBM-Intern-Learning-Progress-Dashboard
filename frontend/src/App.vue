<script setup>
/**
 * App root. Chooses between two shells:
 *  - the standalone login screen (bare <router-view>, no chrome), and
 *  - the authenticated DashboardLayout (top nav + sidebar + content).
 *
 * All chrome now lives inside DashboardLayout / its child components; this file
 * only decides which shell to render.
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import auth from './services/auth'
import DashboardLayout from './layouts/DashboardLayout.vue'

const route = useRoute()

// Show the full shell only when authenticated and off the login screen.
const showShell = computed(() => auth.isAuthenticated.value && route.path !== '/login')
</script>

<template>
  <DashboardLayout v-if="showShell" />
  <router-view v-else />
</template>
