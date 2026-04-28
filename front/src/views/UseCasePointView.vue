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
  technicalLabels: {
    type: Array,
    default: () => []
  },
  environmentalLabels: {
    type: Array,
    default: () => []
  },
  technicalTotal: {
    type: Number,
    default: 0
  },
  environmentalTotal: {
    type: Number,
    default: 0
  }
})

defineEmits(['analyze-from-file', 'analyze-current', 'export', 'update-field', 'update-technical', 'update-environmental'])

const actorGroups = computed(() => [
  { key: 'simpleActors', label: '简单', value: props.form.simpleActors },
  { key: 'averageActors', label: '一般', value: props.form.averageActors },
  { key: 'complexActors', label: '复杂', value: props.form.complexActors }
])

const useCaseGroups = computed(() => [
  { key: 'simpleUseCases', label: '简单', value: props.form.simpleUseCases },
  { key: 'averageUseCases', label: '一般', value: props.form.averageUseCases },
  { key: 'complexUseCases', label: '复杂', value: props.form.complexUseCases }
])

function totalOf(groups) {
  return groups.reduce((sum, item) => sum + Number(item.value || 0), 0)
}

function splitOf(groups) {
  return groups.map((item) => Number(item.value || 0)).join(' / ')
}

const resultBars = computed(() => {
  if (!props.result) {
    return []
  }
  const items = [
    { label: 'UAW', title: '参与者权重', value: Number(props.result.actorWeight || 0), tone: 'blue' },
    { label: 'UUCW', title: '用例权重', value: Number(props.result.useCaseWeight || 0), tone: 'green' },
    { label: 'TCF', title: '技术因子', value: Number(props.result.technicalComplexityFactor || 0), tone: 'cyan' },
    { label: 'ECF', title: '环境因子', value: Number(props.result.environmentalComplexityFactor || 0), tone: 'gold' }
  ]
  const max = Math.max(...items.map(item => item.value), 1)
  return items.map((item, index) => ({
    ...item,
    r: 24 + (item.value / max) * 26,
    cx: [76, 212, 132, 264][index],
    cy: [86, 78, 182, 174][index]
  }))
})
</script>

