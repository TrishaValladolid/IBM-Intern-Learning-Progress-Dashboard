import { reactive, computed } from 'vue'
import api from '../api/client'

const STORAGE_KEY = 'lpd.auth'

/**
 * Load a previously persisted session (if any) so a page refresh keeps the user
 * logged in. Anything malformed is treated as "logged out".
 */
function loadPersisted() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (parsed && parsed.token && parsed.user) return parsed
  } catch (e) {
    // ignore corrupt storage
  }
  return null
}

const persisted = loadPersisted()

// Single shared reactive state object for the whole app.
const state = reactive({
  token: persisted ? persisted.token : null,
  user: persisted ? persisted.user : null, // { username, role, fullName }
})

function persist() {
  if (state.token && state.user) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: state.token, user: state.user }))
  } else {
    localStorage.removeItem(STORAGE_KEY)
  }
}

/**
 * Authenticate against the backend. On success, stores the token + user and
 * persists them. Throws a friendly Error on failure so the view can display it.
 */
async function login(username, password) {
  try {
    const res = await api.post('/auth/login', { username, password })
    state.token = res.data.token
    state.user = {
      username: res.data.username,
      role: res.data.role,
      fullName: res.data.fullName,
    }
    persist()
    return state.user
  } catch (e) {
    const msg =
      e.response && e.response.data && e.response.data.error
        ? e.response.data.error
        : 'Unable to sign in. Please try again.'
    throw new Error(msg)
  }
}

function logout() {
  state.token = null
  state.user = null
  persist()
}

// Reactive getters shared everywhere (router guard, App shell, views).
const isAuthenticated = computed(() => !!state.token)
const role = computed(() => (state.user ? state.user.role : null))
const isAdmin = computed(() => role.value === 'ADMIN')
const isTrainer = computed(() => role.value === 'TRAINER')

export const auth = {
  state,
  login,
  logout,
  isAuthenticated,
  role,
  isAdmin,
  isTrainer,
  getToken: () => state.token,
}

export default auth
