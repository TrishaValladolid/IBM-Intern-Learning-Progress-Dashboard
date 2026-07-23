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
    <div class="page-header">
      <h1>Assignments</h1>
      <p class="subtitle">Define assignments and their maximum scores for each batch.</p>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>

    <section class="card section">
      <h3 class="form-title">Add assignment</h3>
      <form class="add-form" @submit.prevent="addAssignment">
        <div class="field">
          <label for="a-title">Title</label>
          <input id="a-title" class="input" v-model="form.title" placeholder="Assignment title" required />
        </div>
        <div class="field">
          <label for="a-max">Max score</label>
          <input id="a-max" class="input" v-model.number="form.maxScore" type="number" placeholder="Max score" />
        </div>
        <div class="field">
          <label for="a-batch">Batch</label>
          <input id="a-batch" class="input" v-model="form.batch" placeholder="e.g. 2026-Q1" />
        </div>
        <div class="field field--action">
          <button type="submit" class="btn btn--primary">Add Assignment</button>
        </div>
      </form>
    </section>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>Title</th><th>Max Score</th><th>Batch</th><th class="col-action"></th></tr>
        </thead>
        <tbody>
          <tr v-for="a in assignments" :key="a.id">
            <td>{{ a.title }}</td>
            <td>{{ a.maxScore }}</td>
            <td>{{ a.batch }}</td>
            <td class="col-action">
              <button class="btn btn--danger" @click="removeAssignment(a.id)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.form-title {
  margin-bottom: var(--sp-02);
}
.add-form {
  display: flex;
  gap: var(--sp-02);
  flex-wrap: wrap;
  align-items: flex-end;
}
.add-form .field {
  flex: 1 1 180px;
  min-width: 160px;
}
.add-form .field--action {
  flex: 0 0 auto;
  min-width: 0;
}
.col-action {
  text-align: right;
  width: 1%;
  white-space: nowrap;
}
</style>
