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
  formatPercent: {
    type: Function,
    required: true
  }
})

defineEmits(['analyze', 'export'])

function ratio(value, total) {
  if (!total) {
    return 0
  }
  return Math.max(0, Math.min(100, Number(((value / total) * 100).toFixed(1))))
}
</script>

<template>
  <MetricActionHeader
    eyebrow="代码行"
    title="代码行统计结果"
    :loading="loading"
    primary-text="开始 LoC 分析"
    primary-loading-text="分析中..."
    :export-disabled="!result"
    @primary="$emit('analyze')"
    @export="$emit('export')"
  />
  <MetricStatusMessages
    :success-messages="[message, reportMessage]"
    :error-message="errorMessage"
  />
  <div v-if="!result" class="empty-state loc-empty">
    暂无结果。上传 `.java` 或 `.zip` 后开始分析。
  </div>
  <div v-else class="loc-result">
    <div class="loc-summary-grid">
      <article>
        <span>Java 文件数</span>
        <strong>{{ result.summary.fileCount }}</strong>
      </article>
      <article>
        <span>总行数</span>
        <strong>{{ result.summary.totalLines }}</strong>
      </article>
      <article>
        <span>有效代码行</span>
        <strong>{{ result.summary.sourceLines }}</strong>
      </article>
      <article>
        <span>注释率</span>
        <strong>{{ formatPercent(result.summary.commentRate) }}</strong>
      </article>
    </div>
    <section class="visual-card">
      <div class="visual-card-copy">
        <h3>代码构成</h3>
        <p>展示总行数中有效代码、注释和空行各自所占比例。</p>
      </div>
      <div class="composition-bar">
        <span
          class="composition-segment source"
          :style="{ width: `${ratio(result.summary.sourceLines, result.summary.totalLines)}%` }"
        ></span>
        <span
          class="composition-segment comment"
          :style="{ width: `${ratio(result.summary.commentLines, result.summary.totalLines)}%` }"
        ></span>
        <span
          class="composition-segment blank"
          :style="{ width: `${ratio(result.summary.blankLines, result.summary.totalLines)}%` }"
        ></span>
      </div>
      <div class="visual-legend">
        <span><i class="source"></i>有效代码 {{ result.summary.sourceLines }}</span>
        <span><i class="comment"></i>注释 {{ result.summary.commentLines }}</span>
        <span><i class="blank"></i>空行 {{ result.summary.blankLines }}</span>
      </div>
    </section>
    <div class="loc-table-wrap">
      <table class="loc-table">
        <thead>
          <tr>
            <th>文件</th>
            <th>来源</th>
            <th>总行</th>
            <th>有效行</th>
            <th>注释行</th>
            <th>空行</th>
            <th>注释率</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="file in result.files" :key="`${file.sourceUploadName}-${file.fileName}`">
            <td>{{ file.fileName }}</td>
            <td>{{ file.sourceUploadName }}</td>
            <td>{{ file.totalLines }}</td>
            <td>{{ file.sourceLines }}</td>
            <td>{{ file.commentLines }}</td>
            <td>{{ file.blankLines }}</td>
            <td>{{ formatPercent(file.commentRate) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
