<script setup>
/**
 * DRY scaffold for module pages that are not built yet. Every new module view
 * (Trainers, Batches, Reports, etc.) is a thin wrapper around this so they all
 * share the same header treatment and "coming soon" empty state.
 *
 * Usage:
 *   <PlaceholderPage title="Trainers"
 *     description="Manage trainer accounts and assignments." />
 */
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>{{ title }}</h1>
      <p v-if="description" class="subtitle">{{ description }}</p>
    </div>

    <section class="card section placeholder-note">
      <svg width="24" height="24" viewBox="0 0 32 32" fill="currentColor" aria-hidden="true">
        <path d="M16 2A14 14 0 1 0 30 16 14 14 0 0 0 16 2Zm0 26a12 12 0 1 1 12-12 12 12 0 0 1-12 12Z"/>
        <path d="M14.5 21.5h3v3h-3zM16 7a5 5 0 0 0-5 5h3a2 2 0 1 1 3.42 1.42l-1.3 1.3A3 3 0 0 0 14.5 18v.5h3V18a1 1 0 0 1 .3-.71l1.29-1.29A5 5 0 0 0 16 7Z"/>
      </svg>
      <div>
        <strong>Module coming soon</strong>
        <p class="muted">
          This area is part of the dashboard foundation. The interface is in place;
          functionality will be connected in a later phase.
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.placeholder-note {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-02);
  color: var(--text-secondary);
}
.placeholder-note strong {
  display: block;
  color: var(--text);
  margin-bottom: 4px;
}
.placeholder-note svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--blue-60);
}
</style>
