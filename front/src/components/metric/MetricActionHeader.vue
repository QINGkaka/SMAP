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
  exportDisabled: {
    type: Boolean,
    default: false
  }
})

defineEmits(['primary', 'export'])
</script>

<template>
  <div class="loc-header metric-action-header">
    <div class="metric-action-copy">
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
    </div>
    <div class="button-row metric-action-buttons">
      <button type="button" class="primary-button" :disabled="loading" @click="$emit('primary')">
        {{ loading ? primaryLoadingText : primaryText }}
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
