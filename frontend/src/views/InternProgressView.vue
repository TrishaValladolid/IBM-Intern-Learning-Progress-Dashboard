<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import auth from '../services/auth'
import Modal from '../components/Modal.vue'

const route = useRoute()
const progress = ref(null)
const assignments = ref([])
const grades = ref([]) // this intern's recorded grades (GradeCell[])
const attendance = ref(null) // this intern's AttendanceSummary
const scoreForm = ref({ assignmentId: '', score: '' })
const error = ref('')
const scoreError = ref('')

// The assignment currently picked in the score form, so we can bound the score
// input by its maxScore (can't grade 100 on a 50-point assignment).
const selectedAssignment = computed(() =>
  assignments.value.find((a) => a.id === scoreForm.value.assignmentId) || null,
)

const isAdmin = auth.isAdmin

// ---- Training History ----
const trainings = ref([])
const trainingError = ref('')
const editDialog = ref(false)
const editForm = ref({ id: null, trainingName: '' })

async function loadProgress() {
  try {
    const res = await api.get(`/interns/${route.params.id}/progress`)
    progress.value = res.data
  } catch (e) {
    error.value = 'Could not load progress. Is the backend running?'
  }
}

async function loadAssignments() {
  const res = await api.get('/assignments')
  assignments.value = res.data
}

// Grades: the flat /submissions feed (GradeCell: internId, assignmentId, score)
// filtered to this intern. Joined to assignment titles/maxScore for display.
async function loadGrades() {
  const res = await api.get('/submissions')
  grades.value = res.data.filter((g) => String(g.internId) === String(route.params.id))
}

// Attendance summary for this intern (present/late/absent + percentage).
async function loadAttendance() {
  try {
    const res = await api.get(`/attendance/summary?internId=${route.params.id}`)
    attendance.value = res.data
  } catch (e) {
    // Non-fatal: the rest of the page still renders without attendance.
    attendance.value = null
  }
}

// Join each recorded grade to its assignment so we can show title + percentage.
const gradeRows = computed(() =>
  grades.value.map((g) => {
    const a = assignments.value.find((x) => x.id === g.assignmentId)
    const maxScore = a?.maxScore ?? null
    const pct =
      g.score != null && maxScore ? Math.round((g.score / maxScore) * 100) : null
    return {
      assignmentId: g.assignmentId,
      title: a?.title || `Assignment #${g.assignmentId}`,
      trainingName: a?.trainingName || 'Other',
      score: g.score,
      maxScore,
      pct,
    }
  }),
)

// Group the grade rows by their assignment's training and compute a total score
// per training (sum of scores over sum of maxScores). This gives each training a
// running "X / Y (Z%)" tally underneath its assignment scores.
const gradesByTraining = computed(() => {
  const groups = new Map()
  for (const r of gradeRows.value) {
    if (!groups.has(r.trainingName)) {
      groups.set(r.trainingName, { training: r.trainingName, rows: [], totalScore: 0, totalMax: 0 })
    }
    const grp = groups.get(r.trainingName)
    grp.rows.push(r)
    if (r.score != null) grp.totalScore += r.score
    if (r.score != null && r.maxScore) grp.totalMax += r.maxScore
  }
  return [...groups.values()].map((grp) => ({
    ...grp,
    totalPct: grp.totalMax ? Math.round((grp.totalScore / grp.totalMax) * 100) : null,
  }))
})

async function submitScore() {
  scoreError.value = ''
  if (!scoreForm.value.assignmentId || scoreForm.value.score === '') return

  // Range guard: score must be within 0..maxScore for the chosen assignment.
  const max = selectedAssignment.value?.maxScore
  if (scoreForm.value.score < 0 || (max != null && scoreForm.value.score > max)) {
    scoreError.value = `Score must be between 0 and ${max}.`
    return
  }

  try {
    await api.post('/submissions', {
      internId: route.params.id,
      assignmentId: scoreForm.value.assignmentId,
      score: scoreForm.value.score,
      status: 'GRADED',
    })
    scoreForm.value = { assignmentId: '', score: '' }
    await Promise.all([loadProgress(), loadGrades()])
  } catch (e) {
    scoreError.value = e.response?.data || 'Could not save score.'
  }
}

