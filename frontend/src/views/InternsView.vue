<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import auth from '../services/auth'
import Modal from '../components/Modal.vue'

const isAdmin = auth.isAdmin

const interns = ref([])
const form = ref({ name: '', talentId: '', batch: '', track: '' })
const loading = ref(false)
const error = ref('')

// ---- Assign training to a batch ----
const batchDialog = ref(false)
const batchForm = ref({ trainingName: '', repoUrl: '', batch: '' })
const batchError = ref('')
const batchResult = ref('')

// Distinct, non-empty batches taken from the loaded interns.
const batches = computed(() => {
  const set = new Set(
    interns.value.map((i) => i.batch).filter((b) => b && b.trim())
  )
  return [...set].sort()
})

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

function openBatchDialog() {
  batchForm.value = { trainingName: '', repoUrl: '', batch: '' }
  batchError.value = ''
  batchResult.value = ''
  batchDialog.value = true
}

async function assignBatch() {
  if (!batchForm.value.trainingName.trim() || !batchForm.value.batch) {
    batchError.value = 'Training name and batch are required.'
    return
  }
  batchError.value = ''
  try {
    const res = await api.post('/interns/trainings/batch', {
      trainingName: batchForm.value.trainingName.trim(),
      repoUrl: batchForm.value.repoUrl.trim(),
      batch: batchForm.value.batch,
    })
    const r = res.data
    batchResult.value =
      `"${r.trainingName}" assigned to ${r.assigned} of ${r.total} intern(s) in batch ${r.batch}` +
      (r.skipped ? ` (${r.skipped} already had it).` : '.')
  } catch (e) {
    batchError.value = 'Could not assign training to batch.'
  }
}

async function addIntern() {
  if (!form.value.name) return
  try {
    await api.post('/interns', form.value)
    form.value = { name: '', talentId: '', batch: '', track: '' }
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
      <div class="header-row">
        <div>
          <h1>Interns</h1>
          <p class="subtitle">Manage intern records and open individual progress trackers.</p>
        </div>
        <button v-if="isAdmin" class="btn btn--primary" @click="openBatchDialog">
          Assign Training to Batch
        </button>
      </div>
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
          <label for="intern-emp">Talent ID</label>
          <input id="intern-emp" class="input" v-model="form.talentId" placeholder="e.g. AVY9VKPH1" />
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
            <th>Name</th><th>Talent ID</th><th>Batch</th><th>Track</th><th v-if="isAdmin" class="col-action"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="i in interns" :key="i.id">
            <td>
              <router-link :to="`/interns/${i.id}/progress`">{{ i.name }}</router-link>
            </td>
            <td>{{ i.talentId }}</td>
            <td>{{ i.batch }}</td>
            <td>{{ i.track }}</td>
            <td v-if="isAdmin" class="col-action">
              <button class="btn btn--danger" @click="removeIntern(i.id)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Assign training to a batch (ADMIN) -->
    <Modal :open="batchDialog" title="Assign training to batch" @close="batchDialog = false">
      <p class="muted modal-hint">
        Create a training once and assign it to every intern in a batch. Interns who
        already have this training are skipped.
      </p>
      <p v-if="batchError" class="error">{{ batchError }}</p>
      <p v-if="batchResult" class="batch-result">{{ batchResult }}</p>

      <div class="field">
        <label for="batch-training-name">Training name</label>
        <input
          id="batch-training-name"
          class="input"
          v-model="batchForm.trainingName"
          placeholder="e.g. Java, Ionic, Japanese Language"
          required
        />
      </div>
      <div class="field">
        <label for="batch-select">Batch</label>
        <select id="batch-select" class="select" v-model="batchForm.batch" required>
          <option value="" disabled>Select a batch</option>
          <option v-for="b in batches" :key="b" :value="b">{{ b }}</option>
        </select>
        <p v-if="batches.length === 0" class="muted">No batches found. Add interns with a batch first.</p>
      </div>
      <div class="field">
        <label for="batch-box">Box Drive link (optional)</label>
        <input
          id="batch-box"
          class="input"
          v-model="batchForm.repoUrl"
          placeholder="https://app.box.com/… (shared upload folder)"
        />
      </div>

      <template #footer>
        <button class="btn btn--secondary" @click="batchDialog = false">Close</button>
        <button class="btn btn--primary" @click="assignBatch">Assign</button>
      </template>
    </Modal>
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
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--sp-03);
  flex-wrap: wrap;
}
.modal-hint {
  margin-bottom: var(--sp-03);
}
.batch-result {
  color: var(--support-success, #24a148);
  font-weight: 600;
  margin-bottom: var(--sp-02);
}
</style>
