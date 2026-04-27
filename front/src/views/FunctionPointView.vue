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

defineEmits([
  'analyze',
  'export',
  'update-mode',
  'update-count',
  'add-detail',
  'remove-detail',
  'update-detail',
  'update-gsc'
])

const manualWeights = {
  EI: { low: 3, average: 4, high: 6 },
  EO: { low: 4, average: 5, high: 7 },
  EQ: { low: 3, average: 4, high: 6 },
  ILF: { low: 7, average: 10, high: 15 },
  EIF: { low: 5, average: 7, high: 10 }
}

const sections = computed(() => [
  {
    key: 'externalInputs',
    code: 'EI',
    title: '外部输入',
    detailKey: 'externalInputDetails',
    relationKey: 'ftr',
    relationLabel: 'FTR',
    sizeLabel: 'DER',
    detailHint: '事务功能按课件中的 DER + FTR 自动判定复杂度',
    values: props.form.externalInputs,
    details: props.form.externalInputDetails || []
  },
  {
    key: 'externalOutputs',
    code: 'EO',
    title: '外部输出',
    detailKey: 'externalOutputDetails',
    relationKey: 'ftr',
    relationLabel: 'FTR',
    sizeLabel: 'DER',
    detailHint: '事务功能按课件中的 DER + FTR 自动判定复杂度',
    values: props.form.externalOutputs,
    details: props.form.externalOutputDetails || []
  },
  {
    key: 'externalInquiries',
    code: 'EQ',
    title: '外部查询',
    detailKey: 'externalInquiryDetails',
    relationKey: 'ftr',
    relationLabel: 'FTR',
    sizeLabel: 'DER',
    detailHint: '事务功能按课件中的 DER + FTR 自动判定复杂度',
    values: props.form.externalInquiries,
    details: props.form.externalInquiryDetails || []
  },
  {
    key: 'internalLogicalFiles',
    code: 'ILF',
    title: '内部逻辑文件',
    detailKey: 'internalLogicalFileDetails',
    relationKey: 'ret',
    relationLabel: 'RET',
    sizeLabel: 'DET',
    detailHint: '数据功能按 DET + RET 自动判定复杂度',
    values: props.form.internalLogicalFiles,
    details: props.form.internalLogicalFileDetails || []
  },
  {
    key: 'externalInterfaceFiles',
    code: 'EIF',
    title: '外部接口文件',
    detailKey: 'externalInterfaceFileDetails',
    relationKey: 'ret',
    relationLabel: 'RET',
    sizeLabel: 'DET',
    detailHint: '数据功能按 DET + RET 自动判定复杂度',
    values: props.form.externalInterfaceFiles,
    details: props.form.externalInterfaceFileDetails || []
  }
])

function toNumber(value) {
  const next = Number(value)
  return Number.isFinite(next) ? next : 0
}

function classifyExternalInput(det, ftr) {
  if (det <= 4) {
    return ftr >= 3 ? 'average' : 'low'
  }
  if (det <= 15) {
    if (ftr <= 1) {
      return 'low'
    }
    return ftr === 2 ? 'average' : 'high'
  }
  if (ftr <= 1) {
    return 'average'
  }
  return 'high'
}

function classifyOutputOrInquiry(det, ftr) {
  if (det <= 5) {
    return ftr <= 3 ? 'low' : 'average'
  }
  if (det <= 19) {
    if (ftr <= 1) {
      return 'low'
    }
    return ftr <= 3 ? 'average' : 'high'
  }
  if (ftr <= 1) {
    return 'average'
  }
  return 'high'
}

function classifyDataFunction(det, ret) {
  if (det <= 19) {
    return ret <= 5 ? 'low' : 'average'
  }
  if (det <= 50) {
    if (ret === 1) {
      return 'low'
    }
    return ret <= 5 ? 'average' : 'high'
  }
  if (ret === 1) {
    return 'average'
  }
  return 'high'
}

function levelLabel(level) {
  return level === 'low' ? '低' : level === 'average' ? '中' : '高'
}

function pointWeight(code, level) {
  return manualWeights[code]?.[level] || 0
}

function isMeaningfulDetail(detail) {
  if (!detail) {
    return false
  }
  return String(detail.name || '').trim() !== ''
    || toNumber(detail.det) > 0
    || toNumber(detail.ret) > 0
    || toNumber(detail.ftr) > 0
}

