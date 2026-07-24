<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import Modal from '../components/Modal.vue'

const assignments = ref([])
const interns = ref([])
const grades = ref([])
const trainingNames = ref([]) // distinct trainings an assignment can belong to
const error = ref('')

// ---- Filters (batch / assignment / date) ----
// Batch filters the intern rows; assignment + date filter the assignment columns.
const filterBatch = ref('')
const filterAssignment = ref('')
const filterDate = ref('')

// Manage-assignments dialog state. `drafts` is an editable clone of the
// assignments list so edits stay local until Save; `newForm` backs the add row.
const showManage = ref(false)
const drafts = ref([])
const newForm = ref({ title: '', maxScore: 100, batch: '', trainingName: '', repoUrl: '', dueDate: '' })

function blankForm() {
  return { title: '', maxScore: 100, batch: '', trainingName: '', repoUrl: '', dueDate: '' }
}

// Grade lookup keyed by "internId-assignmentId" so a cell can find its score
// in O(1) while rendering the matrix.
const gradeMap = computed(() => {
  const map = {}
  for (const g of grades.value) {
    map[`${g.internId}-${g.assignmentId}`] = g.score
  }
  return map
})

function scoreFor(internId, assignmentId) {
  const s = gradeMap.value[`${internId}-${assignmentId}`]
  return s === undefined || s === null ? '—' : s
}

// Distinct, non-empty batches from the interns (for the batch filter).
const batches = computed(() => {
  const set = new Set(interns.value.map((i) => i.batch).filter((b) => b && b.trim()))
  return [...set].sort()
})

// Distinct, non-empty due dates from the assignments (for the date filter).
const dueDates = computed(() => {
  const set = new Set(assignments.value.map((a) => a.dueDate).filter((d) => d && d.trim()))
  return [...set].sort()
})

// Columns after applying the assignment + date filters (AND-combined).
const filteredAssignments = computed(() =>
  assignments.value.filter((a) => {
    if (filterAssignment.value && a.id !== Number(filterAssignment.value)) return false
    if (filterDate.value && a.dueDate !== filterDate.value) return false
    return true
  }),
)

// Rows after applying the batch filter.
const filteredInterns = computed(() =>
  interns.value.filter((i) => !filterBatch.value || i.batch === filterBatch.value),
)

function resetFilters() {
  filterBatch.value = ''
  filterAssignment.value = ''
  filterDate.value = ''
}

async function loadMatrix() {
  try {
    const [aRes, iRes, gRes, tRes] = await Promise.all([
      api.get('/assignments'),
      api.get('/interns'),
      api.get('/submissions'),
      api.get('/interns/trainings/names'),
    ])
    assignments.value = aRes.data
    interns.value = iRes.data
    grades.value = gRes.data
    trainingNames.value = tRes.data
  } catch (e) {
    error.value = 'Could not load grades. Is the backend running?'
  }
}

function openManage() {
  // Clone so editing inputs never mutate the matrix until we reload.
  drafts.value = assignments.value.map((a) => ({
    id: a.id,
    title: a.title,
    maxScore: a.maxScore,
    batch: a.batch,
    trainingName: a.trainingName || '',
    repoUrl: a.repoUrl || '',
    dueDate: a.dueDate || '',
  }))
  newForm.value = blankForm()
  showManage.value = true
}

async function addAssignment() {
  if (!newForm.value.title) return
  await api.post('/assignments', newForm.value)
  newForm.value = blankForm()
  await loadMatrix()
  openManage()
}

async function saveAssignment(draft) {
  await api.put(`/assignments/${draft.id}`, {
    title: draft.title,
    maxScore: draft.maxScore,
    batch: draft.batch,
    trainingName: draft.trainingName,
    repoUrl: draft.repoUrl,
    dueDate: draft.dueDate,
  })
  await loadMatrix()
}

async function removeAssignment(id) {
  await api.delete(`/assignments/${id}`)
  await loadMatrix()
  openManage()
}

onMounted(loadMatrix)
</script>

