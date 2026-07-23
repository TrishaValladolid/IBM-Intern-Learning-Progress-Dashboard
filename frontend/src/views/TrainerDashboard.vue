<script setup>
/**
 * Trainer dashboard home.
 *
 * Dashboard-foundation phase: shows KPI cards with MOCK values only. No API
 * calls, no CRUD — the numbers below are placeholders until the data layer is
 * wired up in a later phase.
 */
import auth from '../services/auth'
import DashboardCard from '../components/DashboardCard.vue'

// Placeholder KPI values (not connected to the backend).
const cards = [
  { label: 'Assigned Batches', value: 3 },
  { label: 'Total Interns', value: 28 },
  { label: 'Assignments', value: 14 },
  { label: 'Attendance Today', value: '26 / 28' },
  {
    label: 'Pending Submissions',
    value: 7,
    accent: 'warning',
    caption: 'Awaiting your review',
  },
]
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Trainer Dashboard</h1>
      <p class="subtitle">
        Welcome, {{ auth.state.user?.fullName }} — Trainer.
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
