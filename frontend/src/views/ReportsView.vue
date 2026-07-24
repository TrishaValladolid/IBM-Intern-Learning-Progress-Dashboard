<script setup>
/**
 * Reports.
 *
 * Summary analytics + a per-intern Progress Summary table, built entirely from
 * existing endpoints — no new backend was added for this page (mirrors the
 * approach in ProgressTrackingView):
 *   GET /interns                          -> the roster + names + batch (team)
 *   GET /interns/{id}/progress            -> completed/total, completion %, avg score %
 *   GET /attendance/summary               -> overall attendance statistics
 *   GET /attendance/summary?internId={id} -> per-intern attendance %
 *
 * "Overall Progress" has no backend field; it is derived here with the same
 * weighted blend used on the Progress Tracking page so the two stay consistent.
 * The "Generate Report" section re-runs the same load and lets the user export
 * the Progress Summary table as a CSV file (built client-side, no new endpoint).
 */
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'
import DashboardCard from '../components/DashboardCard.vue'

const rows = ref([])
const attendance = ref({ totalPresent: 0, totalLate: 0, totalAbsent: 0, attendancePercentage: 0 })
const loading = ref(true)
const error = ref('')
// Stamped when the user clicks "Generate Report" so they can see it is current.
const generatedAt = ref('')

// Overall Progress: same weighted blend as Progress Tracking. Completion carries
// the most weight, with score and attendance sharing the rest.
function computeOverall(completionPct, avgScorePct, attendancePct) {
  const overall = 0.4 * completionPct + 0.3 * avgScorePct + 0.3 * attendancePct
  return Math.round(overall * 100) / 100
}

function round2(v) {
  return Math.round(v * 100) / 100
}

function average(values) {
  if (!values.length) return 0
  return round2(values.reduce((sum, v) => sum + v, 0) / values.length)
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [internsRes, attendanceRes] = await Promise.all([
      api.get('/interns'),
      api.get('/attendance/summary'),
    ])
    attendance.value = attendanceRes.data

    // For each intern, pull progress + attendance in parallel, then across all
    // interns in parallel too (same fan-out as Progress Tracking).
    rows.value = await Promise.all(
      internsRes.data.map(async (i) => {
        const [progRes, attRes] = await Promise.all([
          api.get(`/interns/${i.id}/progress`),
          api.get(`/attendance/summary?internId=${i.id}`),
        ])
        const p = progRes.data
        const attendancePercentage = attRes.data.attendancePercentage ?? 0
        const row = {
          id: i.id,
          name: p.internName || i.name,
          batch: i.batch,
          totalAssignments: p.totalAssignments,
          completedAssignments: p.completedAssignments,
          completionPercentage: p.completionPercentage,
          averageScorePercentage: p.averageScorePercentage,
          attendancePercentage,
        }
        row.overallProgress = computeOverall(
          row.completionPercentage,
          row.averageScorePercentage,
          row.attendancePercentage,
        )
        return row
      }),
    )
    rows.value.sort((a, b) => b.overallProgress - a.overallProgress)
  } catch (e) {
    error.value = 'Could not load report data. Is the backend running?'
  } finally {
    loading.value = false
  }
}

// ---- Summary analytics (derived from the loaded rows) ----

const avgAssignmentScore = computed(() =>
  average(rows.value.map((r) => r.averageScorePercentage)),
)

const avgAssignmentProgress = computed(() =>
  average(rows.value.map((r) => r.completionPercentage)),
)

// Average each batch's completion, then average the batches so every cohort is
// weighted equally regardless of size.
const avgBatchAssignmentProgress = computed(() => {
  const byBatch = new Map()
  for (const r of rows.value) {
    const key = r.batch || 'Unassigned'
    if (!byBatch.has(key)) byBatch.set(key, [])
    byBatch.get(key).push(r.completionPercentage)
  }
  const teamAverages = [...byBatch.values()].map((list) => average(list))
  return average(teamAverages)
})

