<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import auth from '../services/auth'
import Modal from '../components/Modal.vue'

const isAdmin = auth.isAdmin

const batches = ref([])
const interns = ref([])
const loading = ref(false)
const error = ref('')

// Which batch rows are expanded to show their intern list.
const expanded = ref(new Set())

// ---- Rename batch ----
const renameDialog = ref(false)
const renameForm = ref({ oldBatch: '', newBatch: '' })
const renameError = ref('')

// KPI tiles roll up the per-batch figures already returned by the backend.
const totalBatches = computed(() => batches.value.length)
const totalInterns = computed(() =>
  batches.value.reduce((sum, b) => sum + b.internCount, 0),
)
const overallCompletion = computed(() => {
  if (batches.value.length === 0) return 0
  const sum = batches.value.reduce((s, b) => s + b.avgCompletionPercentage, 0)
  return Math.round((sum / batches.value.length) * 100) / 100
})

// Interns belonging to a batch, for the expandable inline list.
function internsIn(batch) {
  return interns.value
    .filter((i) => i.batch === batch)
    .sort((a, b) => (a.name || '').localeCompare(b.name || ''))
}

function toggle(batch) {
  const next = new Set(expanded.value)
  if (next.has(batch)) next.delete(batch)
  else next.add(batch)
  expanded.value = next
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [summaryRes, internsRes] = await Promise.all([
      api.get('/interns/batches'),
      api.get('/interns'),
    ])
    batches.value = summaryRes.data
    interns.value = internsRes.data
  } catch (e) {
    error.value = 'Could not load training batches. Is the backend running?'
  } finally {
    loading.value = false
  }
}

function openRename(batch) {
  renameForm.value = { oldBatch: batch, newBatch: batch }
  renameError.value = ''
  renameDialog.value = true
}

async function saveRename() {
  const oldBatch = renameForm.value.oldBatch.trim()
  const newBatch = renameForm.value.newBatch.trim()
  if (!newBatch) {
    renameError.value = 'New batch name is required.'
    return
  }
  if (oldBatch === newBatch) {
    renameError.value = 'New batch name must differ from the current one.'
    return
  }
  renameError.value = ''
  try {
    await api.put('/interns/batches/rename', { oldBatch, newBatch })
    renameDialog.value = false
    await loadData()
  } catch (e) {
    renameError.value = e.response?.data?.error || 'Could not rename batch.'
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Training Batches</h1>
      <p class="subtitle">Overview of every intern cohort, its tracks, trainings, and progress.</p>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>

    <!-- KPI stat tiles -->
    <section class="stat-grid section">
      <div class="stat-tile">
        <span class="stat-label">Total batches</span>
        <span class="stat-value">{{ totalBatches }}</span>
      </div>
      <div class="stat-tile">
        <span class="stat-label">Total interns</span>
        <span class="stat-value">{{ totalInterns }}</span>
      </div>
      <div class="stat-tile">
        <span class="stat-label">Overall avg completion</span>
        <span class="stat-value">{{ overallCompletion }}%</span>
      </div>
    </section>

    <p v-if="loading" class="muted">Loading…</p>

    <div v-else-if="batches.length === 0" class="card section empty-state">
      <p class="muted">No training batches yet. Add interns with a batch on the Interns page.</p>
    </div>

    <div v-else class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th class="col-caret"></th>
            <th>Batch</th>
            <th>Interns</th>
            <th>Tracks</th>
            <th>Trainings</th>
            <th>Assignments</th>
            <th>Avg completion</th>
            <th>Avg score</th>
            <th v-if="isAdmin" class="col-action"></th>
          </tr>
        </thead>
        <tbody>
          <template v-for="b in batches" :key="b.batch">
            <tr class="batch-row" @click="toggle(b.batch)">
              <td class="col-caret">
                <span class="caret" :class="{ open: expanded.has(b.batch) }">▶</span>
              </td>
              <td class="batch-name">{{ b.batch }}</td>
              <td>{{ b.internCount }}</td>
              <td>
                <span v-if="b.tracks.length" class="tracks">{{ b.tracks.join(', ') }}</span>
                <span v-else class="muted">—</span>
              </td>
              <td>{{ b.trainingCount }}</td>
              <td>{{ b.assignmentCount }}</td>
              <td>
                <div class="meter-cell">
                  <div class="meter">
                    <div class="meter-fill" :style="{ width: b.avgCompletionPercentage + '%' }"></div>
                  </div>
                  <span class="meter-pct">{{ b.avgCompletionPercentage }}%</span>
                </div>
              </td>
              <td>{{ b.avgScorePercentage }}%</td>
              <td v-if="isAdmin" class="col-action">
                <button class="btn btn--ghost" @click.stop="openRename(b.batch)">Rename</button>
              </td>
            </tr>
            <tr v-if="expanded.has(b.batch)" class="detail-row">
              <td :colspan="isAdmin ? 9 : 8">
                <div class="detail-panel">
                  <p v-if="internsIn(b.batch).length === 0" class="muted">No interns in this batch.</p>
                  <ul v-else class="intern-list">
                    <li v-for="i in internsIn(b.batch)" :key="i.id">
                      <router-link :to="`/interns/${i.id}/progress`">{{ i.name }}</router-link>
                      <span v-if="i.track" class="muted"> · {{ i.track }}</span>
                    </li>
                  </ul>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <!-- Rename batch (ADMIN) -->
    <Modal :open="renameDialog" title="Rename batch" @close="renameDialog = false">
      <p class="muted modal-hint">
        Renames the batch for every intern in the cohort, and updates any assignments
        that target it. This does not delete anything.
      </p>
      <p v-if="renameError" class="error">{{ renameError }}</p>

      <div class="field">
        <label for="rename-old">Current name</label>
        <input id="rename-old" class="input" :value="renameForm.oldBatch" disabled />
      </div>
      <div class="field">
        <label for="rename-new">New name</label>
        <input
          id="rename-new"
          class="input"
          v-model="renameForm.newBatch"
          placeholder="e.g. 2026-Q2"
          required
        />
      </div>

      <template #footer>
        <button class="btn btn--secondary" @click="renameDialog = false">Cancel</button>
        <button class="btn btn--primary" @click="saveRename">Save</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.empty-state {
  text-align: center;
}
.col-caret {
  width: 1%;
  white-space: nowrap;
}
.col-action {
  text-align: right;
  width: 1%;
  white-space: nowrap;
}
.batch-row {
  cursor: pointer;
}
.batch-row:hover {
  background: var(--layer-hover, #e8e8e8);
}
.batch-name {
  font-weight: 600;
}
.caret {
  display: inline-block;
  transition: transform 0.15s ease;
  color: var(--text-secondary, #6f6f6f);
  font-size: 11px;
}
.caret.open {
  transform: rotate(90deg);
}
.tracks {
  font-size: 13px;
}
.meter-cell {
  display: flex;
  align-items: center;
  gap: var(--sp-02);
}
.meter-cell .meter {
  flex: 1 1 auto;
  min-width: 80px;
}
.meter-pct {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}
.detail-row td {
  background: var(--layer, #f4f4f4);
  padding: 0;
}
.detail-panel {
  padding: var(--sp-03);
}
.intern-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--sp-02);
}
.intern-list li {
  font-size: 14px;
}
.modal-hint {
  margin-bottom: var(--sp-03);
}
</style>