<template>
  <section class="use-case-view compact-use-case-view">
    <MetricActionHeader
      title="用例点度量"
      :loading="loading"
      primary-text="从文件计算"
      primary-loading-text="计算中..."
      helper-text="按当前修改计算"
      export-text="导出"
      :export-disabled="!result"
      @primary="$emit('analyze-from-file')"
      @helper="$emit('analyze-current')"
      @export="$emit('export')"
    />

    <section class="ucp-intro-card">
      <strong>文件识别主导</strong>
      <p>系统会优先读取当前项目中的用例模型文件，以及控制器、服务、外部接口类名，自动生成参与者和用例复杂度。这里的数字主要用于修正识别结果，不再要求从零手填。</p>
    </section>

    <div class="ucp-overview-grid">
      <article class="ucp-overview-card">
        <span>参与者</span>
        <strong>{{ totalOf(actorGroups) }}</strong>
      </article>
      <article class="ucp-overview-card">
        <span>用例</span>
        <strong>{{ totalOf(useCaseGroups) }}</strong>
      </article>
      <article class="ucp-overview-card">
        <span>技术总分</span>
        <strong>{{ technicalTotal }}</strong>
      </article>
      <article class="ucp-overview-card">
        <span>环境总分</span>
        <strong>{{ environmentalTotal }}</strong>
      </article>
      <article class="ucp-overview-card ucp-overview-card-accent">
        <span>工时</span>
        <strong>{{ form.productivityHoursPerUseCasePoint }}</strong>
      </article>
    </div>

    <div class="use-case-panel-list">
      <details class="use-case-panel" open>
        <summary>
          <div class="use-case-panel-title">
            <strong>参与者复杂度</strong>
            <small>Actor</small>
          </div>
          <div class="use-case-panel-meta">
            <span>{{ totalOf(actorGroups) }}</span>
            <em>{{ splitOf(actorGroups) }}</em>
          </div>
        </summary>
        <div class="use-case-panel-body">
          <label v-for="item in actorGroups" :key="item.key">
            <span>{{ item.label }}</span>
            <input
              :value="item.value"
              type="number"
              min="0"
              @input="$emit('update-field', { field: item.key, value: Number($event.target.value) || 0 })"
            />
          </label>
        </div>
      </details>

      <details class="use-case-panel">
        <summary>
          <div class="use-case-panel-title">
            <strong>用例复杂度</strong>
            <small>Use Case</small>
          </div>
          <div class="use-case-panel-meta">
            <span>{{ totalOf(useCaseGroups) }}</span>
            <em>{{ splitOf(useCaseGroups) }}</em>
          </div>
        </summary>
        <div class="use-case-panel-body">
          <label v-for="item in useCaseGroups" :key="item.key">
            <span>{{ item.label }}</span>
            <input
              :value="item.value"
              type="number"
              min="0"
              @input="$emit('update-field', { field: item.key, value: Number($event.target.value) || 0 })"
            />
          </label>
        </div>
      </details>

      <details class="use-case-panel">
        <summary>
          <div class="use-case-panel-title">
            <strong>生产率</strong>
            <small>Hours</small>
          </div>
          <div class="use-case-panel-meta">
            <span>{{ form.productivityHoursPerUseCasePoint }}</span>
            <em>每 UCP</em>
          </div>
        </summary>
        <div class="use-case-panel-body use-case-panel-body-single">
          <label>
            <span>工时 / UCP</span>
            <input
              :value="form.productivityHoursPerUseCasePoint"
              type="number"
              min="1"
              @input="$emit('update-field', { field: 'productivityHoursPerUseCasePoint', value: Number($event.target.value) || 0 })"
            />
          </label>
        </div>
      </details>
    </div>

      <details class="form-section-toggle">
      <summary>
        <span>技术因子总分</span>
        <strong>{{ technicalTotal }} / 65</strong>
      </summary>
      <div class="factor-score-panel embedded">
        <div class="factor-score-grid">
          <label v-for="(label, index) in technicalLabels" :key="label">
            <span>{{ index + 1 }}. {{ label }}</span>
            <input
              :value="form.technicalFactors[index]"
              type="number"
              min="0"
              max="5"
              @input="$emit('update-technical', { index, value: Number($event.target.value) || 0 })"
            />
          </label>
        </div>
      </div>
    </details>

    <details class="form-section-toggle">
      <summary>
        <span>环境因子总分</span>
        <strong>{{ environmentalTotal }} / 40</strong>
      </summary>
      <div class="factor-score-panel embedded">
        <div class="factor-score-grid">
          <label v-for="(label, index) in environmentalLabels" :key="label">
            <span>{{ index + 1 }}. {{ label }}</span>
            <input
              :value="form.environmentalFactors[index]"
              type="number"
              min="0"
              max="5"
              @input="$emit('update-environmental', { index, value: Number($event.target.value) || 0 })"
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
          <span>UCP</span>
          <strong>{{ result.useCasePoints }}</strong>
        </article>
        <article>
          <span>工时</span>
          <strong>{{ result.estimatedHours }}</strong>
        </article>
        <article>
          <span>人月</span>
          <strong>{{ result.estimatedPersonMonths }}</strong>
        </article>
        <article>
          <span>UUCP</span>
          <strong>{{ result.unadjustedUseCasePoints }}</strong>
        </article>
      </div>
      <section class="visual-card">
        <div class="visual-card-copy">
          <h3>用例点构成</h3>
          <p>每个气泡代表一个核心指标，气泡越大说明该指标数值越高。</p>
        </div>
        <div class="bubble-chart">
          <svg viewBox="0 0 340 240" role="img" aria-label="用例点气泡图">
            <g v-for="item in resultBars" :key="item.label">
              <circle :cx="item.cx" :cy="item.cy" :r="item.r" class="bubble-node" :class="item.tone" />
              <text :x="item.cx" :y="item.cy - 2" text-anchor="middle" class="bubble-label">{{ item.label }}</text>
              <text :x="item.cx" :y="item.cy + 14" text-anchor="middle" class="bubble-value">{{ item.value }}</text>
            </g>
          </svg>
        </div>
      </section>
      <div class="estimation-result-grid">
        <article>
          <span>UAW</span>
          <strong>{{ result.actorWeight }}</strong>
        </article>
        <article>
          <span>UUCW</span>
          <strong>{{ result.useCaseWeight }}</strong>
        </article>
        <article>
          <span>TCF</span>
          <strong>{{ result.technicalComplexityFactor }}</strong>
        </article>
        <article>
          <span>ECF</span>
          <strong>{{ result.environmentalComplexityFactor }}</strong>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.compact-use-case-view {
  display: grid;
  gap: 12px;
}

