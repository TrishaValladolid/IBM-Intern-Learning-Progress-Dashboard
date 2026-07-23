<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/client'

const assignments = ref([])
const form = ref({ title: '', maxScore: 100, batch: '' })
const error = ref('')

async function loadAssignments() {
  try {
    const res = await api.get('/assignments')
    assignments.value = res.data
  } catch (e) {
    error.value = 'Could not load assignments. Is the backend running?'
  }
}

async function addAssignment() {
  if (!form.value.title) return
  await api.post('/assignments', form.value)
  form.value = { title: '', maxScore: 100, batch: '' }
  await loadAssignments()
}

async function removeAssignment(id) {
  await api.delete(`/assignments/${id}`)
  await loadAssignments()
}

onMounted(loadAssignments)
</script>

<template>
  <div class="page">
    <h1>Assignments</h1>
    <p v-if="error" class="error">{{ error }}</p>

    <form class="add-form" @submit.prevent="addAssignment">
      <input v-model="form.title" placeholder="Assignment title" required />
      <input v-model.number="form.maxScore" type="number" placeholder="Max score" />
      <input v-model="form.batch" placeholder="Batch" />
      <button type="submit">Add Assignment</button>
    </form>

    <table class="data-table">
      <thead><tr><th>Title</th><th>Max Score</th><th>Batch</th><th></th></tr></thead>
      <tbody>
        <tr v-for="a in assignments" :key="a.id">
          <td>{{ a.title }}</td>
          <td>{{ a.maxScore }}</td>
          <td>{{ a.batch }}</td>
          <td><button @click="removeAssignment(a.id)">Delete</button></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.page { max-width: 900px; margin: 0 auto; padding: 2rem; text-align: left; }
.add-form { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; flex-wrap: wrap; }
.add-form input { padding: 0.4rem; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { border: 1px solid #ddd; padding: 0.5rem; text-align: left; }
.error { color: #c0392b; }
</style>
