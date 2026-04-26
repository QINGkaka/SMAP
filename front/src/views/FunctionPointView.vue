<script setup>
import { computed } from 'vue'
import MetricActionHeader from '../components/metric/MetricActionHeader.vue'
import MetricStatusMessages from '../components/metric/MetricStatusMessages.vue'

const props = defineProps({
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
  },
  gscLabels: {
    type: Array,
    default: () => []
  },
  gscTotal: {
    type: Number,
    default: 0
  }
})

defineEmits(['analyze', 'export', 'update-count', 'update-gsc'])

const sections = computed(() => [
  { key: 'externalInputs', code: 'EI', title: '外部输入', values: props.form.externalInputs },
  { key: 'externalOutputs', code: 'EO', title: '外部输出', values: props.form.externalOutputs },
  { key: 'externalInquiries', code: 'EQ', title: '外部查询', values: props.form.externalInquiries },
  { key: 'internalLogicalFiles', code: 'ILF', title: '内部逻辑文件', values: props.form.internalLogicalFiles },
  { key: 'externalInterfaceFiles', code: 'EIF', title: '外部接口文件', values: props.form.externalInterfaceFiles }
])

function totalOf(values) {
  return Number(values.low || 0) + Number(values.average || 0) + Number(values.high || 0)
}

function splitOf(values) {
  return `${Number(values.low || 0)} / ${Number(values.average || 0)} / ${Number(values.high || 0)}`
}
</script>

<template>
  <section class="function-point-view compact-function-point-view">
    <MetricActionHeader
      title="功能点度量"
      :loading="loading"
      primary-text="计算"
      primary-loading-text="计算中..."
      export-text="导出"
      :export-disabled="!result"
      @primary="$emit('analyze')"
      @export="$emit('export')"
    />

    <div class="fp-overview-grid">
      <article v-for="section in sections" :key="section.key" class="fp-overview-card">
        <span>{{ section.code }}</span>
        <strong>{{ totalOf(section.values) }}</strong>
        <small>{{ section.title }}</small>
      </article>
      <article class="fp-overview-card fp-overview-card-accent">
        <span>GSC</span>
        <strong>{{ gscTotal }}</strong>
        <small>/ 70</small>
      </article>
    </div>

    <div class="function-point-panel-list">
      <details
        v-for="(section, index) in sections"
        :key="section.key"
        class="function-point-panel"
        :open="index === 0"
      >
        <summary>
          <div class="function-point-panel-title">
            <strong>{{ section.title }}</strong>
            <small>{{ section.code }}</small>
          </div>
          <div class="function-point-panel-meta">
            <span>{{ totalOf(section.values) }}</span>
            <em>{{ splitOf(section.values) }}</em>
          </div>
        </summary>

        <div class="function-point-panel-body">
          <label>
            <span>低</span>
            <input
              :value="section.values.low"
              type="number"
              min="0"
              @input="$emit('update-count', { section: section.key, field: 'low', value: Number($event.target.value) || 0 })"
            />
          </label>
          <label>
            <span>中</span>
            <input
              :value="section.values.average"
              type="number"
              min="0"
              @input="$emit('update-count', { section: section.key, field: 'average', value: Number($event.target.value) || 0 })"
            />
          </label>
          <label>
            <span>高</span>
            <input
              :value="section.values.high"
              type="number"
              min="0"
              @input="$emit('update-count', { section: section.key, field: 'high', value: Number($event.target.value) || 0 })"
            />
          </label>
        </div>
      </details>
    </div>

    <details class="form-section-toggle function-point-toggle">
      <summary>
        <span>通用系统特征 GSC</span>
        <strong>{{ gscTotal }} / 70</strong>
      </summary>
      <div class="factor-score-panel embedded">
        <div class="factor-score-grid">
          <label v-for="(label, index) in gscLabels" :key="label">
            <span>{{ index + 1 }}. {{ label }}</span>
            <input
              :value="form.generalSystemCharacteristics[index]"
              type="number"
              min="0"
              max="5"
              @input="$emit('update-gsc', { index, value: Number($event.target.value) || 0 })"
            />
          </label>
        </div>
      </div>
    </details>

    <MetricStatusMessages
      :success-messages="[message, reportMessage]"
      :error-message="errorMessage"
    />

    <div v-if="!result" class="empty-state loc-empty">暂无结果</div>

    <div v-else class="loc-result">
      <div class="loc-summary-grid">
        <article>
          <span>UFP</span>
          <strong>{{ result.unadjustedFunctionPoints }}</strong>
        </article>
        <article>
          <span>VAF</span>
          <strong>{{ result.valueAdjustmentFactor }}</strong>
        </article>
        <article>
          <span>AFP</span>
          <strong>{{ result.adjustedFunctionPoints }}</strong>
        </article>
        <article>
          <span>GSC</span>
          <strong>{{ result.generalSystemCharacteristicTotal }}</strong>
        </article>
      </div>

      <div class="estimation-result-grid">
        <article>
          <span>EI</span>
          <strong>{{ result.externalInputs }}</strong>
        </article>
        <article>
          <span>EO</span>
          <strong>{{ result.externalOutputs }}</strong>
        </article>
        <article>
          <span>EQ</span>
          <strong>{{ result.externalInquiries }}</strong>
        </article>
        <article>
          <span>ILF + EIF</span>
          <strong>{{ result.internalLogicalFiles + result.externalInterfaceFiles }}</strong>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.compact-function-point-view {
  display: grid;
  gap: 12px;
}

