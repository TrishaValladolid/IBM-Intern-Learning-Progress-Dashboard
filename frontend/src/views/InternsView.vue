<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import auth from '../services/auth'
import Modal from '../components/Modal.vue'

const isAdmin = auth.isAdmin

// Lifecycle statuses. API/DB use the uppercase enum names; the UI shows the
// friendly labels. ARCHIVE_STATUSES excludes ACTIVE — batch archive cannot
// reactivate a cohort (that is a per-intern action).
const STATUSES = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'OFFBOARDED', label: 'Offboarded' },
  { value: 'CONVERTED_TO_EMPLOYEE', label: 'Converted to Employee' },
  { value: 'WITHDRAWN', label: 'Withdrawn' },
  { value: 'TERMINATED', label: 'Terminated' },
]
const ARCHIVE_STATUSES = STATUSES.filter((s) => s.value !== 'ACTIVE')

function statusLabel(value) {
  const s = STATUSES.find((x) => x.value === value)
  return s ? s.label : 'Active'
}
// CSS class carrying the status colour, e.g. "status-active".
function statusClass(value) {
  return 'status-' + (value || 'ACTIVE').toLowerCase()
}

const interns = ref([])
const emptyForm = () => ({
  name: '', talentId: '', batch: '', track: '', status: 'ACTIVE',
  totalHoursRequired: '', expectedGraduationDate: '', expectedInternshipEndDate: '',
  school: '', course: '',
})
const form = ref(emptyForm())
const loading = ref(false)
const error = ref('')

// ---- Filters ----
// Batch is derived from the interns themselves; assignment + attendance need the
// grade cells and per-intern attendance, loaded once and joined client-side.
const assignments = ref([]) // [{ id, title, maxScore }] for the assignment dropdown
const internsByAssignment = ref(new Map()) // assignmentId -> Set(internId with a grade)
const attendanceByIntern = ref(new Map()) // internId -> attendance percentage
const filterBatch = ref('')
const filterAssignment = ref('')
const filterAttendance = ref('') // '' | 'good' (>=75%) | 'low' (<75%)
const filterStatus = ref('') // '' = All Statuses; otherwise a Status enum value (ADMIN only)

// ---- Assign training to a batch ----
const batchDialog = ref(false)
const batchForm = ref({ trainingName: '', repoUrl: '', batch: '' })
const batchError = ref('')
const batchResult = ref('')

// ---- Archive a batch (ADMIN) ----
// Two-step: pick batch + archival status, then confirm the stated count.
const archiveDialog = ref(false)
const archiveForm = ref({ batch: '', status: '' })
const archiveError = ref('')
const archiveResult = ref('')
const archiveConfirm = ref(false)

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
    // Load the extra data the assignment/attendance filters need. Non-fatal:
    // the roster still renders (and the batch filter still works) if these fail.
    await loadFilterData()
  } catch (e) {
    error.value = 'Could not load interns. Is the backend running?'
  } finally {
    loading.value = false
  }
}

// Loads the data behind the assignment + attendance filters:
//  - assignments feed the assignment dropdown,
//  - /submissions (flat grade cells) tell us which interns have a grade per assignment,
//  - /attendance/summary (per intern) gives the attendance % used for the status buckets.
async function loadFilterData() {
  try {
    const [assignmentsRes, submissionsRes] = await Promise.all([
      api.get('/assignments'),
      api.get('/submissions'),
    ])
    assignments.value = assignmentsRes.data
    const byAssignment = new Map()
    for (const s of submissionsRes.data) {
      if (!byAssignment.has(s.assignmentId)) byAssignment.set(s.assignmentId, new Set())
      byAssignment.get(s.assignmentId).add(String(s.internId))
    }
    internsByAssignment.value = byAssignment

    // Attendance has no bulk endpoint, so fan out one call per intern.
    const attMap = new Map()
    await Promise.all(
      interns.value.map(async (i) => {
        try {
          const res = await api.get(`/attendance/summary?internId=${i.id}`)
          attMap.set(String(i.id), res.data.attendancePercentage ?? 0)
        } catch (e) {
          attMap.set(String(i.id), 0)
        }
      }),
    )
    attendanceByIntern.value = attMap
  } catch (e) {
    // Leave the filters with whatever data loaded; batch filter is unaffected.
  }
}

// Interns after applying the batch / assignment / attendance filters (AND-combined).
const filteredInterns = computed(() =>
  interns.value.filter((i) => {
    if (filterBatch.value && i.batch !== filterBatch.value) return false
    if (filterStatus.value && (i.status || 'ACTIVE') !== filterStatus.value) return false
    if (filterAssignment.value) {
      const ids = internsByAssignment.value.get(Number(filterAssignment.value))
      if (!ids || !ids.has(String(i.id))) return false
    }
    if (filterAttendance.value) {
      const pct = attendanceByIntern.value.get(String(i.id)) ?? 0
      if (filterAttendance.value === 'good' && pct < 75) return false
      if (filterAttendance.value === 'low' && pct >= 75) return false
    }
    return true
  }),
)

