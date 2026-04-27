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
    supported-label=".xml、.xmi、.oom"
    @update:scope-mode="$emit('update:scopeMode', $event)"
    @toggle-file="$emit('toggle-file', $event)"
    @select-all="$emit('select-all-files')"
    @clear-selection="$emit('clear-selected-files')"
  />
  <div v-if="!result" class="empty-state loc-empty">
    暂无模型度量结果。请上传 `.xml`、`.xmi` 或 `.oom` 模型文件，然后点击“开始模型分析”。
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
