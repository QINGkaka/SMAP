<script setup>
import MetricActionHeader from '../components/metric/MetricActionHeader.vue'
import MetricFileScopePanel from '../components/metric/MetricFileScopePanel.vue'
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
  scopeMode: {
    type: String,
    default: 'project'
  },
  availableFiles: {
    type: Array,
    default: () => []
  },
  selectedFileIds: {
    type: Array,
    default: () => []
  },
  formatRiskLabel: {
    type: Function,
    required: true
  }
})

defineEmits([
  'analyze',
  'export',
  'update:scopeMode',
  'toggle-file',
  'select-all-files',
  'clear-selected-files'
])

function structureItems(summary) {
  const values = [
    { label: '类', value: summary.classCount, tone: 'blue' },
    { label: '接口', value: summary.interfaceCount, tone: 'cyan' },
    { label: '属性', value: summary.attributeCount, tone: 'gold' },
    { label: '操作', value: summary.operationCount, tone: 'green' }
  ]
  const max = Math.max(...values.map(item => item.value), 1)
  return values.map(item => ({
    ...item,
    width: 56,
    height: 32 + (item.value / max) * 124
  }))
}
</script>

<template>
  <MetricActionHeader
    eyebrow="模型文件"
    title="模型文件度量结果"
    :loading="loading"
    primary-text="开始模型分析"
    primary-loading-text="分析中..."
    :export-disabled="!result"
    @primary="$emit('analyze')"
    @export="$emit('export')"
  />
  <MetricStatusMessages
    :success-messages="[message, reportMessage]"
    :error-message="errorMessage"
  />
  <MetricFileScopePanel
    :scope-mode="scopeMode"
    :available-files="availableFiles"
    :selected-file-ids="selectedFileIds"
    supported-label="类图模型 .xml、.xmi、.oom"
    @update:scope-mode="$emit('update:scopeMode', $event)"
    @toggle-file="$emit('toggle-file', $event)"
    @select-all="$emit('select-all-files')"
    @clear-selection="$emit('clear-selected-files')"
  />
  <div v-if="!result" class="empty-state loc-empty">
    暂无模型度量结果。请上传包含类、接口、属性、操作或继承关系的类图模型文件（`.xml`、`.xmi`、`.oom`），然后点击“开始模型分析”。
  </div>
  <div v-else class="loc-result">
    <div class="loc-summary-grid">
      <article>
        <span>模型文件数</span>
        <strong>{{ result.summary.fileCount }}</strong>
      </article>
      <article>
        <span>类/接口数</span>
        <strong>{{ result.summary.classCount + result.summary.interfaceCount }}</strong>
      </article>
      <article>
        <span>属性/操作</span>
        <strong>{{ result.summary.attributeCount + result.summary.operationCount }}</strong>
      </article>
      <article>
        <span>高风险类</span>
        <strong>{{ result.summary.highRiskClassCount }}</strong>
      </article>
    </div>
    <section class="visual-card">
      <div class="visual-card-copy">
        <h3>模型结构分布</h3>
        <p>柱高表示模型实体数量，用来对比类、接口、属性和操作的规模差异。</p>
      </div>
      <div class="column-chart">
        <svg viewBox="0 0 320 220" role="img" aria-label="模型结构柱状图">
          <line x1="28" y1="184" x2="296" y2="184" class="chart-axis-line" />
          <g v-for="(item, index) in structureItems(result.summary)" :key="item.label">
            <rect
              :x="44 + index * 66"
              :y="184 - item.height"
              :width="item.width"
              :height="item.height"
              rx="10"
              class="column-bar"
              :class="item.tone"
            />
            <text :x="72 + index * 66" y="202" text-anchor="middle" class="chart-text-label">{{ item.label }}</text>
            <text :x="72 + index * 66" :y="174 - item.height" text-anchor="middle" class="chart-text-value">{{ item.value }}</text>
          </g>
        </svg>
      </div>
    </section>
    <div class="loc-table-wrap">
      <table class="loc-table oo-table">
        <thead>
          <tr>
            <th>类/接口</th>
            <th>类型</th>
            <th>来源</th>
            <th>属性</th>
            <th>操作</th>
            <th>子类</th>
            <th>继承深度</th>
            <th>父类</th>
            <th>风险</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in result.classes" :key="`${item.sourceUploadName}-${item.className}`">
            <td>{{ item.className }}</td>
            <td>{{ item.type }}</td>
            <td>{{ item.sourceUploadName }}</td>
            <td>{{ item.attributeCount }}</td>
            <td>{{ item.operationCount }}</td>
            <td>{{ item.childCount }}</td>
            <td>{{ item.inheritanceDepth }}</td>
            <td>{{ item.parentName || '-' }}</td>
            <td>
              <span class="risk-badge" :class="item.riskLevel.toLowerCase()">
                {{ formatRiskLabel(item.riskLevel) }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
