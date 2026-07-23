<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'

const assignments = ref([])
const interns = ref([])
const grades = ref([])
const form = ref({ title: '', maxScore: 100, batch: '' })
const error = ref('')

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

async function addAssignment() {
  if (!form.value.title) return
  await api.post('/assignments', form.value)
  form.value = { title: '', maxScore: 100, batch: '' }
  await loadMatrix()
}

async function removeAssignment(id) {
  await api.delete(`/assignments/${id}`)
  await loadMatrix()
}

onMounted(loadMatrix)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Assignments and Grades</h1>
      <p class="subtitle">
        Every intern's score per assignment. Click an intern's name to open their profile and record or change a grade.
      </p>
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
                  <button class="btn btn--ghost btn--sm" @click="removeAssignment(a.id)">Delete</button>
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
</style>
