<script setup>
/**
 * The single reusable authenticated shell: a sticky TopNav across the top, the
 * role-aware Sidebar on the left, and the routed page in the main content area.
 *
 * Pages never re-declare chrome — they render only their own content and this
 * layout supplies the surrounding structure.
 */
import TopNav from '../components/TopNav.vue'
import Sidebar from '../components/Sidebar.vue'
</script>

<template>
  <div class="layout">
    <TopNav />
    <div class="layout-body">
      <Sidebar />
      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--white);
}
.layout-body {
  flex: 1;
  display: flex;
  align-items: stretch;
}
.layout-main {
  flex: 1;
  min-width: 0;
  background: var(--gray-10);
}
</style>