function summarizeSection(section) {
  if (props.form.countMode !== 'DETAILED') {
    const low = toNumber(section.values.low)
    const average = toNumber(section.values.average)
    const high = toNumber(section.values.high)
    return {
      itemCount: low + average + high,
      lowCount: low,
      averageCount: average,
      highCount: high,
      functionPoints:
        low * pointWeight(section.code, 'low')
        + average * pointWeight(section.code, 'average')
        + high * pointWeight(section.code, 'high')
    }
  }

  return section.details
    .filter(isMeaningfulDetail)
    .reduce((summary, detail) => {
      const det = Math.max(0, toNumber(detail.det))
      const relation = Math.max(0, toNumber(detail[section.relationKey]))
      if (det <= 0 || relation <= 0) {
        return summary
      }
      const level = section.relationKey === 'ftr'
        ? (section.code === 'EI' ? classifyExternalInput(det, relation) : classifyOutputOrInquiry(det, relation))
        : classifyDataFunction(det, relation)
      summary.itemCount += 1
      summary.functionPoints += pointWeight(section.code, level)
      if (level === 'low') {
        summary.lowCount += 1
      } else if (level === 'average') {
        summary.averageCount += 1
      } else {
        summary.highCount += 1
      }
      return summary
    }, { itemCount: 0, lowCount: 0, averageCount: 0, highCount: 0, functionPoints: 0 })
}

const previewSummaries = computed(() => sections.value.map(section => ({
  ...section,
  summary: summarizeSection(section)
})))

const previewUfp = computed(() => previewSummaries.value.reduce((total, section) => total + section.summary.functionPoints, 0))
const previewVaf = computed(() => Math.round((0.65 + 0.01 * props.gscTotal) * 100) / 100)
const previewAfp = computed(() => Math.round(previewUfp.value * previewVaf.value * 100) / 100)

