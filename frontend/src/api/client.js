import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/progress-dashboard/api',
})

// Read the persisted token directly from storage to avoid a circular import
// with the auth service (auth.js imports this client).
function currentToken() {
  try {
    const raw = localStorage.getItem('lpd.auth')
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return parsed && parsed.token ? parsed.token : null
  } catch (e) {
    return null
  }
}

// Attach the bearer token to every outgoing request when present.
api.interceptors.request.use((config) => {
  const token = currentToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// If the token is missing/expired/invalid, the backend replies 401.
// Clear the session and send the user back to the login page.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Skip for the login call itself so its error can surface in the form.
      const url = error.config && error.config.url ? error.config.url : ''
      const isLoginCall = url.includes('/auth/login')
      if (!isLoginCall) {
        try {
          localStorage.removeItem('lpd.auth')
        } catch (e) {
          // ignore
        }
        if (window.location.pathname !== '/login') {
          window.location.assign('/login')
        }
      }
    }
    return Promise.reject(error)
  }
)

export default api
