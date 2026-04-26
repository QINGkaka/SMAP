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
  legacyWarningMessage: {
    type: String,
    default: ''
  },
  formatRiskLabel: {
    type: Function,
    required: true
  }
})

defineEmits(['analyze', 'export'])

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

    <div v-if="!result" class="empty-state loc-empty">暂无结果</div>

    <div v-else class="loc-result complexity-result">
      <div class="loc-summary-grid complexity-stat-grid">
        <article>
          <span>文件</span>
          <strong>{{ result.summary.fileCount }}</strong>
          <small>已扫描</small>
        </article>
        <article>
          <span>方法</span>
          <strong>{{ result.summary.methodCount }}</strong>
          <small>可分析</small>
        </article>
        <article>
          <span>均值</span>
          <strong>{{ result.summary.averageComplexity }}</strong>
          <small>复杂度</small>
        </article>
        <article>
          <span>高风险</span>
          <strong>{{ result.summary.highRiskMethodCount }}</strong>
          <small>&ge; 10</small>
        </article>
      </div>

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
        <div v-if="result.methods.length === 0" class="table-notice">
          接口与抽象声明不单独计入
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