const resultComponentSummaries = computed(() => {
  if (props.result?.componentSummaries?.length) {
    return props.result.componentSummaries
  }
  if (!props.result) {
    return []
  }
  return [
    { code: 'EI', label: '外部输入', itemCount: 0, lowCount: 0, averageCount: 0, highCount: 0, functionPoints: props.result.externalInputs || 0 },
    { code: 'EO', label: '外部输出', itemCount: 0, lowCount: 0, averageCount: 0, highCount: 0, functionPoints: props.result.externalOutputs || 0 },
    { code: 'EQ', label: '外部查询', itemCount: 0, lowCount: 0, averageCount: 0, highCount: 0, functionPoints: props.result.externalInquiries || 0 },
    { code: 'ILF', label: '内部逻辑文件', itemCount: 0, lowCount: 0, averageCount: 0, highCount: 0, functionPoints: props.result.internalLogicalFiles || 0 },
    { code: 'EIF', label: '外部接口文件', itemCount: 0, lowCount: 0, averageCount: 0, highCount: 0, functionPoints: props.result.externalInterfaceFiles || 0 }
  ]
})
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

    <section class="mode-panel">
      <div class="mode-panel-head">
        <div>
          <strong>IFPUG 计数方式</strong>
          <p>详细计数使用课件里的 DET / FTR / RET 判级规则，预估算模式保留低中高人工录入。</p>
        </div>
        <div class="mode-toggle">
          <button
            type="button"
            class="mode-button"
            :class="{ active: form.countMode === 'DETAILED' }"
            @click="$emit('update-mode', 'DETAILED')"
          >
            详细计数
          </button>
          <button
            type="button"
            class="mode-button"
            :class="{ active: form.countMode === 'ESTIMATED' }"
            @click="$emit('update-mode', 'ESTIMATED')"
          >
            预估算
          </button>
        </div>
      </div>

      <div class="fp-overview-grid">
        <article v-for="section in previewSummaries" :key="section.code" class="fp-overview-card">
          <span>{{ section.code }}</span>
          <strong>{{ section.summary.functionPoints }}</strong>
          <small>{{ section.title }} · {{ section.summary.itemCount }} 项</small>
          <em>{{ section.summary.lowCount }}/{{ section.summary.averageCount }}/{{ section.summary.highCount }}</em>
        </article>
        <article class="fp-overview-card fp-overview-card-accent">
          <span>UFP</span>
          <strong>{{ previewUfp }}</strong>
          <small>VAF {{ previewVaf }}</small>
          <em>AFP {{ previewAfp }}</em>
        </article>
      </div>
    </section>

    <div class="function-point-panel-list">
      <details
        v-for="(section, index) in previewSummaries"
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
            <span>{{ section.summary.functionPoints }} FP</span>
            <em>{{ section.summary.lowCount }}/{{ section.summary.averageCount }}/{{ section.summary.highCount }}</em>
          </div>
        </summary>

        <div v-if="form.countMode === 'DETAILED'" class="function-point-panel-body detailed-body">
          <div class="detail-hint">{{ section.detailHint }}</div>
          <div class="detail-table">
            <div class="detail-table-head">
              <span>功能项</span>
              <span>{{ section.sizeLabel }}</span>
              <span>{{ section.relationLabel }}</span>
              <span>复杂度</span>
              <span>功能点</span>
              <span></span>
            </div>

            <div
              v-for="(detail, detailIndex) in section.details"
              :key="`${section.detailKey}-${detailIndex}`"
              class="detail-row"
            >
              <input
                :value="detail.name"
                type="text"
                placeholder="例如：新增学生"
                @input="$emit('update-detail', { section: section.detailKey, index: detailIndex, field: 'name', value: $event.target.value })"
              />
              <input
                :value="detail.det"
                type="number"
                min="0"
                @input="$emit('update-detail', { section: section.detailKey, index: detailIndex, field: 'det', value: Number($event.target.value) || 0 })"
              />
              <input
                :value="detail[section.relationKey]"
                type="number"
                min="0"
                @input="$emit('update-detail', { section: section.detailKey, index: detailIndex, field: section.relationKey, value: Number($event.target.value) || 0 })"
              />
              <span class="detail-chip">
                {{
                  Math.max(0, Number(detail.det || 0)) > 0 && Math.max(0, Number(detail[section.relationKey] || 0)) > 0
                    ? levelLabel(
                      section.relationKey === 'ftr'
                        ? (section.code === 'EI'
                          ? classifyExternalInput(Math.max(0, Number(detail.det || 0)), Math.max(0, Number(detail.ftr || 0)))
                          : classifyOutputOrInquiry(Math.max(0, Number(detail.det || 0)), Math.max(0, Number(detail.ftr || 0))))
                        : classifyDataFunction(Math.max(0, Number(detail.det || 0)), Math.max(0, Number(detail.ret || 0)))
                    )
                    : '-'
                }}
              </span>
              <span class="detail-points">
                {{
                  Math.max(0, Number(detail.det || 0)) > 0 && Math.max(0, Number(detail[section.relationKey] || 0)) > 0
                    ? pointWeight(
                      section.code,
                      section.relationKey === 'ftr'
                        ? (section.code === 'EI'
                          ? classifyExternalInput(Math.max(0, Number(detail.det || 0)), Math.max(0, Number(detail.ftr || 0)))
                          : classifyOutputOrInquiry(Math.max(0, Number(detail.det || 0)), Math.max(0, Number(detail.ftr || 0))))
                        : classifyDataFunction(Math.max(0, Number(detail.det || 0)), Math.max(0, Number(detail.ret || 0)))
                    )
                    : 0
                }}
              </span>
              <button
                type="button"
                class="icon-button"
                title="删除这一项"
                @click="$emit('remove-detail', { section: section.detailKey, index: detailIndex })"
              >
                −
              </button>
            </div>
          </div>

          <button
            type="button"
            class="add-detail-button"
            @click="$emit('add-detail', section.detailKey)"
          >
            + 添加 {{ section.code }}
          </button>
        </div>

        <div v-else class="function-point-panel-body">
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
      <p class="factor-hint">VAF = 0.65 + 0.01 × GSC，总分按 14 个通用系统特征的 0-5 评分累加。</p>
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
          <span>模式</span>
          <strong>{{ result.countMode === 'DETAILED' ? '详细计数' : '预估算' }}</strong>
        </article>
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
      </div>

      <div class="result-table-card">
        <div class="result-table-head">
          <strong>组件统计</strong>
          <small>系统按课件公式自动汇总功能点</small>
        </div>
        <table class="result-table">
          <thead>
            <tr>
              <th>类别</th>
              <th>项数</th>
              <th>低</th>
              <th>中</th>
              <th>高</th>
              <th>功能点</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in resultComponentSummaries" :key="item.code">
              <td>{{ item.label }} {{ item.code }}</td>
              <td>{{ item.itemCount }}</td>
              <td>{{ item.lowCount }}</td>
              <td>{{ item.averageCount }}</td>
              <td>{{ item.highCount }}</td>
              <td>{{ item.functionPoints }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="result.detailItems && result.detailItems.length" class="result-table-card">
        <div class="result-table-head">
          <strong>详细计数明细</strong>
          <small>事务功能使用 DER + FTR，数据功能使用 DET + RET</small>
        </div>
        <table class="result-table">
          <thead>
            <tr>
              <th>类别</th>
              <th>功能项</th>
              <th>DET/DER</th>
              <th>FTR/RET</th>
              <th>复杂度</th>
              <th>功能点</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in result.detailItems" :key="`${item.code}-${index}`">
              <td>{{ item.code }}</td>
              <td>{{ item.name }}</td>
              <td>{{ item.det }}</td>
              <td>{{ item.ftr ?? item.ret ?? '-' }}</td>
              <td>{{ item.complexity }}</td>
              <td>{{ item.functionPoints }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<style scoped>
.compact-function-point-view {
  display: grid;
  gap: 14px;
}

.mode-panel,
.function-point-panel,
.function-point-toggle,
.result-table-card {
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
}

.mode-panel {
  padding: 16px 18px;
  display: grid;
  gap: 14px;
}

.mode-panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.mode-panel-head strong,
.result-table-head strong {
  display: block;
  color: #f8fafc;
}

.mode-panel-head p,
.result-table-head small,
.detail-hint,
.factor-hint {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.mode-toggle {
  display: inline-flex;
  border: 1px solid rgba(96, 165, 250, 0.28);
  border-radius: 8px;
  overflow: hidden;
}

.mode-button {
  border: 0;
  background: transparent;
  color: #cbd5e1;
  padding: 10px 14px;
  font: inherit;
  cursor: pointer;
  min-width: 88px;
}

.mode-button.active {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.88), rgba(14, 165, 233, 0.82));
  color: #eff6ff;
}

.fp-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(148px, 1fr));
  gap: 12px;
}

