<script setup>
/**
 * Admin dashboard home.
 *
 * Dashboard-foundation phase: shows KPI cards with MOCK values only. No API
 * calls, no CRUD — the numbers below are placeholders until the data layer is
 * wired up in a later phase.
 */
import auth from '../services/auth'
import DashboardCard from '../components/DashboardCard.vue'

// Placeholder KPI values (not connected to the backend).
const cards = [
  { label: 'Total Interns', value: 128 },
  { label: 'Total Trainers', value: 12 },
  { label: 'Total Assignments', value: 46 },
  { label: 'Pending Reports', value: 5 },
  {
    label: 'Interns Requiring Attention',
    value: 4,
    accent: 'warning',
    caption: 'Below target this week',
  },
]
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Admin Dashboard</h1>
      <p class="subtitle">
        Welcome, {{ auth.state.user?.fullName }} — Program Coordinator.
      </p>
    </div>

    <section class="stat-grid section">
      <DashboardCard
        v-for="card in cards"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :accent="card.accent"
        :caption="card.caption"
      />
    </section>
  </div>
</template>
