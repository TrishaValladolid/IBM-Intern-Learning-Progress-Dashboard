<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import auth from '../services/auth'
import Modal from '../components/Modal.vue'

const route = useRoute()
const progress = ref(null)
const intern = ref(null)
const assignments = ref([])
const grades = ref([]) // this intern's recorded grades (GradeCell[])
const attendance = ref(null) // this intern's AttendanceSummary
const scoreForm = ref({ assignmentId: '', score: '' })
const error = ref('')
const scoreError = ref('')
const gradeSummary = ref(null)
const feedback = ref([])
const feedbackForm = ref({ content: '' })
const feedbackError = ref('')
const feedbackLoading = ref(false)
const editingFeedbackId = ref(null)

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
const profileDialog = ref(false)
const profileForm = ref({})
const profileError = ref('')

async function loadProgress() {
  try {
    const res = await api.get(`/interns/${route.params.id}/progress`)
    progress.value = res.data
  } catch (e) {
    error.value = 'Could not load progress. Is the backend running?'
  }
}

async function loadIntern() {
  const res = await api.get(`/interns/${route.params.id}`)
  intern.value = res.data
}

function openProfileEdit() {
  profileForm.value = {
    name: intern.value.name || '', talentId: intern.value.talentId || '',
    batch: intern.value.batch || '', track: intern.value.track || '', status: intern.value.status || 'ACTIVE',
    totalHoursRequired: intern.value.totalHoursRequired ?? '', school: intern.value.school || '',
    course: intern.value.course || '', expectedGraduationDate: intern.value.expectedGraduationDate || '',
    expectedInternshipEndDate: intern.value.expectedInternshipEndDate || '',
  }
  profileError.value = ''
  profileDialog.value = true
}

async function saveProfile() {
  profileError.value = ''
  if (!profileForm.value.name.trim()) { profileError.value = 'Name is required.'; return }
  if (profileForm.value.totalHoursRequired !== '' && Number(profileForm.value.totalHoursRequired) < 0) { profileError.value = 'Required hours cannot be negative.'; return }
  try {
    await api.put(`/interns/${route.params.id}`, {
      ...profileForm.value,
      totalHoursRequired: profileForm.value.totalHoursRequired === '' ? null : Number(profileForm.value.totalHoursRequired),
    })
    profileDialog.value = false
    await loadIntern()
  } catch (e) {
    profileError.value = e.response?.data?.error || 'Could not update the intern profile.'
  }
}

async function loadGradeSummary() {
  const res = await api.get(`/interns/${route.params.id}/grades`)
  gradeSummary.value = res.data
}

async function loadFeedback() {
  feedbackLoading.value = true
  try {
    const res = await api.get(`/feedback?internId=${route.params.id}`)
    feedback.value = res.data
  } catch (e) {
    feedbackError.value = 'Could not load teacher feedback.'
  } finally {
    feedbackLoading.value = false
  }
}

function canManageFeedback(entry) {
  return isAdmin.value || entry.authorUsername === auth.state.user?.username
}

function startEditFeedback(entry) {
  editingFeedbackId.value = entry.id
  feedbackForm.value.content = entry.content
  feedbackError.value = ''
}

function cancelFeedbackEdit() {
  editingFeedbackId.value = null
  feedbackForm.value.content = ''
}

async function saveFeedback() {
  const content = feedbackForm.value.content.trim()
  if (!content) {
    feedbackError.value = 'Feedback cannot be empty.'
    return
  }
  feedbackError.value = ''
  try {
    if (editingFeedbackId.value) {
      await api.put(`/feedback/${editingFeedbackId.value}`, { content })
    } else {
      await api.post('/feedback', { internId: Number(route.params.id), content })
    }
    cancelFeedbackEdit()
    await loadFeedback()
  } catch (e) {
    feedbackError.value = e.response?.data?.error || 'Could not save feedback.'
  }
}