function resetFilters() {
  filterBatch.value = ''
  filterAssignment.value = ''
  filterAttendance.value = ''
  filterStatus.value = ''
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

// Number of interns the archive will affect (whole cohort, any current status).
const archiveCount = computed(() =>
  archiveForm.value.batch
    ? interns.value.filter((i) => i.batch === archiveForm.value.batch).length
    : 0,
)

function openArchiveDialog() {
  archiveForm.value = { batch: '', status: '' }
  archiveError.value = ''
  archiveResult.value = ''
  archiveConfirm.value = false
  archiveDialog.value = true
}

// Step 1 -> confirmation: validate the choices, then show the count-affected banner.
function reviewArchive() {
  if (!archiveForm.value.batch) {
    archiveError.value = 'Select a batch.'
    return
  }
  if (!archiveForm.value.status) {
    archiveError.value = 'Select a status.'
    return
  }
  archiveError.value = ''
  archiveConfirm.value = true
}

// Step 2: apply the archive. Records are never deleted — the backend moves the
// whole cohort to the chosen status.
async function confirmArchive() {
  try {
    const res = await api.post('/interns/batch/archive', {
      batch: archiveForm.value.batch,
      status: archiveForm.value.status,
    })
    const r = res.data
    archiveResult.value =
      `Batch ${r.batch}: ${r.internsUpdated} intern(s) set to ${statusLabel(r.status)}.`
    archiveConfirm.value = false
    await loadInterns()
  } catch (e) {
    archiveError.value = 'Could not archive batch.'
    archiveConfirm.value = false
  }
}

// Change one intern's status inline (ADMIN). Optimistic, reverting on failure.
async function changeStatus(intern, status) {
  if (!status || status === intern.status) return
  const previous = intern.status
  try {
    await api.put(`/interns/${intern.id}/status`, { status })
    intern.status = status
  } catch (e) {
    intern.status = previous
    error.value = 'Could not update intern status.'
  }
}

async function addIntern() {
  if (!form.value.name) return
  try {
    await api.post('/interns', form.value)
    form.value = emptyForm()
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
        <div v-if="isAdmin" class="header-actions">
          <button class="btn btn--secondary" @click="openArchiveDialog">
            Archive Batch
          </button>
          <button class="btn btn--primary" @click="openBatchDialog">
            Assign Training to Batch
          </button>
        </div>
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
        <div class="field">
          <label for="intern-status">Status</label>
          <select id="intern-status" class="select" v-model="form.status">
            <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
        <div class="field">
          <label for="intern-hours">Required hours</label>
          <input id="intern-hours" class="input" v-model.number="form.totalHoursRequired" type="number" min="0" placeholder="e.g. 600" />
        </div>
        <div class="field">
          <label for="intern-graduation">Expected graduation</label>
          <input id="intern-graduation" class="input" v-model="form.expectedGraduationDate" type="date" />
        </div>
        <div class="field">
          <label for="intern-end">Expected internship end</label>
          <input id="intern-end" class="input" v-model="form.expectedInternshipEndDate" type="date" />
        </div>
        <div class="field">
          <label for="intern-school">School</label>
          <input id="intern-school" class="input" v-model="form.school" placeholder="School name" maxlength="255" />
        </div>
        <div class="field">
          <label for="intern-course">Course</label>
          <input id="intern-course" class="input" v-model="form.course" placeholder="Course or program" maxlength="255" />
        </div>
        <div class="field field--action">
          <button type="submit" class="btn btn--primary">Add Intern</button>
        </div>
      </form>
    </section>

    <p v-if="loading" class="muted">Loading…</p>
    <template v-else>
      <!-- Filters: batch / assignment / attendance status (AND-combined) -->
      <section class="card section filter-bar">
        <div class="field">
          <label for="filter-batch">Batch</label>
          <select id="filter-batch" class="select" v-model="filterBatch">
            <option value="">All batches</option>
            <option v-for="b in batches" :key="b" :value="b">{{ b }}</option>
          </select>
        </div>
        <div class="field">
          <label for="filter-assignment">Assignment</label>
          <select id="filter-assignment" class="select" v-model="filterAssignment">
            <option value="">All assignments</option>
            <option v-for="a in assignments" :key="a.id" :value="a.id">{{ a.title }}</option>
          </select>
        </div>
        <div class="field">
          <label for="filter-attendance">Attendance status</label>
          <select id="filter-attendance" class="select" v-model="filterAttendance">
            <option value="">All attendance</option>
            <option value="good">Good (≥ 75%)</option>
            <option value="low">Low (&lt; 75%)</option>
          </select>
        </div>
        <div v-if="isAdmin" class="field">
          <label for="filter-status">Status</label>
          <select id="filter-status" class="select" v-model="filterStatus">
            <option value="">All Statuses</option>
            <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
        <div class="field field--action">
          <button class="btn btn--ghost" @click="resetFilters">Clear</button>
        </div>
      </section>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Name</th><th>Talent ID</th><th>Batch</th><th>Track</th><th v-if="isAdmin">Status</th><th v-if="isAdmin" class="col-action"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredInterns.length === 0">
              <td :colspan="isAdmin ? 6 : 5" class="muted">No interns match the current filters.</td>
            </tr>
            <tr v-for="i in filteredInterns" :key="i.id">
              <td>
                <router-link :to="`/interns/${i.id}/progress`">{{ i.name }}</router-link>
              </td>
              <td>{{ i.talentId }}</td>
              <td>{{ i.batch }}</td>
              <td>{{ i.track }}</td>
              <td v-if="isAdmin" class="col-status">
                <span class="status-dot" :class="statusClass(i.status)"></span>
                <select
                  class="select status-select"
                  :class="statusClass(i.status)"
                  :value="i.status || 'ACTIVE'"
                  @change="changeStatus(i, $event.target.value)"
                >
                  <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
                </select>
              </td>
              <td v-if="isAdmin" class="col-action">
                <button class="btn btn--danger" @click="removeIntern(i.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

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

    <!-- Archive a batch (ADMIN) -->
    <Modal :open="archiveDialog" title="Archive batch" @close="archiveDialog = false">
      <p class="muted modal-hint">
        Move every intern in a batch to an archival status. Records are never
        deleted — archived interns stay visible via the Status filter.
      </p>
      <p v-if="archiveError" class="error">{{ archiveError }}</p>
      <p v-if="archiveResult" class="batch-result">{{ archiveResult }}</p>

      <template v-if="!archiveResult">
        <div class="field">
          <label for="archive-batch">Batch</label>
          <select id="archive-batch" class="select" v-model="archiveForm.batch" :disabled="archiveConfirm">
            <option value="" disabled>Select a batch</option>
            <option v-for="b in batches" :key="b" :value="b">{{ b }}</option>
          </select>
          <p v-if="batches.length === 0" class="muted">No batches found. Add interns with a batch first.</p>
        </div>
        <div class="field">
          <label for="archive-status">Archival status</label>
          <select id="archive-status" class="select" v-model="archiveForm.status" :disabled="archiveConfirm">
            <option value="" disabled>Select a status</option>
            <option v-for="s in ARCHIVE_STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>

        <p v-if="archiveConfirm" class="archive-confirm">
          This will set <strong>{{ archiveCount }}</strong> intern(s) in batch
          <strong>{{ archiveForm.batch }}</strong> to
          <strong>{{ statusLabel(archiveForm.status) }}</strong>. Continue?
        </p>
      </template>

      <template #footer>
        <button class="btn btn--secondary" @click="archiveDialog = false">
          {{ archiveResult ? 'Close' : 'Cancel' }}
        </button>
        <button v-if="!archiveResult && !archiveConfirm" class="btn btn--primary" @click="reviewArchive">
          Review
        </button>
        <button v-if="!archiveResult && archiveConfirm" class="btn btn--danger" @click="confirmArchive">
          Archive {{ archiveCount }} intern(s)
        </button>
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
.filter-bar {
  display: flex;
  gap: var(--sp-02);
  flex-wrap: wrap;
  align-items: flex-end;
}
.filter-bar .field {
  flex: 1 1 180px;
  min-width: 160px;
}
.filter-bar .field--action {
  flex: 0 0 auto;
  min-width: 0;
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
.header-actions {
  display: flex;
  gap: var(--sp-02);
  flex-wrap: wrap;
}
.archive-confirm {
  margin-top: var(--sp-03);
}

/* ---- Status column (ADMIN) ---- */
.col-status {
  white-space: nowrap;
}
.status-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: var(--sp-01);
  vertical-align: middle;
  background: var(--status-color, #24a148);
}
.status-select {
  display: inline-block;
  width: auto;
  min-width: 160px;
  vertical-align: middle;
}
/* One accent per lifecycle status, applied to both the dot and the select. */
.status-active {
  --status-color: #24a148;
}
.status-offboarded {
  --status-color: #4589ff;
}
.status-converted_to_employee {
  --status-color: #8a3ffc;
}
.status-withdrawn {
  --status-color: #f1c21b;
}
.status-terminated {
  --status-color: #da1e28;
}
.status-dot.status-active,
.status-dot.status-offboarded,
.status-dot.status-converted_to_employee,
.status-dot.status-withdrawn,
.status-dot.status-terminated {
  background: var(--status-color);
}
.status-select.status-active,
.status-select.status-offboarded,
.status-select.status-converted_to_employee,
.status-select.status-withdrawn,
.status-select.status-terminated {
  border-left: 3px solid var(--status-color);
}
</style>