const summaryCards = computed(() => [
  { label: 'Average Assignment Score', value: `${avgAssignmentScore.value}%` },
  { label: 'Average Assignment Progress', value: `${avgAssignmentProgress.value}%` },
  { label: 'Average Batch Progress', value: `${avgBatchAssignmentProgress.value}%` },
  {
    label: 'Attendance',
    value: `${attendance.value.attendancePercentage}%`,
    caption: `${attendance.value.totalPresent} present · ${attendance.value.totalLate} late · ${attendance.value.totalAbsent} absent`,
  },
])

// ---- Generate Report + CSV export ----

async function generateReport() {
  await loadData()
  generatedAt.value = new Date().toLocaleString()
}

// Wrap a value for CSV: stringify, escape quotes, and quote so commas are safe.
function csvCell(value) {
  const s = value == null ? '' : String(value)
  return `"${s.replace(/"/g, '""')}"`
}

function exportCsv() {
  const headers = [
    'Intern Name',
    'Average Score (%)',
    'Attendance (%)',
    'Assignment Completion',
    'Completion (%)',
    'Overall Progress (%)',
  ]
  const lines = [headers.map(csvCell).join(',')]
  for (const r of rows.value) {
    lines.push(
      [
        r.name,
        r.averageScorePercentage,
        r.attendancePercentage,
        `${r.completedAssignments}/${r.totalAssignments}`,
        r.completionPercentage,
        r.overallProgress,
      ]
        .map(csvCell)
        .join(','),
    )
  }
  const csv = lines.join('\r\n')

  // Trigger a client-side download — no backend endpoint required.
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'progress-summary-report.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Reports</h1>
      <p class="subtitle">
        Summary analytics across all interns, with an exportable progress summary.
      </p>
    </div>

    <p v-if="error" class="error section">{{ error }}</p>
    <p v-else-if="loading" class="muted section">Loading…</p>

    <template v-else>
      <!-- Summary analytics -->
      <section class="stat-grid section">
        <DashboardCard
          v-for="card in summaryCards"
          :key="card.label"
          :label="card.label"
          :value="card.value"
          :caption="card.caption"
        />
      </section>

      <!-- Progress Summary table -->
      <section class="section">
        <h2 class="section-title">Progress Summary</h2>
        <div v-if="rows.length === 0" class="card empty-state">
          <p class="muted">No interns yet. Add interns on the Interns page.</p>
        </div>
        <div v-else class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>Intern Name</th>
                <th>Average Score</th>
                <th>Attendance Percentage</th>
                <th>Assignment Completion</th>
                <th>Overall Progress</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.id">
                <td class="intern-name">
                  {{ r.name }}
                  <span v-if="r.batch" class="muted"> · {{ r.batch }}</span>
                </td>
                <td>{{ r.averageScorePercentage }}%</td>
                <td>{{ r.attendancePercentage }}%</td>
                <td>
                  <div class="meter-cell">
                    <div class="meter">
                      <div class="meter-fill" :style="{ width: r.completionPercentage + '%' }"></div>
                    </div>
                    <span class="meter-pct">{{ r.completedAssignments }}/{{ r.totalAssignments }}</span>
                  </div>
                </td>
                <td>
                  <div class="meter-cell">
                    <div class="meter">
                      <div class="meter-fill" :style="{ width: r.overallProgress + '%' }"></div>
                    </div>
                    <span class="meter-pct">{{ r.overallProgress }}%</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Generate Report -->
      <section class="section">
        <h2 class="section-title">Generate Report</h2>
        <div class="card generate-card">
          <div class="generate-text">
            <p>Generate a progress summary report and export it as a CSV file.</p>
            <p v-if="generatedAt" class="muted">Last generated: {{ generatedAt }}</p>
          </div>
          <div class="generate-actions">
            <button class="btn btn--secondary" @click="generateReport">Generate Report</button>
            <button class="btn btn--primary" :disabled="rows.length === 0" @click="exportCsv">
              Export CSV
            </button>
          </div>
        </div>
      </section>
    </template>
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
.intern-name {
  font-weight: 600;
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
.generate-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-04);
  flex-wrap: wrap;
}
.generate-actions {
  display: flex;
  gap: var(--sp-02);
}
</style>