async function deleteFeedback(entry) {
  if (!window.confirm('Delete this feedback?')) return
  try {
    await api.delete(`/feedback/${entry.id}`)
    await loadFeedback()
  } catch (e) {
    feedbackError.value = e.response?.data?.error || 'Could not delete feedback.'
  }
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : ''
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
    await Promise.all([loadProgress(), loadGrades(), loadGradeSummary()])
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
  loadIntern()
  loadAssignments()
  loadGrades()
  loadGradeSummary()
  loadAttendance()
  loadTrainings()
  loadFeedback()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <router-link to="/interns" class="back-link">← Interns</router-link>
      <div class="profile-title-row">
        <div>
          <h1>{{ intern?.name || progress?.internName || 'Intern Profile' }}</h1>
          <p class="subtitle">Intern progress and development record</p>
        </div>
        <router-link v-if="isAdmin" :to="`/interns/${route.params.id}/print`" class="btn btn--secondary">Print</router-link>
      </div>
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

    <section v-if="intern" class="card section">
      <div class="section-heading"><h3 class="form-title">Intern Profile</h3><button v-if="isAdmin" class="btn btn--secondary" @click="openProfileEdit">Edit Profile</button></div>
      <dl class="profile-grid">
        <div><dt>School</dt><dd>{{ intern.school || 'Not recorded' }}</dd></div>
        <div><dt>Course</dt><dd>{{ intern.course || 'Not recorded' }}</dd></div>
        <div><dt>Required hours</dt><dd>{{ intern.totalHoursRequired ?? 'Not recorded' }}</dd></div>
        <div><dt>Expected graduation</dt><dd>{{ intern.expectedGraduationDate || 'Not recorded' }}</dd></div>
        <div><dt>Expected internship end</dt><dd>{{ intern.expectedInternshipEndDate || 'Not recorded' }}</dd></div>
      </dl>
    </section>

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

    <section class="card section">
      <h3 class="form-title">Grade Summary</h3>
      <p v-if="!gradeSummary || (!gradeSummary.categories.length && !gradeSummary.independentAssignments.length)" class="muted">No graded assignments recorded yet.</p>
      <div v-for="category in gradeSummary?.categories || []" :key="category.trainingName" class="grade-category">
        <div class="grade-category__header"><strong>{{ category.trainingName }}</strong><span>{{ category.totalScore ?? '—' }} / {{ category.totalMaxScore ?? '—' }}<template v-if="category.totalPercentage != null"> ({{ category.totalPercentage }}%)</template></span></div>
        <p v-for="item in category.assignments" :key="item.assignmentId" class="grade-line">{{ item.title }}: <span v-if="item.score != null">{{ item.score }} / {{ item.maxScore }}</span><span v-else class="muted">Not graded</span></p>
      </div>
      <div v-if="gradeSummary?.independentAssignments?.length" class="grade-category">
        <strong>Independent Assignments</strong>
        <p v-for="item in gradeSummary.independentAssignments" :key="item.assignmentId" class="grade-line">{{ item.title }}: <span v-if="item.score != null">{{ item.score }} / {{ item.maxScore }}<template v-if="item.percentage != null"> ({{ item.percentage }}%)</template></span><span v-else class="muted">Not graded</span></p>
      </div>
    </section>

    <section class="card section">
      <h3 class="form-title">Teacher Feedback</h3>
      <p v-if="feedbackError" class="error">{{ feedbackError }}</p>
      <form class="feedback-form" @submit.prevent="saveFeedback">
        <label for="feedback-content">{{ editingFeedbackId ? 'Edit feedback' : 'Add feedback' }}</label>
        <textarea id="feedback-content" class="input" v-model="feedbackForm.content" maxlength="4000" rows="4" placeholder="Write feedback for this intern..."></textarea>
        <div class="row-actions"><button class="btn btn--primary" type="submit">{{ editingFeedbackId ? 'Save changes' : 'Add feedback' }}</button><button v-if="editingFeedbackId" class="btn btn--secondary" type="button" @click="cancelFeedbackEdit">Cancel</button></div>
      </form>
      <p v-if="feedbackLoading" class="muted">Loading feedback…</p>
      <p v-else-if="feedback.length === 0" class="muted">No teacher feedback yet.</p>
      <article v-for="entry in feedback" :key="entry.id" class="feedback-entry">
        <div><strong>{{ entry.authorName || entry.authorUsername }}</strong><span class="muted feedback-date">{{ formatDate(entry.createdAt) }}<template v-if="entry.updatedAt"> · Edited {{ formatDate(entry.updatedAt) }}</template></span></div>
        <p>{{ entry.content }}</p>
        <div v-if="canManageFeedback(entry)" class="row-actions"><button class="btn btn--ghost" @click="startEditFeedback(entry)">Edit</button><button class="btn btn--danger" @click="deleteFeedback(entry)">Delete</button></div>
      </article>
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

    <Modal :open="profileDialog" title="Edit Intern Profile" @close="profileDialog = false">
      <p v-if="profileError" class="error">{{ profileError }}</p>
      <div class="profile-edit-grid">
        <div class="field"><label for="profile-name">Name</label><input id="profile-name" class="input" v-model="profileForm.name" required /></div>
        <div class="field"><label for="profile-talent">Talent ID</label><input id="profile-talent" class="input" v-model="profileForm.talentId" /></div>
        <div class="field"><label for="profile-batch">Batch</label><input id="profile-batch" class="input" v-model="profileForm.batch" /></div>
        <div class="field"><label for="profile-track">Track</label><input id="profile-track" class="input" v-model="profileForm.track" /></div>
        <div class="field"><label for="profile-hours">Required hours</label><input id="profile-hours" class="input" type="number" min="0" v-model="profileForm.totalHoursRequired" /></div>
        <div class="field"><label for="profile-school">School</label><input id="profile-school" class="input" v-model="profileForm.school" maxlength="255" /></div>
        <div class="field"><label for="profile-course">Course</label><input id="profile-course" class="input" v-model="profileForm.course" maxlength="255" /></div>
        <div class="field"><label for="profile-graduation">Expected graduation</label><input id="profile-graduation" class="input" type="date" v-model="profileForm.expectedGraduationDate" /></div>
        <div class="field"><label for="profile-end">Expected internship end</label><input id="profile-end" class="input" type="date" v-model="profileForm.expectedInternshipEndDate" /></div>
      </div>
      <template #footer><button class="btn btn--secondary" @click="profileDialog = false">Cancel</button><button class="btn btn--primary" @click="saveProfile">Save Profile</button></template>
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
.profile-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--sp-03);
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
.profile-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: var(--sp-03); }
.profile-grid dt { color: var(--text-secondary, #525252); font-size: 13px; }
.profile-grid dd { margin: var(--sp-01) 0 0; font-weight: 600; }
.section-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--sp-02); }
.profile-edit-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--sp-02); }
@media (max-width: 600px) { .profile-edit-grid { grid-template-columns: 1fr; } }
.grade-category { padding: var(--sp-02) 0; border-top: 1px solid var(--border, #e0e0e0); }
.grade-category__header { display: flex; justify-content: space-between; gap: var(--sp-02); }
.grade-line { margin: var(--sp-01) 0; }
.feedback-form { display: grid; gap: var(--sp-02); margin-bottom: var(--sp-03); }
.feedback-entry { border-top: 1px solid var(--border, #e0e0e0); padding: var(--sp-02) 0; }
.feedback-entry p { white-space: pre-wrap; }
.feedback-date { margin-left: var(--sp-02); font-size: 13px; }
</style>
