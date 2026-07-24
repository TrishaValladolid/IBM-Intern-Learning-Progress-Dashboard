<script setup>
/**
 * Progress Tracking.
 *
 * A read-only overview of every intern's learning progress, built entirely from
 * existing endpoints — no new backend was added for this page:
 *   GET /interns                          -> the roster + names
 *   GET /interns/{id}/progress            -> completed/total, completion %, avg score %
 *   GET /attendance/summary?internId={id} -> attendance % + present/late/absent
 *   GET /submissions                      -> flat grade cells (internId, assignmentId, score)
 *   GET /assignments                      -> assignment titles + maxScore (joined for grades)
 *
 * "Overall Progress" and "Status" have no backend field; they are derived here
 * from the three metrics above so the rule is easy to see and tweak. Attendance
 * has no bulk endpoint, so it is fetched once per intern and fanned out with
 * Promise.all — fine at this scale and avoids adding an endpoint.
 */
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import Modal from '../components/Modal.vue'

const rows = ref([])
const loading = ref(true)
const error = ref('')

// The intern shown in the individual-progress modal (null = closed).
const selected = ref(null)

// Overall Progress: a weighted blend of the three metrics. Completion carries
// the most weight, with score and attendance sharing the rest.
function computeOverall(completionPct, avgScorePct, attendancePct) {
  const overall = 0.4 * completionPct + 0.3 * avgScorePct + 0.3 * attendancePct
  return Math.round(overall * 100) / 100
}

// Status is automatic. At Risk if the overall is low or any single metric is
// critically weak; On Track only when every metric is healthy; otherwise the
// intern Needs Attention.
function statusFor(r) {
  if (r.overallProgress < 50 || r.attendancePercentage < 60 || r.averageScorePercentage < 50) {
    return 'At Risk'
  }
  if (r.overallProgress >= 75 && r.attendancePercentage >= 75 && r.averageScorePercentage >= 70) {
    return 'On Track'
  }
  return 'Needs Attention'
}

function statusClass(status) {
  if (status === 'On Track') return 'pill--ontrack'
  if (status === 'At Risk') return 'pill--risk'
  return 'pill--attention'
}

// The concrete reasons an intern is flagged for the "Requiring Attention"
// section. An empty list means the intern is not flagged.
function reasonsFor(r) {
  const reasons = []
  if (r.averageScorePercentage < 60) reasons.push('Low scores')
  if (r.completionPercentage < 50) reasons.push('Missing submissions')
  if (r.attendancePercentage < 75) reasons.push('Low attendance')
  return reasons
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    // Grades + assignments are loaded once (not per intern) and joined client-side
    // to build a per-intern grade list for the detail modal.
    const [internsRes, submissionsRes, assignmentsRes] = await Promise.all([
      api.get('/interns'),
      api.get('/submissions'),
      api.get('/assignments'),
    ])
    const interns = internsRes.data
    const submissions = submissionsRes.data
    const assignments = assignmentsRes.data
    const assignmentById = new Map(assignments.map((a) => [a.id, a]))

    // For each intern, pull progress + attendance in parallel, then across all
    // interns in parallel too.
    rows.value = await Promise.all(
      interns.map(async (i) => {
        const [progRes, attRes] = await Promise.all([
          api.get(`/interns/${i.id}/progress`),
          api.get(`/attendance/summary?internId=${i.id}`),
        ])
        const p = progRes.data
        const att = attRes.data
        const attendancePercentage = att.attendancePercentage ?? 0
        const pending = Math.max(0, p.totalAssignments - p.completedAssignments)

        // Join this intern's grade cells to assignment titles/maxScore.
        const grades = submissions
          .filter((s) => String(s.internId) === String(i.id))
          .map((s) => {
            const a = assignmentById.get(s.assignmentId)
            const maxScore = a?.maxScore ?? null
            return {
              assignmentId: s.assignmentId,
              title: a?.title || `Assignment #${s.assignmentId}`,
              score: s.score,
              maxScore,
              pct:
                s.score != null && maxScore
                  ? Math.round((s.score / maxScore) * 100)
                  : null,
            }
          })

        const row = {
          id: i.id,
          name: p.internName || i.name,
          batch: i.batch,
          track: i.track,
          totalAssignments: p.totalAssignments,
          completedAssignments: p.completedAssignments,
          pendingAssignments: pending,
          completionPercentage: p.completionPercentage,
          averageScorePercentage: p.averageScorePercentage,
          attendancePercentage,
          attendancePresent: att.totalPresent ?? 0,
          attendanceLate: att.totalLate ?? 0,
          attendanceAbsent: att.totalAbsent ?? 0,
          grades,
        }
        row.overallProgress = computeOverall(
          row.completionPercentage,
          row.averageScorePercentage,
          row.attendancePercentage,
        )
        row.status = statusFor(row)
        row.reasons = reasonsFor(row)
        return row
      }),
    )
    // Order the table so the interns needing help surface first.
    rows.value.sort((a, b) => a.overallProgress - b.overallProgress)
  } catch (e) {
    error.value = 'Could not load progress data. Is the backend running?'
  } finally {
    loading.value = false
  }
}

