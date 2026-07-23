<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import api from '../api/client'
import auth from '../services/auth'
import Modal from '../components/Modal.vue'

const isAdmin = auth.isAdmin
const isTrainer = auth.isTrainer

const STATUSES = ['PRESENT', 'LATE', 'ABSENT']

// ---- Roster state ----
const trainings = ref([])
const training = ref('')
const date = ref(todayIso())
const roster = ref([]) // [{ internId, name, talentId, track, status }]
const search = ref('')
const loading = ref(false)
const error = ref('')

// ---- Summary tiles ----
const summary = ref({ totalPresent: 0, totalLate: 0, totalAbsent: 0, attendancePercentage: 0 })

// ---- Save state ----
const saving = ref(false)
const saveMessage = ref('')

function todayIso() {
  // Local calendar date (not UTC) so "today" matches the trainer's timezone.
  const d = new Date()
  const off = d.getTimezoneOffset()
  return new Date(d.getTime() - off * 60000).toISOString().slice(0, 10)
}

// Attendance is considered "already recorded" when any intern carries a status.
const alreadyRecorded = computed(() => roster.value.some((r) => !!r.status))

const filteredRoster = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return roster.value
  return roster.value.filter((r) => {
    const name = (r.name || '').toLowerCase()
    const tid = (r.talentId || '').toLowerCase()
    return name.includes(term) || tid.includes(term)
  })
})

function statusLabel(s) {
  if (!s) return '—'
  return s.charAt(0) + s.slice(1).toLowerCase()
}

function statusClass(s) {
  if (s === 'PRESENT') return 'status-pill--present'
  if (s === 'LATE') return 'status-pill--late'
  if (s === 'ABSENT') return 'status-pill--absent'
  return ''
}

function formatDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (isNaN(d.getTime())) return value
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function serverError(e, fallback) {
  if (e.response && e.response.data && e.response.data.error) return e.response.data.error
  return fallback
}

// ---- Load ----
async function loadTrainings() {
  try {
    const res = await api.get('/attendance/trainings')
    trainings.value = res.data
    // Default to the first training so the roster can auto-load on open.
    if (!training.value && trainings.value.length > 0) {
      training.value = trainings.value[0]
    }
  } catch (e) {
    if (e.response && e.response.status === 403) {
      error.value = 'You do not have permission to view attendance.'
    } else {
      error.value = 'Could not load trainings. Is the backend running?'
    }
  }
}

