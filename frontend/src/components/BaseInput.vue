<script setup>
/**
 * Reusable Carbon-styled text field with a floating label, optional error
 * message, and (for passwords) a show/hide toggle.
 *
 * Usage:
 *   <BaseInput v-model="username" label="Username" />
 *   <BaseInput v-model="password" label="Password" type="password" :error="err" />
 */
import { ref, computed, useId } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, required: true },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  error: { type: String, default: '' },
  autocomplete: { type: String, default: 'off' },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const uid = useId()
const revealed = ref(false)

const isPassword = computed(() => props.type === 'password')
const inputType = computed(() =>
  isPassword.value ? (revealed.value ? 'text' : 'password') : props.type
)

function onInput(e) {
  emit('update:modelValue', e.target.value)
}
</script>

<template>
  <div class="field">
    <label class="field-label" :for="uid">{{ label }}</label>
    <div class="input-wrap" :class="{ 'has-toggle': isPassword }">
      <input
        :id="uid"
        class="input"
        :class="{ 'input--error': error }"
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        :disabled="disabled"
        :aria-invalid="!!error"
        :aria-describedby="error ? uid + '-err' : undefined"
        @input="onInput"
      />
      <button
        v-if="isPassword"
        type="button"
        class="reveal-btn"
        :aria-label="revealed ? 'Hide password' : 'Show password'"
        :aria-pressed="revealed"
        tabindex="0"
        @click="revealed = !revealed"
      >
        <!-- eye (same icon for both show and hide states) -->
        <svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor" aria-hidden="true">
          <path d="M30.94 15.66A16.69 16.69 0 0 0 16 5 16.69 16.69 0 0 0 1.06 15.66a1 1 0 0 0 0 .68A16.69 16.69 0 0 0 16 27a16.69 16.69 0 0 0 14.94-10.66 1 1 0 0 0 0-.68ZM16 25c-5.3 0-10.9-3.93-12.93-9C5.1 10.93 10.7 7 16 7s10.9 3.93 12.93 9C26.9 21.07 21.3 25 16 25Z"/>
          <path d="M16 10a6 6 0 1 0 6 6 6 6 0 0 0-6-6Zm0 10a4 4 0 1 1 4-4 4 4 0 0 1-4 4Z"/>
        </svg>
      </button>
    </div>
    <p v-if="error" :id="uid + '-err'" class="field-error" role="alert">{{ error }}</p>
  </div>
</template>

<style scoped>
.input-wrap {
  position: relative;
}
.input-wrap.has-toggle .input {
  padding-right: 44px;
}
.input--error {
  outline: 2px solid var(--support-error);
  outline-offset: -2px;
}
.reveal-btn {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  width: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: var(--gray-70);
  cursor: pointer;
}
.reveal-btn:hover {
  color: var(--gray-100);
}
.reveal-btn:focus-visible {
  outline: 2px solid var(--blue-60);
  outline-offset: -2px;
}
.field-error {
  margin-top: 4px;
  font-size: 12px;
  color: var(--support-error);
}
</style>
