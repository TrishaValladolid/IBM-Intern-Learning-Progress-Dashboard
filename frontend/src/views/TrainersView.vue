<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import auth from '../services/auth'
import BaseInput from '../components/BaseInput.vue'
import Modal from '../components/Modal.vue'

const isAdmin = auth.isAdmin
const currentUsername = computed(() => (auth.state.user ? auth.state.user.username : null))

const trainers = ref([])
const trainingNames = ref([])
const loading = ref(false)
const error = ref('')

// ---- Training tag input helpers ----
// Each form (add/edit) gets its own input ref so they don't share state.
const addTrainingInput = ref('')
const editTrainingInput = ref('')

// Suggestions for the datalist: all known names not already in the list.
function trainingSuggestions(current) {
  return trainingNames.value.filter(n => !current.includes(n))
}

function addTraining(list, inputRef) {
  // In the template, refs are auto-unwrapped, so we receive the raw ref object
  // here only when called from script. From the template @click the arg arrives
  // as the unwrapped string — handle both cases.
  const val = typeof inputRef === 'string'
    ? inputRef.trim()
    : (inputRef && inputRef.value ? inputRef.value.trim() : '')
  if (val && !list.includes(val)) {
    list.push(val)
  }
  // Clear whichever ref owns the input.
  if (list === addForm.value.assignedTrainings) addTrainingInput.value = ''
  else editTrainingInput.value = ''
}

function removeTraining(list, name) {
  const idx = list.indexOf(name)
  if (idx !== -1) list.splice(idx, 1)
}

// ---- Search & sort (client-side) ----
const search = ref('')
const sortKey = ref('fullName') // 'fullName' | 'username'
const sortDir = ref('asc') // 'asc' | 'desc'

function toggleSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = 'asc'
  }
}

const visibleTrainers = computed(() => {
  const term = search.value.trim().toLowerCase()
  let rows = trainers.value
  if (term) {
    rows = rows.filter((t) => {
      const name = (t.fullName || '').toLowerCase()
      const uname = (t.username || '').toLowerCase()
      const mail = (t.email || '').toLowerCase()
      return name.includes(term) || uname.includes(term) || mail.includes(term)
    })
  }
  const key = sortKey.value
  const dir = sortDir.value === 'asc' ? 1 : -1
  return [...rows].sort((a, b) => {
    const av = (a[key] || '').toString().toLowerCase()
    const bv = (b[key] || '').toString().toLowerCase()
    if (av < bv) return -1 * dir
    if (av > bv) return 1 * dir
    return 0
  })
})

function formatDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (isNaN(d.getTime())) return value
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

// ---- Load ----
async function loadTrainers() {
  loading.value = true
  error.value = ''
  try {
    const [res, trainingRes] = await Promise.all([api.get('/trainers'), api.get('/interns/trainings/names')])
    trainers.value = res.data
    trainingNames.value = trainingRes.data
  } catch (e) {
    if (e.response && e.response.status === 403) {
      error.value = 'You do not have permission to view trainers.'
    } else {
      error.value = 'Could not load trainers. Is the backend running?'
    }
  } finally {
    loading.value = false
  }
}

// ---- Dialog state ----
const dialog = ref(null) // 'add' | 'edit' | 'view' | 'reset' | null
const selected = ref(null)
const saving = ref(false)
const formError = ref('')

const addForm = ref(emptyAddForm())
const editForm = ref({ id: null, fullName: '', username: '', email: '', assignedTrainings: [] })
const resetForm = ref({ id: null, newPassword: '', confirmPassword: '' })

function emptyAddForm() {
  return {
    firstName: '',
    lastName: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'TRAINER',
    assignedTrainings: [],
  }
}

function closeDialog() {
  dialog.value = null
  selected.value = null
  formError.value = ''
}

function openAdd() {
  addForm.value = emptyAddForm()
  formError.value = ''
  dialog.value = 'add'
}

function openView(t) {
  selected.value = t
  dialog.value = 'view'
}

function openEdit(t) {
  selected.value = t
  editForm.value = {
    id: t.id,
    fullName: t.fullName || '',
    username: t.username || '',
    email: t.email || '',
    assignedTrainings: [...(t.assignedTrainings || [])],
  }
  formError.value = ''
  dialog.value = 'edit'
}

function openReset(t) {
  selected.value = t
  resetForm.value = { id: t.id, newPassword: '', confirmPassword: '' }
  formError.value = ''
  dialog.value = 'reset'
}

