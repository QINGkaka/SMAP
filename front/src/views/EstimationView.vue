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
  reportMessage: {
    type: String,
    default: ''
  },
  errorMessage: {
    type: String,
    default: ''
  },
  form: {
    type: Object,
    required: true
  }
})

defineEmits(['analyze', 'export', 'update-form'])
</script>

<template>
  <MetricActionHeader
    eyebrow="COCOMO"
    title="工作量与成本估算结果"
    :loading="loading"
    primary-text="开始估算"
    primary-loading-text="估算中..."
    :export-disabled="!result"
    @primary="$emit('analyze')"
    @export="$emit('export')"
  />
  <div class="estimation-form">
    <label>
      <span>项目模式</span>
      <select
        :value="form.mode"
        @change="$emit('update-form', { mode: $event.target.value })"
      >
        <option value="ORGANIC">有机型：小型、经验充足、约束少</option>
        <option value="SEMIDETACHED">半独立型：中等规模、约束适中</option>
        <option value="EMBEDDED">嵌入型：强约束、复杂环境</option>
      </select>
    </label>
    <label>
      <span>KLOC</span>
      <input
        :value="form.kloc"
        type="number"
        min="0"
        step="0.01"
        placeholder="留空则使用最新 LoC"
        @input="$emit('update-form', { kloc: $event.target.value })"
      />
    </label>
    <label>
      <span>人月成本</span>
      <input
        :value="form.costPerPersonMonth"
        type="number"
        min="0"
        step="100"
        @input="$emit('update-form', { costPerPersonMonth: $event.target.value })"
      />
    </label>
    <button type="button" class="primary-button" :disabled="loading" @click="$emit('analyze')">
      {{ loading ? '估算中...' : '计算估算值' }}
    </button>
  </div>
  <MetricStatusMessages
    :success-messages="[message, reportMessage]"
    :error-message="errorMessage"
  />
  <div v-if="!result" class="empty-state loc-empty">
    暂无结果。可直接开始估算，KLOC 留空时将优先使用最新 LoC。
  </div>
  <div v-else class="loc-result">
    <div class="loc-summary-grid">
      <article>
        <span>规模 KLOC</span>
        <strong>{{ result.kloc }}</strong>
      </article>
      <article>
        <span>工作量</span>
        <strong>{{ result.effortPersonMonths }}</strong>
      </article>
      <article>
        <span>开发周期</span>
        <strong>{{ result.developmentMonths }}</strong>
      </article>
      <article>
        <span>平均人员</span>
        <strong>{{ result.averageStaff }}</strong>
      </article>
    </div>
    <div class="estimation-result-grid">
      <article>
        <span>项目模式</span>
        <strong>{{ result.modeLabel }}</strong>
      </article>
      <article>
        <span>规模来源</span>
        <strong>{{ result.scaleSource }}</strong>
      </article>
      <article>
        <span>人月成本</span>
        <strong>{{ result.costPerPersonMonth }}</strong>
      </article>
      <article>
        <span>估算成本</span>
        <strong>{{ result.estimatedCost }}</strong>
      </article>
    </div>
  </div>
</template>