async function loadRoster() {
  if (!training.value || !date.value) {
    roster.value = []
    return
  }
  loading.value = true
  error.value = ''
  saveMessage.value = ''
  try {
    const res = await api.get('/attendance/roster', {
      params: { batch: training.value, date: date.value },
    })
    roster.value = res.data
  } catch (e) {
    if (e.response && e.response.status === 403) {
      error.value = 'You do not have permission to view attendance.'
    } else {
      error.value = serverError(e, 'Could not load the roster. Is the backend running?')
    }
    roster.value = []
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try {
    const res = await api.get('/attendance/summary')
    summary.value = res.data
  } catch (e) {
    // Summary is non-blocking; leave the tiles at their defaults on failure.
  }
}

// Auto-load the roster whenever the training or date changes.
watch([training, date], loadRoster)

// ---- Mark / Save (TRAINER) ----
function setStatus(entry, status) {
  if (!isTrainer.value) return
  entry.status = status
  saveMessage.value = ''
}

function markAllPresent() {
  if (!isTrainer.value) return
  roster.value.forEach((r) => {
    r.status = 'PRESENT'
  })
  saveMessage.value = ''
}

async function save() {
  if (!isTrainer.value) return
  error.value = ''
  saveMessage.value = ''
  const missing = roster.value.some((r) => !r.status)
  if (missing) {
    error.value = 'Please mark a status for every intern before saving.'
    return
  }
  if (roster.value.length === 0) {
    error.value = 'There are no interns to record for this training.'
    return
  }
  saving.value = true
  try {
    const res = await api.post('/attendance/bulk', {
      batch: training.value,
      date: date.value,
      records: roster.value.map((r) => ({ internId: r.internId, status: r.status })),
    })
    roster.value = res.data
    saveMessage.value = 'Attendance saved.'
    await loadSummary()
  } catch (e) {
    error.value = serverError(e, 'Could not save attendance.')
  } finally {
    saving.value = false
  }
}

// ---- History dialog ----
const dialog = ref(null) // 'history' | null
const historyIntern = ref(null)
const historyRows = ref([])
const historyLoading = ref(false)
const historyError = ref('')
const historySummary = ref({ totalPresent: 0, totalLate: 0, totalAbsent: 0, attendancePercentage: 0 })

function closeDialog() {
  dialog.value = null
  historyIntern.value = null
  historyError.value = ''
}

async function openHistory(entry) {
  historyIntern.value = { id: entry.internId, name: entry.name, talentId: entry.talentId }
  historyRows.value = []
  historyError.value = ''
  historyLoading.value = true
  dialog.value = 'history'
  try {
    const [hist, sum] = await Promise.all([
      api.get(`/attendance/interns/${entry.internId}/history`),
      api.get('/attendance/summary', { params: { internId: entry.internId } }),
    ])
    historyRows.value = hist.data
    historySummary.value = sum.data
  } catch (e) {
    historyError.value = serverError(e, 'Could not load attendance history.')
  } finally {
    historyLoading.value = false
  }
}

onMounted(() => {
  loadTrainings()
  loadSummary()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Attendance</h1>
      <p class="subtitle">Take attendance for a whole training in one pass — like a classroom roster.</p>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>

    <!-- Summary tiles (calculated dynamically from the database). -->
    <div class="stat-grid section">
      <div class="stat-tile">
        <div class="stat-label">Total Present</div>
        <div class="stat-value">{{ summary.totalPresent }}</div>
      </div>
      <div class="stat-tile">
        <div class="stat-label">Total Late</div>
        <div class="stat-value">{{ summary.totalLate }}</div>
      </div>
      <div class="stat-tile">
        <div class="stat-label">Total Absent</div>
        <div class="stat-value">{{ summary.totalAbsent }}</div>
      </div>
      <div class="stat-tile">
        <div class="stat-label">Attendance %</div>
        <div class="stat-value">{{ summary.attendancePercentage }}%</div>
      </div>
    </div>

    <!-- Training + Date + Search controls. -->
    <div class="toolbar section">
      <div class="field">
        <label for="att-training">Training</label>
        <select id="att-training" class="select" v-model="training">
          <option value="">Select a training</option>
          <option v-for="t in trainings" :key="t" :value="t">{{ t }}</option>
        </select>
      </div>
      <div class="field">
        <label for="att-date">Date</label>
        <input id="att-date" class="input" type="date" v-model="date" />
      </div>
      <div class="field search-field">
        <label for="att-search">Search</label>
        <input
          id="att-search"
          class="input"
          v-model="search"
          placeholder="Search by name or intern ID"
        />
      </div>
    </div>

    <p v-if="alreadyRecorded && !loading" class="recorded-banner">
      Attendance already recorded for this date{{ isTrainer ? ' — editing.' : '.' }}
    </p>

    <!-- Roster actions (TRAINER only). -->
    <div v-if="isTrainer && roster.length" class="roster-actions section">
      <button class="btn btn--secondary" @click="markAllPresent">Mark All Present</button>
      <div class="spacer"></div>
      <span v-if="saveMessage" class="save-message">{{ saveMessage }}</span>
      <button class="btn btn--primary" :disabled="saving" @click="save">
        {{ saving ? 'Saving…' : 'Save Attendance' }}
      </button>
    </div>

    <p v-if="loading" class="muted">Loading…</p>
    <p v-else-if="!training" class="muted">Select a training to load its roster.</p>
    <div v-else class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>Intern ID</th>
            <th>Name</th>
            <th>Status</th>
            <th class="col-action"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="filteredRoster.length === 0">
            <td colspan="4" class="muted">No interns found for this training.</td>
          </tr>
          <tr v-for="r in filteredRoster" :key="r.internId">
            <td>{{ r.talentId || '—' }}</td>
            <td>{{ r.name }}</td>
            <td>
              <!-- TRAINER: one-click segmented control. ADMIN: read-only pill. -->
              <div v-if="isTrainer" class="segmented" role="group" aria-label="Attendance status">
                <button
                  v-for="s in STATUSES"
                  :key="s"
                  type="button"
                  class="seg-btn"
                  :class="[`seg-btn--${s.toLowerCase()}`, { 'is-active': r.status === s }]"
                  :aria-pressed="r.status === s"
                  @click="setStatus(r, s)"
                >
                  {{ statusLabel(s) }}
                </button>
              </div>
              <span v-else class="status-pill" :class="statusClass(r.status)">
                {{ statusLabel(r.status) }}
              </span>
            </td>
            <td class="col-action">
              <div class="row-actions">
                <button class="btn btn--ghost" @click="openHistory(r)">History</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Attendance history -->
    <Modal :open="dialog === 'history'" title="Attendance history" @close="closeDialog">
      <div v-if="historyIntern" class="edit-context muted">
        <span>{{ historyIntern.name }}</span>
        <span v-if="historyIntern.talentId"> · {{ historyIntern.talentId }}</span>
      </div>

      <div class="stat-grid history-summary">
        <div class="stat-tile">
          <div class="stat-label">Present</div>
          <div class="stat-value">{{ historySummary.totalPresent }}</div>
        </div>
        <div class="stat-tile">
          <div class="stat-label">Late</div>
          <div class="stat-value">{{ historySummary.totalLate }}</div>
        </div>
        <div class="stat-tile">
          <div class="stat-label">Absent</div>
          <div class="stat-value">{{ historySummary.totalAbsent }}</div>
        </div>
        <div class="stat-tile">
          <div class="stat-label">Attendance %</div>
          <div class="stat-value">{{ historySummary.attendancePercentage }}%</div>
        </div>
      </div>

      <p v-if="historyLoading" class="muted">Loading…</p>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Status</th>
              <th>Recorded By</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="historyRows.length === 0">
              <td colspan="3" class="muted">No history for this intern.</td>
            </tr>
            <tr v-for="h in historyRows" :key="h.id">
              <td>{{ formatDate(h.date) }}</td>
              <td>
                <span class="status-pill" :class="statusClass(h.status)">{{ statusLabel(h.status) }}</span>
              </td>
              <td>{{ h.recordedBy || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="historyError" class="error">{{ historyError }}</p>
      <template #footer>
        <button class="btn btn--secondary" @click="closeDialog">Close</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: var(--sp-02);
  flex-wrap: wrap;
  align-items: flex-end;
}
.search-field {
  flex: 1 1 320px;
  max-width: 420px;
}
.recorded-banner {
  margin: var(--sp-02) 0 0;
  padding: var(--sp-02);
  background: #edf5ff;
  border-left: 3px solid var(--interactive, #0f62fe);
  color: var(--text-primary);
  font-size: 13px;
}
.roster-actions {
  display: flex;
  align-items: center;
  gap: var(--sp-02);
}
.roster-actions .spacer {
  flex: 1;
}
.save-message {
  color: var(--support-success);
  font-size: 13px;
  font-weight: 600;
}
.col-action {
  text-align: right;
  width: 1%;
  white-space: nowrap;
}
.row-actions {
  display: inline-flex;
  gap: var(--sp-01);
  justify-content: flex-end;
  flex-wrap: wrap;
}

/* One-click segmented status control for trainers. */
.segmented {
  display: inline-flex;
  border: 1px solid var(--border-strong, #8d8d8d);
  border-radius: 4px;
  overflow: hidden;
}
.seg-btn {
  appearance: none;
  border: none;
  border-left: 1px solid var(--border-subtle, #e0e0e0);
  background: var(--field, #f4f4f4);
  color: var(--text-secondary, #525252);
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  cursor: pointer;
  line-height: 1.5;
}
.seg-btn:first-child {
  border-left: none;
}
.seg-btn:hover {
  background: #e8e8e8;
}
.seg-btn.is-active.seg-btn--present {
  background: #defbe6;
  color: var(--support-success);
}
.seg-btn.is-active.seg-btn--late {
  background: #fdf6dd;
  color: #8e6a00;
}
.seg-btn.is-active.seg-btn--absent {
  background: #fff1f1;
  color: var(--support-error, #da1e28);
}

.status-pill {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 12px;
  line-height: 1.5;
}
.status-pill--present {
  background: #defbe6;
  color: var(--support-success);
}
.status-pill--late {
  background: #fdf6dd;
  color: #8e6a00;
}
.status-pill--absent {
  background: #fff1f1;
  color: var(--support-error, #da1e28);
}
.edit-context {
  margin-bottom: var(--sp-02);
  font-size: 13px;
}
.history-summary {
  margin-bottom: var(--sp-03);
}
</style>
