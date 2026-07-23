<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/client'

const interns = ref([])
const form = ref({ name: '', employeeId: '', batch: '', track: '' })
const loading = ref(false)
const error = ref('')

async function loadInterns() {
  loading.value = true
  try {
    const res = await api.get('/interns')
    interns.value = res.data
  } catch (e) {
    error.value = 'Could not load interns. Is the backend running?'
  } finally {
    loading.value = false
  }
}

async function addIntern() {
  if (!form.value.name) return
  try {
    await api.post('/interns', form.value)
    form.value = { name: '', employeeId: '', batch: '', track: '' }
    await loadInterns()
  } catch (e) {
    error.value = 'Could not add intern.'
  }
}

async function removeIntern(id) {
  await api.delete(`/interns/${id}`)
  await loadInterns()
}

onMounted(loadInterns)
</script>

<template>
  <div class="page">
    <h1>Interns</h1>
    <p v-if="error" class="error">{{ error }}</p>

    <form class="add-form" @submit.prevent="addIntern">
      <input v-model="form.name" placeholder="Name" required />
      <input v-model="form.employeeId" placeholder="Employee ID" />
      <input v-model="form.batch" placeholder="Batch" />
      <input v-model="form.track" placeholder="Track" />
      <button type="submit">Add Intern</button>
    </form>

    <p v-if="loading">Loading...</p>
    <table v-else class="data-table">
      <thead>
        <tr><th>Name</th><th>Employee ID</th><th>Batch</th><th>Track</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="i in interns" :key="i.id">
          <td>
            <router-link :to="`/interns/${i.id}/progress`">{{ i.name }}</router-link>
          </td>
          <td>{{ i.employeeId }}</td>
          <td>{{ i.batch }}</td>
          <td>{{ i.track }}</td>
          <td><button @click="removeIntern(i.id)">Delete</button></td>
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
