<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import Modal from '../components/Modal.vue'

const assignments = ref([])
const interns = ref([])
const grades = ref([])
const error = ref('')

// Manage-assignments dialog state. `drafts` is an editable clone of the
// assignments list so edits stay local until Save; `newForm` backs the add row.
const showManage = ref(false)
const drafts = ref([])
const newForm = ref({ title: '', maxScore: 100, batch: '' })

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

async function loadMatrix() {
  try {
    const [aRes, iRes, gRes] = await Promise.all([
      api.get('/assignments'),
      api.get('/interns'),
      api.get('/submissions'),
    ])
    assignments.value = aRes.data
    interns.value = iRes.data
    grades.value = gRes.data
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
  }))
  newForm.value = { title: '', maxScore: 100, batch: '' }
  showManage.value = true
}

async function addAssignment() {
  if (!newForm.value.title) return
  await api.post('/assignments', newForm.value)
  newForm.value = { title: '', maxScore: 100, batch: '' }
  await loadMatrix()
  openManage()
}

async function saveAssignment(draft) {
  await api.put(`/assignments/${draft.id}`, {
    title: draft.title,
    maxScore: draft.maxScore,
    batch: draft.batch,
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

    <section class="section">
      <div class="table-wrap">
        <table class="data-table grade-matrix">
          <thead>
            <tr>
              <th class="col-intern">Intern</th>
              <th v-for="a in assignments" :key="a.id" class="col-grade">
                <div class="assignment-head">
                  <span class="assignment-title">{{ a.title }}</span>
                  <span class="muted assignment-max">max {{ a.maxScore }}</span>
                </div>
              </th>
              <th v-if="assignments.length === 0" class="muted">No assignments yet.</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="interns.length === 0">
              <td :colspan="assignments.length + 1" class="muted">No interns yet.</td>
            </tr>
            <tr v-for="i in interns" :key="i.id">
              <td class="col-intern">
                <router-link :to="`/interns/${i.id}/progress`">{{ i.name }}</router-link>
              </td>
              <td v-for="a in assignments" :key="a.id" class="col-grade">
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
        <div class="field field--max">
          <label for="a-max">Max score</label>
          <input id="a-max" class="input" v-model.number="newForm.maxScore" type="number" />
        </div>
        <div class="field">
          <label for="a-batch">Batch</label>
          <input id="a-batch" class="input" v-model="newForm.batch" placeholder="e.g. 2026-Q1" />
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
          <div class="field field--max">
            <input class="input" v-model.number="d.maxScore" type="number" />
          </div>
          <div class="field">
            <input class="input" v-model="d.batch" placeholder="Batch" />
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
.assignment-max {
  font-weight: 400;
  font-size: 12px;
}
.btn--sm {
  padding: 2px 8px;
  font-size: 12px;
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
.manage-row-actions {
  flex: 0 0 auto;
  display: flex;
  gap: var(--sp-01);
}
</style>
