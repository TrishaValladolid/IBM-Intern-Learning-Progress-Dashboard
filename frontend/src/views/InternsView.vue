<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/client'
import auth from '../services/auth'

const isAdmin = auth.isAdmin

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
    <div class="page-header">
      <h1>Interns</h1>
      <p class="subtitle">Manage intern records and open individual progress trackers.</p>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>

    <section v-if="isAdmin" class="card section">
      <h3 class="form-title">Add intern</h3>
      <form class="add-form" @submit.prevent="addIntern">
        <div class="field">
          <label for="intern-name">Name</label>
          <input id="intern-name" class="input" v-model="form.name" placeholder="Full name" required />
        </div>
        <div class="field">
          <label for="intern-emp">Employee ID</label>
          <input id="intern-emp" class="input" v-model="form.employeeId" placeholder="e.g. IBM-0042" />
        </div>
        <div class="field">
          <label for="intern-batch">Batch</label>
          <input id="intern-batch" class="input" v-model="form.batch" placeholder="e.g. 2026-Q1" />
        </div>
        <div class="field">
          <label for="intern-track">Track</label>
          <input id="intern-track" class="input" v-model="form.track" placeholder="e.g. Backend" />
        </div>
        <div class="field field--action">
          <button type="submit" class="btn btn--primary">Add Intern</button>
        </div>
      </form>
    </section>

    <p v-if="loading" class="muted">Loading…</p>
    <div v-else class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>Name</th><th>Employee ID</th><th>Batch</th><th>Track</th><th v-if="isAdmin" class="col-action"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="i in interns" :key="i.id">
            <td>
              <router-link :to="`/interns/${i.id}/progress`">{{ i.name }}</router-link>
            </td>
            <td>{{ i.employeeId }}</td>
            <td>{{ i.batch }}</td>
            <td>{{ i.track }}</td>
            <td v-if="isAdmin" class="col-action">
              <button class="btn btn--danger" @click="removeIntern(i.id)">Delete</button>
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