.compact-function-point-view :deep(.loc-header) {
  padding: 14px 18px;
}

.compact-function-point-view :deep(.metric-action-copy) {
  gap: 0;
}

.compact-function-point-view :deep(.metric-action-copy .eyebrow) {
  display: none;
}

.compact-function-point-view :deep(.metric-action-copy h2) {
  font-size: 13px;
  font-weight: 600;
}

.compact-function-point-view :deep(.metric-action-buttons .primary-button),
.compact-function-point-view :deep(.metric-action-buttons .secondary-button) {
  min-width: 0;
  min-height: 32px;
  padding: 0 12px;
  font-size: 12px;
}

.fp-overview-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.fp-overview-card {
  display: grid;
  gap: 6px;
  min-height: 88px;
  padding: 14px 16px;
  border: 1px solid var(--color-border-soft, #f0f1f5);
  border-radius: 8px;
  background: #ffffff;
}

.fp-overview-card span,
.fp-overview-card small {
  color: var(--color-text-secondary, #86909c);
}

.fp-overview-card span {
  font-size: 12px;
  font-weight: 600;
}

.fp-overview-card strong {
  color: var(--color-text, #1d2129);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.03em;
}

.fp-overview-card small {
  font-size: 11px;
}

.fp-overview-card-accent {
  background: #f7faff;
}

.function-point-panel-list {
  display: grid;
  gap: 10px;
}

.function-point-panel {
  overflow: hidden;
  border: 1px solid var(--color-border, #e5e6eb);
  border-radius: 8px;
  background: #ffffff;
}

.function-point-panel summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  list-style: none;
}

.function-point-panel summary::-webkit-details-marker {
  display: none;
}

.function-point-panel summary::after {
  content: '+';
  color: var(--color-text-secondary, #86909c);
  font-size: 16px;
}

.function-point-panel[open] summary::after {
  content: '-';
}

.function-point-panel-title,
.function-point-panel-meta {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.function-point-panel-title strong,
.function-point-panel-meta span {
  color: var(--color-text, #1d2129);
  font-size: 14px;
  font-weight: 600;
}

.function-point-panel-title small,
.function-point-panel-meta em {
  color: var(--color-text-secondary, #86909c);
  font-size: 12px;
  font-style: normal;
}

.function-point-panel-body {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 0 16px 16px;
}

.function-point-panel-body label {
  display: grid;
  gap: 6px;
}

.function-point-panel-body span {
  color: var(--color-text-secondary, #86909c);
  font-size: 12px;
  font-weight: 600;
}

.function-point-panel-body input {
  width: 100%;
  min-width: 0;
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-border, #e5e6eb);
  border-radius: 8px;
  background: #ffffff;
  outline: none;
}

.function-point-panel-body input:focus {
  border-color: var(--color-primary, #4080ff);
  box-shadow: 0 0 0 4px rgba(64, 128, 255, 0.12);
}

.function-point-toggle {
  margin-top: -2px;
}

@media (max-width: 1180px) {
  .fp-overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .fp-overview-grid,
  .function-point-panel-body {
    grid-template-columns: 1fr;
  }

  .function-point-panel summary {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
