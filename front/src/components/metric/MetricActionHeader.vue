<script setup>
defineProps({
  eyebrow: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  },
  primaryText: {
    type: String,
    default: '开始分析'
  },
  primaryLoadingText: {
    type: String,
    default: '分析中...'
  },
  exportText: {
    type: String,
    default: '导出 Markdown'
  },
  helperText: {
    type: String,
    default: ''
  },
  helperDisabled: {
    type: Boolean,
    default: false
  },
  exportDisabled: {
    type: Boolean,
    default: false
  }
})

defineEmits(['primary', 'helper', 'export'])
</script>

<template>
  <div class="loc-header metric-action-header">
    <div class="metric-action-copy">
      <h2>{{ title }}</h2>
    </div>
    <div class="button-row metric-action-buttons">
      <button type="button" class="primary-button" :disabled="loading" @click="$emit('primary')">
        {{ loading ? primaryLoadingText : primaryText }}
      </button>
      <button
        v-if="helperText"
        type="button"
        class="secondary-button"
        :disabled="helperDisabled || loading"
        @click="$emit('helper')"
      >
        {{ helperText }}
      </button>
      <button
        type="button"
        class="secondary-button"
        :disabled="exportDisabled"
        @click="$emit('export')"
      >
        {{ exportText }}
      </button>
    </div>
  </div>
</template>
