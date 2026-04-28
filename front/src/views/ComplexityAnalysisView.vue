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
  legacyWarningMessage: {
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

function methodSpan(method) {
  return Math.max(1, Number(method.endLine || 0) - Number(method.startLine || 0) + 1)
}

function fileShortName(fileName) {
  if (!fileName) {
    return '-'
  }
  return fileName.replace(/\.[^.]+$/, '')
}

function fileExtension(fileName) {
  if (!fileName || !fileName.includes('.')) {
    return 'file'
  }
  return fileName.split('.').pop().toLowerCase()
}

function scanStatusLabel(status) {
  if (!status) {
    return '-'
  }
  if (status.includes('接口')) {
    return '接口声明'
  }
  if (status.includes('抽象')) {
    return '抽象声明'
  }
  if (status.includes('跳过')) {
    return '已跳过'
  }
  if (status.includes('成功') || status.includes('完成') || status.includes('已扫描')) {
    return '已扫描'
  }
  return status
}

function scanStatusClass(status) {
  if (!status) {
    return ''
  }
  if (status.includes('接口') || status.includes('抽象')) {
    return 'neutral'
  }
  if (status.includes('跳过')) {
    return 'warning'
  }
  return 'success'
}

function complexityScatterPoints(methods) {
  const items = [...(methods || [])]
    .sort((left, right) => right.cyclomaticComplexity - left.cyclomaticComplexity)
    .slice(0, 20)
  const maxComplexity = Math.max(...items.map(item => Number(item.cyclomaticComplexity || 0)), 1)
  const maxSpan = Math.max(...items.map(item => methodSpan(item)), 1)
  return items.map(item => {
    const span = methodSpan(item)
    return {
      key: `${item.fileName}-${item.methodName}-${item.startLine}`,
      label: item.methodName,
      shortFile: fileShortName(item.fileName),
      cx: 26 + (span / maxSpan) * 288,
      cy: 176 - (Number(item.cyclomaticComplexity || 0) / maxComplexity) * 132,
      r: item.riskLevel === 'HIGH' || item.riskLevel === 'EXTREME' ? 7 : 5,
      level: item.riskLevel
    }
  })
}

function scatterPointClass(level) {
  if (level === 'HIGH' || level === 'EXTREME') {
    return 'danger'
  }
  if (level === 'MEDIUM') {
    return 'gold'
  }
  return 'blue'
}
</script>

<template>
  <section class="complexity-view">
    <MetricActionHeader
      title="复杂度分析"
      :loading="loading"
      primary-text="开始分析"
      primary-loading-text="分析中..."
      export-text="导出"
      :export-disabled="!result"
      @primary="$emit('analyze')"
      @export="$emit('export')"
    />
    <MetricStatusMessages
      :success-messages="[message, reportMessage]"
      :error-message="errorMessage"
      :warning-messages="[legacyWarningMessage]"
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

    <div v-if="!result" class="empty-state loc-empty">暂无结果</div>

    <div v-else class="loc-result complexity-result">
      <div class="loc-summary-grid complexity-stat-grid">
        <article>
          <span>文件</span>
          <strong>{{ result.summary.fileCount }}</strong>
        </article>
        <article>
          <span>方法</span>
          <strong>{{ result.summary.methodCount }}</strong>
        </article>
        <article>
          <span>均值</span>
          <strong>{{ result.summary.averageComplexity }}</strong>
        </article>
        <article>
          <span>高风险</span>
          <strong>{{ result.summary.highRiskMethodCount }}</strong>
        </article>
      </div>

      <section v-if="result.methods.length > 0" class="visual-card">
        <div class="visual-card-copy">
          <h3>复杂度散点图</h3>
          <p>每个点代表一个方法。横轴是方法跨度，纵轴是圈复杂度，颜色表示风险等级。</p>
        </div>
        <div class="scatter-chart">
          <svg viewBox="0 0 360 220" role="img" aria-label="复杂度散点图">
            <line x1="26" y1="24" x2="26" y2="184" class="chart-axis-line" />
            <line x1="26" y1="184" x2="334" y2="184" class="chart-axis-line" />
            <line x1="26" y1="52" x2="334" y2="52" class="chart-grid-line" />
            <line x1="26" y1="96" x2="334" y2="96" class="chart-grid-line" />
            <line x1="26" y1="140" x2="334" y2="140" class="chart-grid-line" />
            <circle
              v-for="point in complexityScatterPoints(result.methods)"
              :key="point.key"
              :cx="point.cx"
              :cy="point.cy"
              :r="point.r"
              class="scatter-point"
              :class="scatterPointClass(point.level)"
            />
          </svg>
          <div class="scatter-legend">
            <span><i class="blue"></i>低风险</span>
            <span><i class="gold"></i>中风险</span>
            <span><i class="danger"></i>高风险</span>
          </div>
          <div class="scatter-axis-labels">
            <span>Y 轴：圈复杂度</span>
            <span>X 轴：方法跨度（行）</span>
          </div>
        </div>
      </section>

      <section class="metric-card">
        <div class="metric-card-header">
          <h3>文件</h3>
          <small>{{ (result.files || []).length }} 个</small>
        </div>
        <div class="loc-table-wrap">
          <table class="loc-table complexity-table">
            <thead>
              <tr>
                <th>文件</th>
                <th>声明</th>
                <th>方法体</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="file in result.files || []"
                :key="`${file.sourceUploadName}-${file.fileName}`"
              >
                <td>
                  <div class="file-badge">
                    <span class="ext-tag">{{ fileExtension(file.fileName) }}</span>
                    {{ fileShortName(file.fileName) }}
                  </div>
                </td>
                <td>{{ file.declaredMethodCount }}</td>
                <td>{{ file.executableMethodCount }}</td>
                <td>
                  <span class="status-chip" :class="scanStatusClass(file.status)">
                    {{ scanStatusLabel(file.status) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="result.methods.length > 0" class="metric-card">
        <div class="metric-card-header">
          <h3>方法</h3>
          <small>{{ result.methods.length }} 个</small>
        </div>
        <div class="loc-table-wrap">
          <table class="loc-table complexity-table">
            <thead>
              <tr>
                <th>方法</th>
                <th>文件</th>
                <th>行号</th>
                <th>复杂度</th>
                <th>风险</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="method in result.methods"
                :key="`${method.sourceUploadName}-${method.fileName}-${method.startLine}-${method.methodName}`"
              >
                <td>{{ method.methodName }}</td>
                <td>{{ fileShortName(method.fileName) }}</td>
                <td>{{ method.startLine }}-{{ method.endLine }}</td>
                <td>{{ method.cyclomaticComplexity }}</td>
                <td>
                  <span class="risk-badge" :class="method.riskLevel.toLowerCase()">
                    {{ formatRiskLabel(method.riskLevel) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </section>
</template>