const requiringAttention = computed(() =>
  rows.value.filter((r) => r.reasons.length > 0),
)

function openDetail(row) {
  selected.value = row
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Progress Tracking</h1>
      <p class="subtitle">
        Track every intern's assignment completion, scores, attendance and overall progress.
      </p>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>
    <p v-else-if="loading" class="muted section">Loading…</p>

    <template v-else>
      <!-- Interns Requiring Attention -->
      <section class="section">
        <h2 class="section-title">Interns Requiring Attention</h2>
        <div v-if="requiringAttention.length === 0" class="card empty-state">
          <p class="muted">Everyone is on track — no interns need attention right now.</p>
        </div>
        <div v-else class="attention-grid">
          <button
            v-for="r in requiringAttention"
            :key="r.id"
            type="button"
            class="attention-card"
            @click="openDetail(r)"
          >
            <div class="attention-head">
              <span class="attention-name">{{ r.name }}</span>
              <span class="status-pill" :class="statusClass(r.status)">{{ r.status }}</span>
            </div>
            <div class="reason-chips">
              <span v-for="reason in r.reasons" :key="reason" class="reason-chip">{{ reason }}</span>
            </div>
          </button>
        </div>
      </section>

      <!-- All interns -->
      <section class="section">
        <h2 class="section-title">All Interns</h2>
        <div v-if="rows.length === 0" class="card empty-state">
          <p class="muted">No interns yet. Add interns on the Interns page.</p>
        </div>
        <div v-else class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>Intern Name</th>
                <th>Assignment Completion</th>
                <th>Average Assignment Score</th>
                <th>Attendance Percentage</th>
                <th>Overall Progress</th>
                <th>Status</th>
                <th class="col-action"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.id">
                <td class="intern-name">
                  <button type="button" class="link-btn" @click="openDetail(r)">{{ r.name }}</button>
                  <span v-if="r.batch" class="muted"> · {{ r.batch }}</span>
                </td>
                <td>
                  <div class="meter-cell">
                    <div class="meter">
                      <div class="meter-fill" :style="{ width: r.completionPercentage + '%' }"></div>
                    </div>
                    <span class="meter-pct">{{ r.completedAssignments }}/{{ r.totalAssignments }}</span>
                  </div>
                </td>
                <td>{{ r.averageScorePercentage }}%</td>
                <td>{{ r.attendancePercentage }}%</td>
                <td>
                  <div class="meter-cell">
                    <div class="meter">
                      <div class="meter-fill" :style="{ width: r.overallProgress + '%' }"></div>
                    </div>
                    <span class="meter-pct">{{ r.overallProgress }}%</span>
                  </div>
                </td>
                <td>
                  <span class="status-pill" :class="statusClass(r.status)">{{ r.status }}</span>
                </td>
                <td class="col-action">
                  <button class="btn btn--ghost" @click="openDetail(r)">View</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <!-- Individual intern progress -->
    <Modal :open="selected !== null" :title="selected ? selected.name : ''" @close="selected = null">
      <div v-if="selected">
        <p class="detail-sub muted">
          <span v-if="selected.batch">{{ selected.batch }}</span>
          <span v-if="selected.track"> · {{ selected.track }}</span>
          <span class="status-pill detail-status" :class="statusClass(selected.status)">{{ selected.status }}</span>
        </p>

        <div class="stat-grid detail-stats">
          <div class="stat-tile">
            <span class="stat-label">Completed assignments</span>
            <span class="stat-value">{{ selected.completedAssignments }}</span>
          </div>
          <div class="stat-tile">
            <span class="stat-label">Pending assignments</span>
            <span class="stat-value">{{ selected.pendingAssignments }}</span>
          </div>
          <div class="stat-tile">
            <span class="stat-label">Average score</span>
            <span class="stat-value">{{ selected.averageScorePercentage }}%</span>
          </div>
          <div class="stat-tile">
            <span class="stat-label">Attendance</span>
            <span class="stat-value">{{ selected.attendancePercentage }}%</span>
          </div>
        </div>

        <div class="field detail-overall">
          <label>Overall progress</label>
          <div class="meter-cell">
            <div class="meter">
              <div class="meter-fill" :style="{ width: selected.overallProgress + '%' }"></div>
            </div>
            <span class="meter-pct">{{ selected.overallProgress }}%</span>
          </div>
        </div>

        <div v-if="selected.reasons.length" class="reason-chips detail-reasons">
          <span v-for="reason in selected.reasons" :key="reason" class="reason-chip">{{ reason }}</span>
        </div>

        <!-- Attendance summary -->
        <div class="detail-block">
          <h4 class="detail-heading">Attendance Summary</h4>
          <p class="attendance-line">
            <span class="att-part att-present">{{ selected.attendancePresent }} present</span>
            <span class="att-part att-late">{{ selected.attendanceLate }} late</span>
            <span class="att-part att-absent">{{ selected.attendanceAbsent }} absent</span>
            <span class="att-part muted">· {{ selected.attendancePercentage }}%</span>
          </p>
        </div>

        <!-- Grades -->
        <div class="detail-block">
          <h4 class="detail-heading">Grades</h4>
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
                <tr v-if="selected.grades.length === 0">
                  <td colspan="3" class="muted">No grades recorded yet.</td>
                </tr>
                <tr v-for="g in selected.grades" :key="g.assignmentId">
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
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <template #footer>
        <router-link
          v-if="selected"
          class="btn btn--secondary"
          :to="`/interns/${selected.id}/progress`"
        >
          Open full profile
        </router-link>
        <button class="btn btn--primary" @click="selected = null">Close</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.section-title {
  font-family: var(--font-mono);
  font-weight: 400;
  font-size: 18px;
  margin-bottom: var(--sp-03);
}
.empty-state {
  text-align: center;
}
.col-action {
  text-align: right;
  width: 1%;
  white-space: nowrap;
}
.intern-name {
  font-weight: 600;
}
.link-btn {
  background: none;
  border: none;
  padding: 0;
  font: inherit;
  color: var(--blue-60, #0f62fe);
  cursor: pointer;
  text-decoration: none;
}
.link-btn:hover {
  text-decoration: underline;
}
.meter-cell {
  display: flex;
  align-items: center;
  gap: var(--sp-02);
}
.meter-cell .meter {
  flex: 1 1 auto;
  min-width: 80px;
}
.meter-pct {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

/* Status pills — same palette as the attendance pills for consistency. */
.status-pill {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 12px;
  line-height: 1.5;
}
.pill--ontrack {
  background: #defbe6;
  color: var(--support-success, #24a148);
}
.pill--attention {
  background: #fdf6dd;
  color: #8e6a00;
}
.pill--risk {
  background: #fff1f1;
  color: var(--support-error, #da1e28);
}

/* Requiring-attention cards */
.attention-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--sp-03);
}
.attention-card {
  text-align: left;
  background: var(--white, #fff);
  border: 1px solid var(--border, #e0e0e0);
  border-left: 3px solid var(--support-error, #da1e28);
  border-radius: var(--radius, 4px);
  padding: var(--sp-03);
  cursor: pointer;
  font: inherit;
}
.attention-card:hover {
  background: var(--gray-10, #f4f4f4);
}
.attention-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-02);
  margin-bottom: var(--sp-02);
}
.attention-name {
  font-weight: 600;
}
.reason-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-01);
}
.reason-chip {
  font-size: 12px;
  background: var(--gray-10, #f4f4f4);
  color: var(--gray-70, #525252);
  border-radius: 12px;
  padding: 2px 10px;
}

/* Modal detail */
.detail-sub {
  display: flex;
  align-items: center;
  gap: var(--sp-02);
  margin-bottom: var(--sp-03);
}
.detail-status {
  margin-left: auto;
}
.detail-stats {
  margin-bottom: var(--sp-03);
}
.detail-overall {
  margin-bottom: var(--sp-03);
}
.detail-overall label {
  display: block;
  font-size: 12px;
  color: var(--gray-70, #525252);
  margin-bottom: var(--sp-01);
}
.detail-reasons {
  margin-top: var(--sp-02);
}
.detail-block {
  margin-top: var(--sp-04);
}
.detail-heading {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: var(--sp-02);
}
.attendance-line {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-02);
  font-size: 14px;
}
.att-present {
  color: var(--support-success, #24a148);
  font-weight: 600;
}
.att-late {
  color: #8e6a00;
  font-weight: 600;
}
.att-absent {
  color: var(--support-error, #da1e28);
  font-weight: 600;
}
</style>
