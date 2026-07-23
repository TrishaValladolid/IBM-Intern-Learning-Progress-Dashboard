<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import auth from '../services/auth'
import Modal from '../components/Modal.vue'

const route = useRoute()
const progress = ref(null)
const assignments = ref([])
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
    await loadProgress()
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
</style>