<template>
  <div class="page">
    <div class="page-header page-header--row">
      <div>
        <h1>Assignments and Grades</h1>
        <p class="subtitle">
          Every intern's score per assignment. Click an intern's name to open their profile and record or change a grade.
        </p>
      </div>
      <button class="btn btn--primary" @click="openManage">Manage assignments</button>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>

    <!-- Filters: batch (rows) / assignment + date (columns), AND-combined -->
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
        <label for="filter-date">Due date</label>
        <select id="filter-date" class="select" v-model="filterDate">
          <option value="">All dates</option>
          <option v-for="d in dueDates" :key="d" :value="d">{{ d }}</option>
        </select>
      </div>
      <div class="field field--action">
        <button class="btn btn--ghost" @click="resetFilters">Clear</button>
      </div>
    </section>

    <section class="section">
      <div class="table-wrap">
        <table class="data-table grade-matrix">
          <thead>
            <tr>
              <th class="col-intern">Intern</th>
              <th v-for="a in filteredAssignments" :key="a.id" class="col-grade">
                <div class="assignment-head">
                  <span class="assignment-title">{{ a.title }}</span>
                  <span v-if="a.trainingName" class="muted assignment-training">{{ a.trainingName }}</span>
                  <span class="muted assignment-max">max {{ a.maxScore }}</span>
                  <span v-if="a.dueDate" class="muted assignment-due">due {{ a.dueDate }}</span>
                  <a
                    v-if="a.repoUrl"
                    :href="a.repoUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="assignment-link"
                  >🔗 Box</a>
                </div>
              </th>
              <th v-if="filteredAssignments.length === 0" class="muted">No assignments match.</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredInterns.length === 0">
              <td :colspan="filteredAssignments.length + 1" class="muted">No interns match the current filters.</td>
            </tr>
            <tr v-for="i in filteredInterns" :key="i.id">
              <td class="col-intern">
                <router-link :to="`/interns/${i.id}/progress`">{{ i.name }}</router-link>
              </td>
              <td v-for="a in filteredAssignments" :key="a.id" class="col-grade">
                {{ scoreFor(i.id, a.id) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <Modal :open="showManage" title="Manage assignments" @close="showManage = false">
      <form class="manage-add" @submit.prevent="addAssignment">
        <div class="field">
          <label for="a-title">Title</label>
          <input id="a-title" class="input" v-model="newForm.title" placeholder="Assignment title" required />
        </div>
        <div class="field">
          <label for="a-training">Training</label>
          <select id="a-training" class="select" v-model="newForm.trainingName">
            <option value="">None (independent)</option>
            <option v-for="t in trainingNames" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="field field--max">
          <label for="a-max">Max score</label>
          <input id="a-max" class="input" v-model.number="newForm.maxScore" type="number" />
        </div>
        <div class="field">
          <label for="a-batch">Batch</label>
          <input id="a-batch" class="input" v-model="newForm.batch" placeholder="e.g. 2026-Q1" />
        </div>
        <div class="field field--date">
          <label for="a-due">Due date</label>
          <input id="a-due" class="input" v-model="newForm.dueDate" type="date" />
        </div>
        <div class="field">
          <label for="a-box">Box Drive link</label>
          <input id="a-box" class="input" v-model="newForm.repoUrl" placeholder="https://app.box.com/…" />
        </div>
        <div class="field field--action">
          <button type="submit" class="btn btn--primary">Add</button>
        </div>
      </form>

      <p v-if="drafts.length === 0" class="muted manage-empty">No assignments yet. Add one above.</p>

      <ul v-else class="manage-list">
        <li v-for="d in drafts" :key="d.id" class="manage-row">
          <div class="field">
            <input class="input" v-model="d.title" placeholder="Title" />
          </div>
          <div class="field">
            <select class="select" v-model="d.trainingName">
              <option value="">None (independent)</option>
              <option v-for="t in trainingNames" :key="t" :value="t">{{ t }}</option>
            </select>
          </div>
          <div class="field field--max">
            <input class="input" v-model.number="d.maxScore" type="number" />
          </div>
          <div class="field">
            <input class="input" v-model="d.batch" placeholder="Batch" />
          </div>
          <div class="field field--date">
            <input class="input" v-model="d.dueDate" type="date" />
          </div>
          <div class="field">
            <input class="input" v-model="d.repoUrl" placeholder="Box Drive link" />
          </div>
          <div class="manage-row-actions">
            <button class="btn btn--secondary btn--sm" @click="saveAssignment(d)">Save</button>
            <button class="btn btn--danger btn--sm" @click="removeAssignment(d.id)">Delete</button>
          </div>
        </li>
      </ul>

      <template #footer>
        <button class="btn btn--secondary" @click="showManage = false">Done</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.page-header--row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-03);
}
.grade-matrix .col-intern {
  white-space: nowrap;
}
.grade-matrix .col-grade {
  text-align: center;
}
.assignment-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.assignment-max,
.assignment-training,
.assignment-due {
  font-weight: 400;
  font-size: 12px;
}
.assignment-training {
  font-weight: 600;
}
.assignment-link {
  font-size: 12px;
  color: var(--blue-60, #0f62fe);
  text-decoration: none;
}
.assignment-link:hover {
  text-decoration: underline;
}
.btn--sm {
  padding: 2px 8px;
  font-size: 12px;
}

/* Filters: batch / assignment / date (AND-combined) */
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

/* Manage dialog: add row on top, editable list below. */
.manage-add {
  display: flex;
  gap: var(--sp-02);
  flex-wrap: wrap;
  align-items: flex-end;
  padding-bottom: var(--sp-03);
  border-bottom: 1px solid var(--border);
}
.manage-add .field {
  flex: 1 1 140px;
  min-width: 120px;
}
.manage-add .field--max {
  flex: 0 0 90px;
  min-width: 0;
}
.manage-add .field--date {
  flex: 0 0 150px;
  min-width: 0;
}
.manage-add .field--action {
  flex: 0 0 auto;
  min-width: 0;
}
.manage-empty {
  padding-top: var(--sp-03);
}
.manage-list {
  list-style: none;
  margin: 0;
  padding: var(--sp-02) 0 0;
  display: flex;
  flex-direction: column;
  gap: var(--sp-02);
}
.manage-row {
  display: flex;
  gap: var(--sp-02);
  align-items: flex-end;
}
.manage-row .field {
  flex: 1 1 120px;
  min-width: 100px;
}
.manage-row .field--max {
  flex: 0 0 80px;
  min-width: 0;
}
.manage-row .field--date {
  flex: 0 0 150px;
  min-width: 0;
}
.manage-row-actions {
  flex: 0 0 auto;
  display: flex;
  gap: var(--sp-01);
}
</style>