function serverError(e, fallback) {
  if (e.response && e.response.data && e.response.data.error) return e.response.data.error
  return fallback
}

// ---- Create ----
async function submitAdd() {
  const f = addForm.value
  formError.value = ''
  if (!f.firstName.trim() || !f.lastName.trim()) {
    formError.value = 'First and last name are required.'
    return
  }
  if (!f.username.trim()) {
    formError.value = 'Username is required.'
    return
  }
  if (f.password.length < 8) {
    formError.value = 'Password must be at least 8 characters.'
    return
  }
  if (f.password !== f.confirmPassword) {
    formError.value = 'Passwords do not match.'
    return
  }
  saving.value = true
  try {
    await api.post('/trainers', f)
    closeDialog()
    await loadTrainers()
  } catch (e) {
    formError.value = serverError(e, 'Could not create trainer.')
  } finally {
    saving.value = false
  }
}

// ---- Update ----
async function submitEdit() {
  const f = editForm.value
  formError.value = ''
  if (!f.fullName.trim()) {
    formError.value = 'Full name is required.'
    return
  }
  if (!f.username.trim()) {
    formError.value = 'Username is required.'
    return
  }
  saving.value = true
  try {
    await api.put(`/trainers/${f.id}`, {
      fullName: f.fullName,
      username: f.username,
      email: f.email,
      assignedTrainings: f.assignedTrainings,
    })
    closeDialog()
    await loadTrainers()
  } catch (e) {
    formError.value = serverError(e, 'Could not update trainer.')
  } finally {
    saving.value = false
  }
}

// ---- Reset password ----
async function submitReset() {
  const f = resetForm.value
  formError.value = ''
  if (f.newPassword.length < 8) {
    formError.value = 'Password must be at least 8 characters.'
    return
  }
  if (f.newPassword !== f.confirmPassword) {
    formError.value = 'Passwords do not match.'
    return
  }
  saving.value = true
  try {
    await api.put(`/trainers/${f.id}/password`, {
      newPassword: f.newPassword,
      confirmPassword: f.confirmPassword,
    })
    closeDialog()
  } catch (e) {
    formError.value = serverError(e, 'Could not reset password.')
  } finally {
    saving.value = false
  }
}

// ---- Enable / disable ----
async function toggleStatus(t) {
  error.value = ''
  try {
    await api.put(`/trainers/${t.id}/status`, { enabled: !t.enabled })
    await loadTrainers()
  } catch (e) {
    error.value = serverError(e, 'Could not update account status.')
  }
}

function isSelf(t) {
  return t.username && t.username === currentUsername.value
}

onMounted(loadTrainers)
</script>

