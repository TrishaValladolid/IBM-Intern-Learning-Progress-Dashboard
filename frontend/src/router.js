import { createRouter, createWebHistory } from 'vue-router'
import auth from './services/auth'
import LoginView from './views/LoginView.vue'
import AdminDashboard from './views/AdminDashboard.vue'
import TrainerDashboard from './views/TrainerDashboard.vue'
import InternsView from './views/InternsView.vue'
import AssignmentsView from './views/AssignmentsView.vue'
import InternProgressView from './views/InternProgressView.vue'
// Placeholder module pages (dashboard foundation — no functionality yet).
import TrainersView from './views/TrainersView.vue'
import BatchesView from './views/BatchesView.vue'
import ReportsView from './views/ReportsView.vue'
import MyBatchesView from './views/MyBatchesView.vue'
import AttendanceView from './views/AttendanceView.vue'
import ProgressTrackingView from './views/ProgressTrackingView.vue'

const routes = [
  // Public
  { path: '/login', component: LoginView, meta: { public: true } },

  // Role landing pages
  { path: '/admin', component: AdminDashboard, meta: { roles: ['ADMIN'] } },
  { path: '/trainer', component: TrainerDashboard, meta: { roles: ['TRAINER'] } },

  // Shared authenticated pages (both roles may view).
  { path: '/interns', component: InternsView, meta: { roles: ['ADMIN', 'TRAINER'] } },
  { path: '/assignments', component: AssignmentsView, meta: { roles: ['ADMIN', 'TRAINER'] } },
  { path: '/interns/:id/progress', component: InternProgressView, meta: { roles: ['ADMIN', 'TRAINER'] } },
  { path: '/reports', component: ReportsView, meta: { roles: ['ADMIN', 'TRAINER'] } },

  // Admin-only module pages.
  { path: '/trainers', component: TrainersView, meta: { roles: ['ADMIN'] } },
  { path: '/batches', component: BatchesView, meta: { roles: ['ADMIN'] } },

  // Trainer-only module pages.
  { path: '/my-batches', component: MyBatchesView, meta: { roles: ['TRAINER'] } },
  { path: '/attendance', component: AttendanceView, meta: { roles: ['ADMIN', 'TRAINER'] } },
  { path: '/progress-tracking', component: ProgressTrackingView, meta: { roles: ['TRAINER'] } },

  // Root: send users to the right place based on auth/role.
  { path: '/', redirect: () => defaultRouteForCurrentUser() },

  // Fallback
  { path: '/:pathMatch(.*)*', redirect: () => defaultRouteForCurrentUser() },
]

function defaultRouteForCurrentUser() {
  if (!auth.isAuthenticated.value) return '/login'
  return auth.isAdmin.value ? '/admin' : '/trainer'
}

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * Global guard enforcing:
 *  - unauthenticated users can only reach public routes (redirected to /login),
 *  - authenticated users are kept off /login,
 *  - role-restricted routes reject users without the required role.
 */
router.beforeEach((to) => {
  const authed = auth.isAuthenticated.value
  const role = auth.role.value

  // Public routes.
  if (to.meta.public) {
    // Already logged in? Skip the login page.
    if (authed && to.path === '/login') {
      return auth.isAdmin.value ? '/admin' : '/trainer'
    }
    return true
  }

  // From here on the route requires authentication.
  if (!authed) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // Role check (requirement #9: trainers cannot reach admin-only pages).
  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return role === 'ADMIN' ? '/admin' : '/trainer'
  }

  return true
})

export default router
