<script setup>
/**
 * Trainer dashboard home.
 *
 * KPI cards backed by live data. Endpoints are limited to those the TRAINER
 * role can reach (no /trainers or /interns/batches — both ADMIN-only). A "batch"
 * is just the Intern.batch string and the data model has no trainer→batch
 * assignment, so this shows all active trainings, not a per-trainer subset.
 */
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import auth from '../services/auth'
import DashboardCard from '../components/DashboardCard.vue'

const loading = ref(true)
const error = ref('')

const activeTrainings = ref(0)
const totalInterns = ref(0)
const totalAssignments = ref(0)
const attendanceRate = ref(0)
const pendingGrading = ref(0)

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [trainings, interns, assignments, attendance, grades] =
      await Promise.all([
        api.get('/attendance/trainings'),
        api.get('/interns'),
        api.get('/assignments'),
        api.get('/attendance/summary'),
        api.get('/submissions'),
      ])
    activeTrainings.value = trainings.data.length
    totalInterns.value = interns.data.length
    totalAssignments.value = assignments.data.length
    attendanceRate.value = attendance.data.attendancePercentage
    // A grade cell with no score is a recorded submission still awaiting a mark.
    pendingGrading.value = grades.data.filter((g) => g.score == null).length
  } catch (e) {
    error.value = 'Could not load dashboard data. Is the backend running?'
  } finally {
    loading.value = false
  }
}

const cards = computed(() => [
  { label: 'Active Trainings', value: activeTrainings.value },
  { label: 'Total Interns', value: totalInterns.value },
  { label: 'Assignments', value: totalAssignments.value },
  { label: 'Attendance Rate', value: `${attendanceRate.value}%` },
  {
    label: 'Pending Grading',
    value: pendingGrading.value,
    accent: pendingGrading.value > 0 ? 'warning' : '',
    caption: 'Submissions awaiting a score',
  },
])

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Trainer Dashboard</h1>
      <p class="subtitle">
        Welcome, {{ auth.state.user?.fullName }} — Trainer.
      </p>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>
    <p v-else-if="loading" class="muted section">Loading…</p>

    <section v-else class="stat-grid section">
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