.compact-use-case-view :deep(.loc-header) {
  padding: 14px 18px;
}

.compact-use-case-view :deep(.metric-action-copy) {
  gap: 0;
}

.compact-use-case-view :deep(.metric-action-copy .eyebrow) {
  display: none;
}

.compact-use-case-view :deep(.metric-action-copy h2) {
  font-size: 13px;
  font-weight: 600;
}

.compact-use-case-view :deep(.metric-action-buttons .primary-button),
.compact-use-case-view :deep(.metric-action-buttons .secondary-button) {
  min-width: 0;
  min-height: 32px;
  padding: 0 12px;
  font-size: 12px;
}

.factor-hint {
  margin: 0;
  padding: 0 14px 12px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.ucp-intro-card {
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid var(--color-border, #e5e6eb);
  border-radius: 8px;
  background: #ffffff;
}

.ucp-intro-card strong {
  color: var(--color-text, #1d2129);
  font-size: 13px;
}

.ucp-intro-card p {
  margin: 0;
  color: var(--color-text-secondary, #86909c);
  font-size: 12px;
  line-height: 1.6;
}

.ucp-overview-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.ucp-overview-card {
  display: grid;
  gap: 6px;
  min-height: 88px;
  padding: 14px 16px;
  border: 1px solid var(--color-border-soft, #f0f1f5);
  border-radius: 8px;
  background: #ffffff;
}

.ucp-overview-card span,
.ucp-overview-card small {
  color: var(--color-text-secondary, #86909c);
}

.ucp-overview-card span {
  font-size: 12px;
  font-weight: 600;
}

.ucp-overview-card strong {
  color: var(--color-text, #1d2129);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.03em;
}

.ucp-overview-card small {
  font-size: 11px;
}

.ucp-overview-card-accent {
  background: #f7faff;
}

.use-case-panel-list {
  display: grid;
  gap: 10px;
}

.use-case-panel {
  overflow: hidden;
  border: 1px solid var(--color-border, #e5e6eb);
  border-radius: 8px;
  background: #ffffff;
}

.use-case-panel summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  list-style: none;
}

.use-case-panel summary::-webkit-details-marker {
  display: none;
}

.use-case-panel summary::after {
  content: '+';
  color: var(--color-text-secondary, #86909c);
  font-size: 16px;
}

.use-case-panel[open] summary::after {
  content: '-';
}

.use-case-panel-title,
.use-case-panel-meta {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.use-case-panel-title strong,
.use-case-panel-meta span {
  color: var(--color-text, #1d2129);
  font-size: 14px;
  font-weight: 600;
}

.use-case-panel-title small,
.use-case-panel-meta em {
  color: var(--color-text-secondary, #86909c);
  font-size: 12px;
  font-style: normal;
}

.use-case-panel-body {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 0 16px 16px;
}

.use-case-panel-body-single {
  grid-template-columns: minmax(0, 240px);
}

.use-case-panel-body label {
  display: grid;
  gap: 6px;
}

.use-case-panel-body span {
  color: var(--color-text-secondary, #86909c);
  font-size: 12px;
  font-weight: 600;
}

.use-case-panel-body input {
  width: 100%;
  min-width: 0;
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-border, #e5e6eb);
  border-radius: 8px;
  background: #ffffff;
  outline: none;
}

.use-case-panel-body input:focus {
  border-color: var(--color-primary, #4080ff);
  box-shadow: 0 0 0 4px rgba(64, 128, 255, 0.12);
}

@media (max-width: 1180px) {
  .ucp-overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .ucp-overview-grid,
  .use-case-panel-body,
  .use-case-panel-body-single {
    grid-template-columns: 1fr;
  }

  .use-case-panel summary {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
