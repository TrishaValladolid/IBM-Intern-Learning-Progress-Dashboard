<script setup>
</script>

<template>
  <div id="app-shell">
    <nav>
      <router-link to="/interns">Interns</router-link>
      <router-link to="/assignments">Assignments</router-link>
    </nav>
    <router-view />
  </div>
</template>

<style scoped>
nav {
  display: flex;
  gap: 1.5rem;
  padding: 1rem 2rem;
  border-bottom: 1px solid #ddd;
}
nav a {
  text-decoration: none;
  color: #2c3e50;
  font-weight: 500;
}
nav a.router-link-active {
  color: #42b883;
}
</style>
