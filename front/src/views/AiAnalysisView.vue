<script setup>
import MetricActionHeader from '../components/metric/MetricActionHeader.vue'
import MetricStatusMessages from '../components/metric/MetricStatusMessages.vue'

defineProps({
  result: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  message: {
    type: String,
    default: ''
  },
  errorMessage: {
    type: String,
    default: ''
  },
  formatDateValue: {
    type: Function,
    required: true
  }
})

defineEmits(['analyze', 'export'])
</script>

<template>
  <MetricActionHeader
    eyebrow="智能分析"
    title="智能质量分析结果"
    :loading="loading"
    primary-text="生成智能建议"
    primary-loading-text="分析中..."
    :export-disabled="!result"
    @primary="$emit('analyze')"
    @export="$emit('export')"
  />
  <MetricStatusMessages
    :success-messages="[message]"
    :error-message="errorMessage"
  />
  <div v-if="!result" class="empty-state loc-empty">
    暂无智能分析结果。点击“生成智能建议”后，系统会基于 LoC、圈复杂度、CK/LK 和估算结果生成质量评价。
  </div>
  <div v-else class="ai-result">
    <article class="analysis-card">
      <span>总体评价</span>
      <p>{{ result.overallAssessment }}</p>
      <small>分析器：{{ result.modelName }} · {{ formatDateValue(result.analyzedAt) }}</small>
    </article>
    <div class="analysis-columns">
      <article>
        <h3>主要风险</h3>
        <ul>
          <li v-for="item in result.riskItems" :key="item">{{ item }}</li>
        </ul>
      </article>
      <article>
        <h3>重构建议</h3>
        <ul>
          <li v-for="item in result.refactoringSuggestions" :key="item">{{ item }}</li>
        </ul>
      </article>
      <article>
        <h3>测试建议</h3>
        <ul>
          <li v-for="item in result.testSuggestions" :key="item">{{ item }}</li>
        </ul>
      </article>
    </div>
  </div>
</template>
