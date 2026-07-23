<script setup>
/**
 * Reusable KPI card for the dashboards.
 *
 * Visual base mirrors the existing `.stat-tile` look from style.css so the
 * dashboards stay consistent with the rest of the app. Values are supplied by
 * the parent — this component does no data fetching.
 *
 * Usage:
 *   <DashboardCard label="Total Interns" :value="128" />
 *   <DashboardCard label="Requiring Attention" :value="4" accent="warning"
 *                  caption="Below target this week" />
 */
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  caption: { type: String, default: '' },
  // Optional visual emphasis. Currently supports 'warning'.
  accent: { type: String, default: '' },
})
</script>

<template>
  <div class="stat-tile dash-card" :class="accent ? `dash-card--${accent}` : ''">
    <span class="stat-value">{{ value }}</span>
    <span class="stat-label">{{ label }}</span>
    <span v-if="caption" class="dash-card-caption">{{ caption }}</span>
  </div>
</template>

<style scoped>
.dash-card-caption {
  font-size: 12px;
  color: var(--text-secondary);
}
/* Left accent bar for cards that flag something needing attention.
   Base .stat-tile padding is --sp-03; subtract the 3px border to keep
   the content edge aligned with the other cards. */
.dash-card--warning {
  border-left: 3px solid var(--support-error);
  padding-left: calc(var(--sp-03) - 3px);
}
</style>
