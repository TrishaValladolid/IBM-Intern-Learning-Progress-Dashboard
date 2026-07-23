<script setup>
/**
 * Reusable Carbon-styled modal dialog.
 *
 * Usage:
 *   <Modal :open="show" title="Add trainer" @close="show = false">
 *     <p>Body content…</p>
 *     <template #footer>
 *       <button class="btn btn--secondary" @click="show = false">Cancel</button>
 *       <button class="btn btn--primary" @click="save">Save</button>
 *     </template>
 *   </Modal>
 */
import { watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
})

const emit = defineEmits(['close'])

function close() {
  emit('close')
}

function onKeydown(e) {
  if (e.key === 'Escape') close()
}

// Lock body scroll and wire the Escape key only while the modal is open.
watch(
  () => props.open,
  (isOpen) => {
    if (typeof document === 'undefined') return
    if (isOpen) {
      document.addEventListener('keydown', onKeydown)
      document.body.style.overflow = 'hidden'
    } else {
      document.removeEventListener('keydown', onKeydown)
      document.body.style.overflow = ''
    }
  }
)

onBeforeUnmount(() => {
  if (typeof document === 'undefined') return
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-overlay" @click.self="close">
      <div class="modal" role="dialog" aria-modal="true" :aria-label="title">
        <header class="modal-header">
          <h3 class="modal-title">{{ title }}</h3>
          <button type="button" class="modal-close" aria-label="Close" @click="close">
            <svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor" aria-hidden="true">
              <path d="M24 9.4L22.6 8 16 14.6 9.4 8 8 9.4 14.6 16 8 22.6 9.4 24 16 17.4 22.6 24 24 22.6 17.4 16 24 9.4z"/>
            </svg>
          </button>
        </header>
        <div class="modal-body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="modal-footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(22, 22, 22, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--sp-03);
  z-index: 1000;
}
.modal {
  background: var(--white);
  border-radius: var(--radius);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 520px;
  max-height: calc(100vh - var(--sp-06));
  display: flex;
  flex-direction: column;
}
.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-02);
  padding: var(--sp-03) var(--sp-03) var(--sp-02);
}
.modal-title {
  font-family: var(--font-mono);
  font-weight: 400;
}
.modal-close {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  color: var(--gray-70);
  cursor: pointer;
  border-radius: var(--radius);
}
.modal-close:hover {
  background: var(--gray-10);
  color: var(--gray-100);
}
.modal-close:focus-visible {
  outline: 2px solid var(--blue-60);
  outline-offset: -2px;
}
.modal-body {
  padding: 0 var(--sp-03) var(--sp-03);
  overflow-y: auto;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--sp-01);
  padding: var(--sp-03);
  border-top: 1px solid var(--border);
}
</style>
