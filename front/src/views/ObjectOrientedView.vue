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
  radarAxes: {
    type: Array,
    default: () => []
  },
  radarSeries: {
    type: Array,
    default: () => []
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
    eyebrow="面向对象"
    title="面向对象度量结果"
    :loading="loading"
    primary-text="开始 CK/LK 分析"
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
    supported-label=".java、.zip"
    @update:scope-mode="$emit('update:scopeMode', $event)"
    @toggle-file="$emit('toggle-file', $event)"
    @select-all="$emit('select-all-files')"
    @clear-selection="$emit('clear-selected-files')"
  />
  <div v-if="!result" class="empty-state loc-empty">
    暂无面向对象度量结果。请先上传 `.java` 文件或包含 Java 文件的 `.zip`，然后点击“开始 CK/LK 分析”。
  </div>
  <div v-else class="loc-result">
    <div class="loc-summary-grid">
      <article>
        <span>类/接口数</span>
        <strong>{{ result.summary.classCount + result.summary.interfaceCount }}</strong>
      </article>
      <article>
        <span>平均 CBO</span>
        <strong>{{ result.summary.averageCbo }}</strong>
      </article>
      <article>
        <span>平均 RFC</span>
        <strong>{{ result.summary.averageRfc }}</strong>
      </article>
      <article>
        <span>风险类</span>
        <strong>{{ result.summary.highRiskClassCount }}</strong>
      </article>
    </div>
    <div class="oo-explain">
      <span>CK：CBO、RFC、DIT、NOC、WMC、LCOM</span>
      <span>LK：NOA、NOO、CS</span>
    </div>
    <div class="radar-wrap">
      <div class="radar-grid">
        <span v-for="axis in radarAxes" :key="axis" class="axis-label">{{ axis }}</span>
        <svg viewBox="0 0 240 240" role="img" aria-label="CK LK 雷达图">
          <polygon class="grid-line" points="120,34 194.5,77 194.5,163 120,206 45.5,163 45.5,77" />
          <polygon class="grid-line" points="120,62 170.2,91 170.2,149 120,178 69.8,149 69.8,91" />
          <polygon class="grid-line" points="120,90 146,105 146,135 120,150 94,135 94,105" />
          <line class="axis-line" x1="120" y1="120" x2="120" y2="34" />
          <line class="axis-line" x1="120" y1="120" x2="194.5" y2="77" />
          <line class="axis-line" x1="120" y1="120" x2="194.5" y2="163" />
          <line class="axis-line" x1="120" y1="120" x2="120" y2="206" />
          <line class="axis-line" x1="120" y1="120" x2="45.5" y2="163" />
          <line class="axis-line" x1="120" y1="120" x2="45.5" y2="77" />
          <polygon
            v-for="series in radarSeries"
            :key="series.name"
            class="data-line"
            :class="series.color"
            :points="series.points"
          />
        </svg>
      </div>
      <div class="legend">
        <span v-for="series in radarSeries" :key="series.name">
          <i :class="series.color"></i>{{ series.name }}
        </span>
      </div>
    </div>
    <div class="loc-table-wrap">
      <table class="loc-table oo-table">
        <thead>
          <tr>
            <th>类/接口</th>
            <th>类型</th>
            <th>文件</th>
            <th>CBO</th>
            <th>RFC</th>
            <th>DIT</th>
            <th>NOC</th>
            <th>NOA</th>
            <th>NOO</th>
            <th>CS</th>
            <th>WMC</th>
            <th>LCOM</th>
            <th>风险</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in result.classes" :key="`${item.sourceUploadName}-${item.className}`">
            <td>{{ item.className }}</td>
            <td>{{ item.type }}</td>
            <td>{{ item.fileName }}</td>
            <td>{{ item.cbo }}</td>
            <td>{{ item.rfc }}</td>
            <td>{{ item.dit }}</td>
            <td>{{ item.noc }}</td>
            <td>{{ item.noa }}</td>
            <td>{{ item.noo }}</td>
            <td>{{ item.cs }}</td>
            <td>{{ item.wmc }}</td>
            <td>{{ item.lcom }}</td>
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