<template>
  <div class="page">
    <div class="page-header header-row">
      <div>
        <h1>Trainers</h1>
        <p class="subtitle">Manage trainer accounts, credentials, and access.</p>
      </div>
      <button v-if="isAdmin" class="btn btn--primary" @click="openAdd">Add Trainer</button>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>

    <div class="toolbar section">
      <div class="field search-field">
        <label for="trainer-search">Search</label>
        <input
          id="trainer-search"
          class="input"
          v-model="search"
          placeholder="Search by name, username, or email"
        />
      </div>
    </div>

    <p v-if="loading" class="muted">Loading…</p>
    <div v-else class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th class="sortable" :aria-sort="sortKey === 'fullName' ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none'" @click="toggleSort('fullName')">
              Full Name
              <span v-if="sortKey === 'fullName'" class="sort-arrow">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
            </th>
            <th class="sortable" :aria-sort="sortKey === 'username' ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none'" @click="toggleSort('username')">
              Username
              <span v-if="sortKey === 'username'" class="sort-arrow">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
            </th>
            <th>Email</th>
            <th>Role</th>
            <th>Assigned Trainings</th>
            <th>Account Status</th>
            <th>Created Date</th>
            <th class="col-action"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="visibleTrainers.length === 0">
            <td colspan="8" class="muted">No trainers found.</td>
          </tr>
          <tr v-for="t in visibleTrainers" :key="t.id">
            <td>{{ t.fullName }}</td>
            <td>{{ t.username }}</td>
            <td>{{ t.email || '—' }}</td>
            <td>{{ t.role }}</td>
            <td>{{ t.assignedTrainings?.length ? t.assignedTrainings.join(', ') : 'All assignments' }}</td>
            <td>
              <span
                class="status-pill"
                :class="t.enabled ? 'status-pill--on' : 'status-pill--off'"
              >
                {{ t.enabled ? 'Active' : 'Disabled' }}
              </span>
            </td>
            <td>{{ formatDate(t.createdDate) }}</td>
            <td class="col-action">
              <div class="row-actions">
                <button class="btn btn--ghost" @click="openView(t)">View</button>
                <button class="btn btn--ghost" @click="openEdit(t)">Edit</button>
                <button class="btn btn--ghost" @click="openReset(t)">Reset Password</button>
                <button
                  class="btn btn--ghost"
                  :disabled="t.enabled && isSelf(t)"
                  :title="t.enabled && isSelf(t) ? 'You cannot disable your own account' : ''"
                  @click="toggleStatus(t)"
                >
                  {{ t.enabled ? 'Disable' : 'Enable' }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Add trainer -->
    <Modal :open="dialog === 'add'" title="Add trainer" @close="closeDialog">
      <form class="dialog-form" @submit.prevent="submitAdd">
        <div class="field-grid">
          <BaseInput v-model="addForm.firstName" label="First name" />
          <BaseInput v-model="addForm.lastName" label="Last name" />
        </div>
        <BaseInput v-model="addForm.username" label="Username" autocomplete="off" />
        <BaseInput v-model="addForm.email" label="Email" type="email" autocomplete="off" />
        <div class="field-grid">
          <BaseInput v-model="addForm.password" label="Password" type="password" autocomplete="new-password" />
          <BaseInput v-model="addForm.confirmPassword" label="Confirm password" type="password" autocomplete="new-password" />
        </div>
        <div class="field">
          <label for="add-role">Role</label>
          <select id="add-role" class="select" v-model="addForm.role">
            <option value="TRAINER">Trainer</option>
            <option value="ADMIN">Admin</option>
          </select>
        </div>
        <div class="field">
          <label>Assigned trainings</label>
          <div class="tag-list" v-if="addForm.assignedTrainings.length">
            <span v-for="t in addForm.assignedTrainings" :key="t" class="tag">
              {{ t }}
              <button type="button" class="tag-remove" @click="removeTraining(addForm.assignedTrainings, t)" aria-label="Remove">×</button>
            </span>
          </div>
          <div class="tag-input-row">
            <input
              id="add-trainings"
              class="input"
              v-model="addTrainingInput"
              list="add-training-list"
              placeholder="Type or pick a training, then Add"
              @keydown.enter.prevent="addTraining(addForm.assignedTrainings, addTrainingInput)"
            />
            <datalist id="add-training-list">
              <option v-for="n in trainingSuggestions(addForm.assignedTrainings)" :key="n" :value="n" />
            </datalist>
            <button type="button" class="btn btn--secondary" @click="addTraining(addForm.assignedTrainings, addTrainingInput)">Add</button>
          </div>
          <p class="muted field-hint">Leave empty to let this trainer view all assignments.</p>
        </div>
        <p v-if="formError" class="error">{{ formError }}</p>
      </form>
      <template #footer>
        <button class="btn btn--secondary" @click="closeDialog">Cancel</button>
        <button class="btn btn--primary" :disabled="saving" @click="submitAdd">
          {{ saving ? 'Saving…' : 'Create trainer' }}
        </button>
      </template>
    </Modal>

    <!-- Edit trainer -->
    <Modal :open="dialog === 'edit'" title="Edit trainer" @close="closeDialog">
      <form class="dialog-form" @submit.prevent="submitEdit">
        <BaseInput v-model="editForm.fullName" label="Full name" />
        <BaseInput v-model="editForm.username" label="Username" autocomplete="off" />
        <BaseInput v-model="editForm.email" label="Email" type="email" autocomplete="off" />
        <div class="field">
          <label>Assigned trainings</label>
          <div class="tag-list" v-if="editForm.assignedTrainings.length">
            <span v-for="t in editForm.assignedTrainings" :key="t" class="tag">
              {{ t }}
              <button type="button" class="tag-remove" @click="removeTraining(editForm.assignedTrainings, t)" aria-label="Remove">×</button>
            </span>
          </div>
          <div class="tag-input-row">
            <input
              id="edit-trainings"
              class="input"
              v-model="editTrainingInput"
              list="edit-training-list"
              placeholder="Type or pick a training, then Add"
              @keydown.enter.prevent="addTraining(editForm.assignedTrainings, editTrainingInput)"
            />
            <datalist id="edit-training-list">
              <option v-for="n in trainingSuggestions(editForm.assignedTrainings)" :key="n" :value="n" />
            </datalist>
            <button type="button" class="btn btn--secondary" @click="addTraining(editForm.assignedTrainings, editTrainingInput)">Add</button>
          </div>
          <p class="muted field-hint">Leave empty to let this trainer view all assignments.</p>
        </div>
        <p v-if="formError" class="error">{{ formError }}</p>
      </form>
      <template #footer>
        <button class="btn btn--secondary" @click="closeDialog">Cancel</button>
        <button class="btn btn--primary" :disabled="saving" @click="submitEdit">
          {{ saving ? 'Saving…' : 'Save changes' }}
        </button>
      </template>
    </Modal>

    <!-- View trainer -->
    <Modal :open="dialog === 'view'" title="Trainer details" @close="closeDialog">
      <dl v-if="selected" class="detail-list">
        <div><dt>Full name</dt><dd>{{ selected.fullName }}</dd></div>
        <div><dt>Username</dt><dd>{{ selected.username }}</dd></div>
        <div><dt>Email</dt><dd>{{ selected.email || '—' }}</dd></div>
        <div><dt>Role</dt><dd>{{ selected.role }}</dd></div>
        <div><dt>Assigned trainings</dt><dd>{{ selected.assignedTrainings?.length ? selected.assignedTrainings.join(', ') : 'All assignments' }}</dd></div>
        <div>
          <dt>Account status</dt>
          <dd>
            <span class="status-pill" :class="selected.enabled ? 'status-pill--on' : 'status-pill--off'">
              {{ selected.enabled ? 'Active' : 'Disabled' }}
            </span>
          </dd>
        </div>
        <div><dt>Created</dt><dd>{{ formatDate(selected.createdDate) }}</dd></div>
      </dl>
      <template #footer>
        <button class="btn btn--secondary" @click="closeDialog">Close</button>
      </template>
    </Modal>

    <!-- Reset password -->
    <Modal :open="dialog === 'reset'" title="Reset password" @close="closeDialog">
      <p v-if="selected" class="muted reset-hint">
        Set a new password for <strong>{{ selected.username }}</strong>.
      </p>
      <form class="dialog-form" @submit.prevent="submitReset">
        <BaseInput v-model="resetForm.newPassword" label="New password" type="password" autocomplete="new-password" />
        <BaseInput v-model="resetForm.confirmPassword" label="Confirm password" type="password" autocomplete="new-password" />
        <p v-if="formError" class="error">{{ formError }}</p>
      </form>
      <template #footer>
        <button class="btn btn--secondary" @click="closeDialog">Cancel</button>
        <button class="btn btn--primary" :disabled="saving" @click="submitReset">
          {{ saving ? 'Saving…' : 'Reset password' }}
        </button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-02);
}
.toolbar {
  display: flex;
  gap: var(--sp-02);
  flex-wrap: wrap;
}
.search-field {
  flex: 1 1 320px;
  max-width: 420px;
}
.sortable {
  cursor: pointer;
  user-select: none;
}
.sort-arrow {
  font-size: 10px;
  margin-left: 4px;
  color: var(--text-secondary);
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
.status-pill {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 12px;
  line-height: 1.5;
}
.status-pill--on {
  background: #defbe6;
  color: var(--support-success);
}
.status-pill--off {
  background: var(--gray-20);
  color: var(--gray-70);
}
.dialog-form {
  display: flex;
  flex-direction: column;
  gap: var(--sp-02);
}
.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--sp-02);
}
.detail-list {
  display: flex;
  flex-direction: column;
  gap: var(--sp-02);
  margin: 0;
}
.detail-list div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.detail-list dt {
  font-size: 12px;
  color: var(--text-secondary);
}
.detail-list dd {
  margin: 0;
  font-size: 14px;
  color: var(--text);
}
.reset-hint {
  margin-bottom: var(--sp-02);
}
.field-hint { margin: var(--sp-01) 0 0; font-size: 12px; }

/* ---- Training tag input ---- */
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: var(--sp-01);
}
.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--blue-10);
  border: 1px solid var(--blue-60);
  color: var(--blue-60);
  font-size: 13px;
  padding: 2px 8px 2px 10px;
  border-radius: 12px;
  font-weight: 500;
}
.tag-remove {
  background: none;
  border: none;
  color: var(--blue-60);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 0 2px;
  display: inline-flex;
  align-items: center;
}
.tag-remove:hover {
  color: var(--support-error);
}
.tag-input-row {
  display: flex;
  gap: var(--sp-01);
  align-items: stretch;
}
.tag-input-row .input {
  flex: 1;
}
.tag-input-row .btn {
  flex-shrink: 0;
}
</style>
