<script setup>
import { computed } from 'vue'

const props = defineProps({
  successMessages: {
    type: Array,
    default: () => []
  },
  errorMessage: {
    type: String,
    default: ''
  },
  warningMessages: {
    type: Array,
    default: () => []
  }
})

const visibleSuccessMessages = computed(() => props.successMessages.filter(Boolean))
const visibleWarningMessages = computed(() => props.warningMessages.filter(Boolean))
</script>

<template>
  <template v-for="message in visibleSuccessMessages" :key="`success-${message}`">
    <p class="form-message success loc-message">{{ message }}</p>
  </template>
  <p v-if="errorMessage" class="form-message error loc-message">{{ errorMessage }}</p>
  <template v-for="message in visibleWarningMessages" :key="`warning-${message}`">
    <p class="form-message warning loc-message">{{ message }}</p>
  </template>
</template>
