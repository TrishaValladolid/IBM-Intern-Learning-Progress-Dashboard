import { createRouter, createWebHistory } from 'vue-router'
import InternsView from './views/InternsView.vue'
import AssignmentsView from './views/AssignmentsView.vue'
import InternProgressView from './views/InternProgressView.vue'

const routes = [
  { path: '/', redirect: '/interns' },
  { path: '/interns', component: InternsView },
  { path: '/assignments', component: AssignmentsView },
  { path: '/interns/:id/progress', component: InternProgressView },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
