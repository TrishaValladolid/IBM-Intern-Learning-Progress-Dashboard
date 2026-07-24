<script setup>
/**
 * Admin dashboard home.
 *
 * KPI cards backed by live data. Every figure below is fetched from an existing
 * endpoint the ADMIN role can reach — no mock values.
 */
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import auth from '../services/auth'
import DashboardCard from '../components/DashboardCard.vue'

const loading = ref(true)
const error = ref('')

const totalInterns = ref(0)
const totalTrainers = ref(0)
const totalBatches = ref(0)
const totalAssignments = ref(0)
const attendanceRate = ref(0)
const pendingGrading = ref(0)

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [interns, trainers, batches, assignments, attendance, grades] =
      await Promise.all([
        api.get('/interns'),
        api.get('/trainers'),
        api.get('/interns/batches'),
        api.get('/assignments'),
        api.get('/attendance/summary'),
        api.get('/submissions'),
      ])
    totalInterns.value = interns.data.length
    totalTrainers.value = trainers.data.filter((u) => u.role === 'TRAINER').length
    totalBatches.value = batches.data.length
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
  { label: 'Total Interns', value: totalInterns.value },
  { label: 'Total Trainers', value: totalTrainers.value },
  { label: 'Training Batches', value: totalBatches.value },
  { label: 'Total Assignments', value: totalAssignments.value },
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
      <h1>Admin Dashboard</h1>
      <p class="subtitle">
        Welcome, {{ auth.state.user?.fullName }} — Program Coordinator.
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