async function loadTrainings() {
  try {
    const res = await api.get(`/interns/${route.params.id}/trainings`)
    trainings.value = res.data
  } catch (e) {
    trainingError.value = 'Could not load training history.'
  }
}

function openEdit(t) {
  editForm.value = { id: t.id, trainingName: t.trainingName }
  editDialog.value = true
}

async function saveEdit() {
  if (!editForm.value.trainingName.trim()) return
  trainingError.value = ''
  try {
    await api.put(`/interns/${route.params.id}/trainings/${editForm.value.id}`, {
      trainingName: editForm.value.trainingName.trim(),
    })
    editDialog.value = false
    await loadTrainings()
  } catch (e) {
    trainingError.value = 'Could not update training.'
  }
}

async function removeTraining(t) {
  if (!window.confirm(`Remove "${t.trainingName}" from this intern? This cannot be undone.`)) {
    return
  }
  trainingError.value = ''
  try {
    await api.delete(`/interns/${route.params.id}/trainings/${t.id}`)
    await loadTrainings()
  } catch (e) {
    trainingError.value = 'Could not remove training.'
  }
}

onMounted(() => {
  loadProgress()
  loadAssignments()
  loadGrades()
  loadAttendance()
  loadTrainings()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <router-link to="/interns" class="back-link">← Interns</router-link>
      <h1>Intern Progress</h1>
      <p v-if="progress" class="subtitle">{{ progress.internName }}</p>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>

    <template v-if="progress">
      <!-- KPI stat tiles -->
      <section class="stat-grid section">
        <div class="stat-tile">
          <span class="stat-label">Assignments completed</span>
          <span class="stat-value">{{ progress.completedAssignments }} / {{ progress.totalAssignments }}</span>
        </div>
        <div class="stat-tile">
          <span class="stat-label">Completion</span>
          <span class="stat-value">{{ progress.completionPercentage }}%</span>
        </div>
        <div class="stat-tile">
          <span class="stat-label">Average score</span>
          <span class="stat-value">{{ progress.averageScorePercentage }}%</span>
        </div>
      </section>

      <!-- Completion meter -->
      <section class="card section">
        <div class="meter-header">
          <span class="stat-label">Overall completion</span>
          <span class="meter-pct">{{ progress.completionPercentage }}%</span>
        </div>
        <div class="meter">
          <div class="meter-fill" :style="{ width: progress.completionPercentage + '%' }"></div>
        </div>
      </section>
    </template>

    <!-- Attendance Summary -->
    <section class="card section">
      <h3 class="form-title">Attendance Summary</h3>
      <div v-if="attendance" class="stat-grid">
        <div class="stat-tile">
          <span class="stat-label">Present</span>
          <span class="stat-value">{{ attendance.totalPresent }}</span>
        </div>
        <div class="stat-tile">
          <span class="stat-label">Late</span>
          <span class="stat-value">{{ attendance.totalLate }}</span>
        </div>
        <div class="stat-tile">
          <span class="stat-label">Absent</span>
          <span class="stat-value">{{ attendance.totalAbsent }}</span>
        </div>
        <div class="stat-tile">
          <span class="stat-label">Attendance</span>
          <span class="stat-value">{{ attendance.attendancePercentage }}%</span>
        </div>
      </div>
      <p v-else class="muted">No attendance records yet.</p>
    </section>

    <!-- Grades, grouped by training with a total score per training -->
    <section class="card section">
      <h3 class="form-title">Grades</h3>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Assignment</th>
              <th>Score</th>
              <th>Percentage</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="gradeRows.length === 0">
              <td colspan="3" class="muted">No grades recorded yet.</td>
            </tr>
            <template v-for="grp in gradesByTraining" :key="grp.training">
              <tr class="training-group-row">
                <td colspan="3">{{ grp.training }}</td>
              </tr>
              <tr v-for="g in grp.rows" :key="g.assignmentId">
                <td>{{ g.title }}</td>
                <td>
                  <span v-if="g.score != null">{{ g.score }} / {{ g.maxScore }}</span>
                  <span v-else class="muted">Not graded</span>
                </td>
                <td>
                  <span v-if="g.pct != null">{{ g.pct }}%</span>
                  <span v-else class="muted">—</span>
                </td>
              </tr>
              <tr class="training-total-row">
                <td>Total</td>
                <td>{{ grp.totalScore }} / {{ grp.totalMax }}</td>
                <td>
                  <span v-if="grp.totalPct != null">{{ grp.totalPct }}%</span>
                  <span v-else class="muted">—</span>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Training History -->
    <section class="card section">
      <h3 class="form-title">Training History</h3>
      <p class="muted section-hint">Trainings are assigned to a whole batch from the Interns page.</p>
      <p v-if="trainingError" class="error">{{ trainingError }}</p>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Training</th>
              <th>Box Drive</th>
              <th v-if="isAdmin" class="col-action"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="trainings.length === 0">
              <td :colspan="isAdmin ? 3 : 2" class="muted">No trainings recorded yet.</td>
            </tr>
            <tr v-for="t in trainings" :key="t.id">
              <td>{{ t.trainingName }}</td>
              <td>
                <a
                  v-if="t.repoUrl"
                  :href="t.repoUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="repo-link"
                >🔗 Open Box Folder</a>
                <span v-else class="muted">No link</span>
              </td>
              <td v-if="isAdmin" class="col-action">
                <div class="row-actions">
                  <button class="btn btn--ghost" @click="openEdit(t)">Edit</button>
                  <button class="btn btn--danger" @click="removeTraining(t)">Remove</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Record a score -->
    <section class="card section">
      <h3 class="form-title">Record a score</h3>
      <form class="add-form" @submit.prevent="submitScore">
        <div class="field">
          <label for="score-assignment">Assignment</label>
          <select id="score-assignment" class="select" v-model="scoreForm.assignmentId" required>
            <option value="" disabled>Select assignment</option>
            <option v-for="a in assignments" :key="a.id" :value="a.id">{{ a.title }} (max {{ a.maxScore }})</option>
          </select>
        </div>
        <div class="field">
          <label for="score-value">
            Score<span v-if="selectedAssignment" class="muted"> (0–{{ selectedAssignment.maxScore }})</span>
          </label>
          <input
            id="score-value"
            class="input"
            v-model.number="scoreForm.score"
            type="number"
            min="0"
            :max="selectedAssignment ? selectedAssignment.maxScore : undefined"
            placeholder="Score"
            required
          />
        </div>
        <div class="field field--action">
          <button type="submit" class="btn btn--primary">Save Score</button>
        </div>
      </form>
      <p v-if="scoreError" class="error score-error">{{ scoreError }}</p>
    </section>

    <!-- Edit training (ADMIN) -->
    <Modal :open="editDialog" title="Edit training" @close="editDialog = false">
      <div class="field">
        <label for="edit-training-name">Training name</label>
        <input id="edit-training-name" class="input" v-model="editForm.trainingName" required />
      </div>
      <p class="muted modal-hint">
        The Box Drive link is set for the whole training when it's assigned to a batch.
      </p>
      <template #footer>
        <button class="btn btn--secondary" @click="editDialog = false">Cancel</button>
        <button class="btn btn--primary" @click="saveEdit">Save</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.back-link {
  display: inline-block;
  font-size: 14px;
  margin-bottom: var(--sp-02);
}
.back-link:hover {
  text-decoration: none;
}
.form-title {
  margin-bottom: var(--sp-02);
}
.meter-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: var(--sp-01);
}
.meter-pct {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}
.add-form {
  display: flex;
  gap: var(--sp-02);
  flex-wrap: wrap;
  align-items: flex-end;
}
.add-form .field {
  flex: 1 1 220px;
  min-width: 180px;
}
.add-form .field--action {
  flex: 0 0 auto;
  min-width: 0;
}
.section-hint {
  margin-top: calc(-1 * var(--sp-01));
  margin-bottom: var(--sp-03);
}
.score-error {
  margin-top: var(--sp-02);
}
.repo-link {
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
/* Grades grouped by training */
.training-group-row td {
  font-weight: 600;
  background: var(--gray-10, #f4f4f4);
}
.training-total-row td {
  font-weight: 600;
  border-top: 2px solid var(--border, #e0e0e0);
}
</style>
