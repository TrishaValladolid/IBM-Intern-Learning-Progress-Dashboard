<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'

const route = useRoute()
const progress = ref(null)
const assignments = ref([])
const scoreForm = ref({ assignmentId: '', score: '' })
const error = ref('')

async function loadProgress() {
  try {
    const res = await api.get(`/interns/${route.params.id}/progress`)
    progress.value = res.data
  } catch (e) {
    error.value = 'Could not load progress. Is the backend running?'
  }
}

async function loadAssignments() {
  const res = await api.get('/assignments')
  assignments.value = res.data
}

async function submitScore() {
  if (!scoreForm.value.assignmentId || scoreForm.value.score === '') return
  await api.post('/submissions', {
    internId: route.params.id,
    assignmentId: scoreForm.value.assignmentId,
    score: scoreForm.value.score,
    status: 'GRADED',
  })
  scoreForm.value = { assignmentId: '', score: '' }
  await loadProgress()
}

onMounted(() => {
  loadProgress()
  loadAssignments()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <router-link to="/interns" class="back-link">← Interns</router-link>
      <h1>Intern Progress</h1>
      <p v-if="progress" class="subtitle">{{ progress.internName }}</p>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>

    <template v-if="progress">
      <!-- KPI stat tiles -->
      <section class="stat-grid section">
        <div class="stat-tile">
          <span class="stat-label">Assignments completed</span>
          <span class="stat-value">{{ progress.completedAssignments }} / {{ progress.totalAssignments }}</span>
        </div>
        <div class="stat-tile">
          <span class="stat-label">Completion</span>
          <span class="stat-value">{{ progress.completionPercentage }}%</span>
        </div>
        <div class="stat-tile">
          <span class="stat-label">Average score</span>
          <span class="stat-value">{{ progress.averageScorePercentage }}%</span>
        </div>
      </section>

      <!-- Completion meter -->
      <section class="card section">
        <div class="meter-header">
          <span class="stat-label">Overall completion</span>
          <span class="meter-pct">{{ progress.completionPercentage }}%</span>
        </div>
        <div class="meter">
          <div class="meter-fill" :style="{ width: progress.completionPercentage + '%' }"></div>
        </div>
      </section>
    </template>

    <!-- Record a score -->
    <section class="card section">
      <h3 class="form-title">Record a score</h3>
      <form class="add-form" @submit.prevent="submitScore">
        <div class="field">
          <label for="score-assignment">Assignment</label>
          <select id="score-assignment" class="select" v-model="scoreForm.assignmentId" required>
            <option value="" disabled>Select assignment</option>
            <option v-for="a in assignments" :key="a.id" :value="a.id">{{ a.title }} (max {{ a.maxScore }})</option>
          </select>
        </div>
        <div class="field">
          <label for="score-value">Score</label>
          <input id="score-value" class="input" v-model.number="scoreForm.score" type="number" placeholder="Score" required />
        </div>
        <div class="field field--action">
          <button type="submit" class="btn btn--primary">Save Score</button>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.back-link {
  display: inline-block;
  font-size: 14px;
  margin-bottom: var(--sp-02);
}
.back-link:hover {
  text-decoration: none;
}
.form-title {
  margin-bottom: var(--sp-02);
}
.meter-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: var(--sp-01);
}
.meter-pct {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}
.add-form {
  display: flex;
  gap: var(--sp-02);
  flex-wrap: wrap;
  align-items: flex-end;
}
.add-form .field {
  flex: 1 1 220px;
  min-width: 180px;
}
.add-form .field--action {
  flex: 0 0 auto;
  min-width: 0;
}
</style>
