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
    <h1>Intern Progress</h1>
    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="progress" class="summary">
      <h2>{{ progress.internName }}</h2>
      <p>Assignments completed: {{ progress.completedAssignments }} / {{ progress.totalAssignments }}</p>
      <div class="bar"><div class="bar-fill" :style="{ width: progress.completionPercentage + '%' }"></div></div>
      <p>Completion: {{ progress.completionPercentage }}%</p>
      <p>Average score: {{ progress.averageScorePercentage }}%</p>
    </div>

    <h3>Record a score</h3>
    <form class="add-form" @submit.prevent="submitScore">
      <select v-model="scoreForm.assignmentId" required>
        <option value="" disabled>Select assignment</option>
        <option v-for="a in assignments" :key="a.id" :value="a.id">{{ a.title }} (max {{ a.maxScore }})</option>
      </select>
      <input v-model.number="scoreForm.score" type="number" placeholder="Score" required />
      <button type="submit">Save Score</button>
    </form>
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; padding: 2rem; text-align: left; }
.summary { border: 1px solid #ddd; padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; }
.bar { background: #eee; border-radius: 6px; height: 10px; overflow: hidden; margin: 0.5rem 0; }
.bar-fill { background: #42b883; height: 100%; }
.add-form { display: flex; gap: 0.5rem; }
.error { color: #c0392b; }
</style>