.fp-overview-card {
  display: grid;
  gap: 4px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.52);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.fp-overview-card span,
.fp-overview-card small,
.fp-overview-card em {
  color: #94a3b8;
  font-style: normal;
  font-size: 12px;
}

.fp-overview-card strong {
  color: #f8fafc;
  font-size: 22px;
}

.fp-overview-card-accent {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.92), rgba(30, 64, 175, 0.72));
}

.function-point-panel-list {
  display: grid;
  gap: 12px;
}

.function-point-panel summary,
.function-point-toggle summary {
  list-style: none;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
}

.function-point-panel summary::-webkit-details-marker,
.function-point-toggle summary::-webkit-details-marker {
  display: none;
}

.function-point-panel-title,
.function-point-panel-meta {
  display: grid;
  gap: 4px;
}

.function-point-panel-title strong,
.function-point-panel-meta span,
.function-point-toggle summary strong {
  color: #f8fafc;
}

.function-point-panel-title small,
.function-point-panel-meta em,
.function-point-toggle summary span {
  color: #94a3b8;
  font-style: normal;
}

.function-point-panel-body {
  padding: 0 18px 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.function-point-panel-body label,
.factor-score-grid label {
  display: grid;
  gap: 6px;
}

.function-point-panel-body span,
.factor-score-grid span,
.detail-table-head span {
  color: #cbd5e1;
  font-size: 12px;
}

.function-point-panel-body input,
.factor-score-grid input,
.detail-row input {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.78);
  color: #f8fafc;
  padding: 10px 12px;
  font: inherit;
}

.function-point-panel-body input:focus,
.factor-score-grid input:focus,
.detail-row input:focus {
  outline: none;
  border-color: rgba(96, 165, 250, 0.72);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.16);
}

.detailed-body {
  grid-template-columns: 1fr;
}

.detail-table {
  display: grid;
  gap: 8px;
}

.detail-table-head,
.detail-row {
  display: grid;
  grid-template-columns: minmax(180px, 2fr) repeat(2, minmax(76px, 92px)) minmax(68px, 88px) minmax(68px, 88px) 44px;
  gap: 8px;
  align-items: center;
}

.detail-row {
  padding: 10px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.42);
}

.detail-chip,
.detail-points {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(30, 41, 59, 0.82);
  color: #e2e8f0;
  font-size: 13px;
}

.icon-button,
.add-detail-button {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: rgba(30, 41, 59, 0.8);
  color: #e2e8f0;
  cursor: pointer;
  font: inherit;
}

.icon-button {
  width: 40px;
  height: 40px;
  font-size: 22px;
  line-height: 1;
}

.add-detail-button {
  justify-self: start;
  padding: 10px 14px;
}

.function-point-toggle {
  padding-bottom: 18px;
}

.factor-hint {
  padding: 0 18px;
}

.factor-score-panel.embedded {
  padding: 0 18px;
}

.factor-score-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.loc-result {
  display: grid;
  gap: 12px;
}

.loc-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.loc-summary-grid article {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.loc-summary-grid span {
  color: #94a3b8;
  font-size: 12px;
}

.loc-summary-grid strong {
  color: #f8fafc;
}

.result-table-card {
  padding: 16px 18px 18px;
  display: grid;
  gap: 12px;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.result-table th,
.result-table td {
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  padding: 10px 8px;
  text-align: left;
  color: #e2e8f0;
}

.result-table th {
  color: #94a3b8;
  font-weight: 600;
}

@media (max-width: 960px) {
  .mode-panel-head {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-table-head,
  .detail-row {
    grid-template-columns: minmax(120px, 1.8fr) repeat(2, minmax(64px, 1fr)) minmax(54px, 0.8fr) minmax(54px, 0.8fr) 40px;
  }
}

@media (max-width: 720px) {
  .function-point-panel-body {
    grid-template-columns: 1fr;
  }

  .detail-table-head {
    display: none;
  }

  .detail-row {
    grid-template-columns: 1fr 1fr;
  }

  .icon-button,
  .detail-chip,
  .detail-points {
    min-height: 42px;
  }
}
</style>
