import { ref, watch } from 'vue'
import {
  analyzeProjectAi,
  analyzeProjectComplexity,
  analyzeProjectEstimation,
  analyzeProjectFunctionPoint,
  analyzeProjectLoc,
  analyzeProjectModel,
  analyzeProjectOo,
  analyzeProjectUseCasePoint,
  exportComplexityReport,
  exportEstimationReport,
  exportFunctionPointReport,
  exportLocReport,
  exportModelReport,
  exportOoReport,
  exportUseCasePointReport,
  fetchLatestAiResult,
  fetchLatestComplexityResult,
  fetchLatestEstimationResult,
  fetchFunctionPointAssist,
  fetchFunctionPointDraft,
  fetchLatestFunctionPointResult,
  fetchLatestLocResult,
  fetchLatestModelResult,
  fetchLatestOoResult,
  fetchUseCasePointAssist,
  fetchLatestUseCasePointResult,
  fetchUseCasePointDraft
} from '../services/api'

function clampScore(value) {
  const score = Number(value)
  if (Number.isNaN(score)) {
    return 0
  }
  return Math.min(Math.max(score, 0), 5)
}

function scoreTotal(values) {
  return values.reduce((total, value) => total + clampScore(value), 0)
}

const useCaseTechnicalWeights = [2, 1, 1, 1, 1, 0.5, 0.5, 2, 1, 1, 1, 1, 1]
const useCaseEnvironmentalWeights = [1.5, 0.5, 1, 0.5, 1, 2, -1, -1]
const uploadDependentMetricKeys = ['loc', 'control-flow', 'object-oriented', 'estimation', 'ai', 'model-analysis']

function createDefaultEstimationForm() {
  return {
    mode: 'ORGANIC',
    kloc: '',
    costPerPersonMonth: 20000
  }
}

function createDefaultFunctionPointForm() {
  return {
    countMode: 'DETAILED',
    externalInputs: { low: 0, average: 0, high: 0 },
    externalOutputs: { low: 0, average: 0, high: 0 },
    externalInquiries: { low: 0, average: 0, high: 0 },
    internalLogicalFiles: { low: 0, average: 0, high: 0 },
    externalInterfaceFiles: { low: 0, average: 0, high: 0 },
    externalInputDetails: [{ name: '', det: 0, ftr: 0, ret: null }],
    externalOutputDetails: [{ name: '', det: 0, ftr: 0, ret: null }],
    externalInquiryDetails: [{ name: '', det: 0, ftr: 0, ret: null }],
    internalLogicalFileDetails: [{ name: '', det: 0, ret: 0, ftr: null }],
    externalInterfaceFileDetails: [{ name: '', det: 0, ret: 0, ftr: null }],
    generalSystemCharacteristicTotal: 35,
    generalSystemCharacteristics: [3, 2, 3, 2, 3, 3, 2, 3, 2, 3, 2, 3, 2, 2]
  }
}

function createDefaultUseCasePointForm() {
  return {
    simpleActors: 0,
    averageActors: 0,
    complexActors: 0,
    simpleUseCases: 0,
    averageUseCases: 0,
    complexUseCases: 0,
    technicalFactorTotal: 30,
    environmentalFactorTotal: 20,
    productivityHoursPerUseCasePoint: 28,
    technicalFactors: [2, 3, 2, 3, 2, 2, 3, 2, 2, 2, 2, 2, 3],
    environmentalFactors: [3, 2, 2, 3, 3, 2, 2, 3]
  }
}

function cloneForm(value) {
  return JSON.parse(JSON.stringify(value))
}

function weightedScoreTotal(values, weights) {
  return Math.round(values.reduce((total, value, index) => {
    return total + clampScore(value) * (weights[index] || 0)
  }, 0) * 100) / 100
}

export function useMetricModules({ selectedProjectId, activeMenu, downloadMarkdown, analysisScopePayloadFor }) {
  const locResult = ref(null)
  const locLoading = ref(false)
  const locError = ref('')
  const locMessage = ref('')
  const locReportMessage = ref('')

  const complexityResult = ref(null)
  const complexityLoading = ref(false)
  const complexityError = ref('')
  const complexityMessage = ref('')
  const complexityReportMessage = ref('')

  const ooResult = ref(null)
  const ooLoading = ref(false)
  const ooError = ref('')
  const ooMessage = ref('')
  const ooReportMessage = ref('')

  const estimationResult = ref(null)
  const estimationLoading = ref(false)
  const estimationError = ref('')
  const estimationMessage = ref('')
  const estimationReportMessage = ref('')

  const aiResult = ref(null)
  const aiLoading = ref(false)
  const aiError = ref('')
  const aiMessage = ref('')

  const functionPointResult = ref(null)
  const functionPointLoading = ref(false)
  const functionPointError = ref('')
  const functionPointMessage = ref('')
  const functionPointReportMessage = ref('')

  const useCasePointResult = ref(null)
  const useCasePointLoading = ref(false)
  const useCasePointError = ref('')
  const useCasePointMessage = ref('')
  const useCasePointReportMessage = ref('')

  const modelResult = ref(null)
  const modelLoading = ref(false)
  const modelError = ref('')
  const modelMessage = ref('')
  const modelReportMessage = ref('')

  const estimationForm = ref(createDefaultEstimationForm())
  const functionPointForm = ref(createDefaultFunctionPointForm())
  const useCasePointForm = ref(createDefaultUseCasePointForm())
  const estimationFormByProject = ref({})
  const functionPointFormByProject = ref({})
  const useCasePointFormByProject = ref({})
  let functionPointDraftRequestToken = 0
  let useCaseDraftRequestToken = 0

  function functionPointGscTotal() {
    return scoreTotal(functionPointForm.value.generalSystemCharacteristics)
  }

  function useCaseTechnicalTotal() {
    return weightedScoreTotal(useCasePointForm.value.technicalFactors, useCaseTechnicalWeights)
  }

  function useCaseEnvironmentalTotal() {
    return weightedScoreTotal(useCasePointForm.value.environmentalFactors, useCaseEnvironmentalWeights)
  }

  function saveProjectScopedForms(projectId) {
    if (!projectId) {
      return
    }
    estimationFormByProject.value = {
      ...estimationFormByProject.value,
      [projectId]: cloneForm(estimationForm.value)
    }
    functionPointFormByProject.value = {
      ...functionPointFormByProject.value,
      [projectId]: cloneForm(functionPointForm.value)
    }
    useCasePointFormByProject.value = {
      ...useCasePointFormByProject.value,
      [projectId]: cloneForm(useCasePointForm.value)
    }
  }

  function restoreProjectScopedForms(projectId) {
    estimationForm.value = projectId && estimationFormByProject.value[projectId]
      ? cloneForm(estimationFormByProject.value[projectId])
      : createDefaultEstimationForm()
    functionPointForm.value = createDefaultFunctionPointForm()
    useCasePointForm.value = createDefaultUseCasePointForm()
  }

  async function hydrateFunctionPointForm(projectId) {
    if (!projectId) {
      functionPointForm.value = createDefaultFunctionPointForm()
      return
    }
    if (functionPointFormByProject.value[projectId]) {
      functionPointForm.value = cloneForm(functionPointFormByProject.value[projectId])
      return
    }
    const requestToken = ++functionPointDraftRequestToken
    try {
      const result = await fetchFunctionPointDraft(projectId)
      if (requestToken !== functionPointDraftRequestToken || selectedProjectId.value !== projectId) {
        return
      }
      const nextForm = result?.data ? cloneForm(result.data) : createDefaultFunctionPointForm()
      functionPointForm.value = nextForm
      functionPointFormByProject.value = {
        ...functionPointFormByProject.value,
        [projectId]: cloneForm(nextForm)
      }
    } catch {
      if (requestToken !== functionPointDraftRequestToken || selectedProjectId.value !== projectId) {
        return
      }
      functionPointForm.value = createDefaultFunctionPointForm()
    }
  }

  async function hydrateUseCasePointForm(projectId) {
    if (!projectId) {
      useCasePointForm.value = createDefaultUseCasePointForm()
      return
    }
    if (useCasePointFormByProject.value[projectId]) {
      useCasePointForm.value = cloneForm(useCasePointFormByProject.value[projectId])
      return
    }
    const requestToken = ++useCaseDraftRequestToken
    try {
      const result = await fetchUseCasePointDraft(projectId)
      if (requestToken !== useCaseDraftRequestToken || selectedProjectId.value !== projectId) {
        return
      }
      const nextForm = result?.data ? cloneForm(result.data) : createDefaultUseCasePointForm()
      useCasePointForm.value = nextForm
      useCasePointFormByProject.value = {
        ...useCasePointFormByProject.value,
        [projectId]: cloneForm(nextForm)
      }
    } catch {
      if (requestToken !== useCaseDraftRequestToken || selectedProjectId.value !== projectId) {
        return
      }
      useCasePointForm.value = createDefaultUseCasePointForm()
    }
  }

  watch(selectedProjectId, (newProjectId, oldProjectId) => {
    saveProjectScopedForms(oldProjectId)
    restoreProjectScopedForms(newProjectId)
    hydrateFunctionPointForm(newProjectId)
    hydrateUseCasePointForm(newProjectId)
  })

  async function loadFunctionPointAssist() {
    if (!selectedProjectId.value) {
      functionPointError.value = '请先选择项目'
      return
    }
    functionPointError.value = ''
    functionPointMessage.value = ''
    try {
      const result = await fetchFunctionPointAssist(selectedProjectId.value)
      const nextForm = result?.data ? cloneForm(result.data) : createDefaultFunctionPointForm()
      functionPointForm.value = nextForm
      functionPointFormByProject.value = {
        ...functionPointFormByProject.value,
        [selectedProjectId.value]: cloneForm(nextForm)
      }
      functionPointMessage.value = '已按当前项目文件重新识别功能点明细，可直接计算或继续修正。'
    } catch (error) {
      functionPointError.value = error.message
    }
  }

  async function loadUseCasePointAssist() {
    if (!selectedProjectId.value) {
      useCasePointError.value = '请先选择项目'
      return
    }
    useCasePointError.value = ''
    useCasePointMessage.value = ''
    try {
      const result = await fetchUseCasePointAssist(selectedProjectId.value)
      const nextForm = result?.data ? cloneForm(result.data) : createDefaultUseCasePointForm()
      useCasePointForm.value = nextForm
      useCasePointFormByProject.value = {
        ...useCasePointFormByProject.value,
        [selectedProjectId.value]: cloneForm(nextForm)
      }
      useCasePointMessage.value = '已按当前项目文件重新识别用例点输入，可直接计算或继续修正。'
    } catch (error) {
      useCasePointError.value = error.message
    }
  }

  const metricModules = {
    loc: {
      result: locResult,
      loading: locLoading,
      error: locError,
      message: locMessage,
      reportMessage: locReportMessage,
      latest: fetchLatestLocResult,
      analyze: analyzeProjectLoc,
      report: exportLocReport,
      reportFileName: 'loc-report.md',
      successMessage: '代码行度量完成，结果已保存到本地任务文件。'
    },
    'control-flow': {
      result: complexityResult,
      loading: complexityLoading,
      error: complexityError,
      message: complexityMessage,
      reportMessage: complexityReportMessage,
      latest: fetchLatestComplexityResult,
      analyze: analyzeProjectComplexity,
      report: exportComplexityReport,
      reportFileName: 'complexity-report.md',
      successMessage: '圈复杂度分析完成，结果已保存到本地任务文件。',
      payload: () => analysisScopePayloadFor?.('control-flow')
    },
    'object-oriented': {
      result: ooResult,
      loading: ooLoading,
      error: ooError,
      message: ooMessage,
      reportMessage: ooReportMessage,
      latest: fetchLatestOoResult,
      analyze: analyzeProjectOo,
      report: exportOoReport,
      reportFileName: 'object-oriented-report.md',
      successMessage: '面向对象 CK/LK 度量完成，结果已保存到本地任务文件。',
      payload: () => analysisScopePayloadFor?.('object-oriented')
    },
    estimation: {
      result: estimationResult,
      loading: estimationLoading,
      error: estimationError,
      message: estimationMessage,
      reportMessage: estimationReportMessage,
      latest: fetchLatestEstimationResult,
      analyze: analyzeProjectEstimation,
      report: exportEstimationReport,
      reportFileName: 'estimation-report.md',
      successMessage: '工作量、工期、人员和成本估算完成，结果已保存到本地任务文件。',
      payload: () => ({
        mode: estimationForm.value.mode,
        kloc: estimationForm.value.kloc === '' ? null : Number(estimationForm.value.kloc),
        costPerPersonMonth: Number(estimationForm.value.costPerPersonMonth) || 20000
      })
    },
    ai: {
      result: aiResult,
      loading: aiLoading,
      error: aiError,
      message: aiMessage,
      latest: fetchLatestAiResult,
      analyze: analyzeProjectAi,
      successMessage: '智能质量分析完成，结果已保存为 JSON 和 Markdown。',
      localExport: () => {
        if (!aiResult.value) {
          aiError.value = '当前项目还没有智能分析结果'
          return
        }
        downloadMarkdown(aiResult.value.markdown, 'ai-analysis.md')
        aiMessage.value = '智能分析 Markdown 已下载。'
      }
    },
    'function-point': {
      result: functionPointResult,
      loading: functionPointLoading,
      error: functionPointError,
      message: functionPointMessage,
      reportMessage: functionPointReportMessage,
      latest: fetchLatestFunctionPointResult,
      analyze: analyzeProjectFunctionPoint,
      report: exportFunctionPointReport,
      reportFileName: 'function-point-report.md',
      successMessage: '功能点度量完成，结果基于当前项目文件识别并已保存。',
      prepareAnalyze: async () => {
        const result = await fetchFunctionPointAssist(selectedProjectId.value)
        const nextForm = result?.data ? cloneForm(result.data) : createDefaultFunctionPointForm()
        functionPointForm.value = nextForm
        functionPointFormByProject.value = {
          ...functionPointFormByProject.value,
          [selectedProjectId.value]: cloneForm(nextForm)
        }
      },
      beforeAnalyze: () => {
        functionPointForm.value.generalSystemCharacteristicTotal = functionPointGscTotal()
      },
      payload: () => functionPointForm.value
    },
    'use-case': {
      result: useCasePointResult,
      loading: useCasePointLoading,
      error: useCasePointError,
      message: useCasePointMessage,
      reportMessage: useCasePointReportMessage,
      latest: fetchLatestUseCasePointResult,
      analyze: analyzeProjectUseCasePoint,
      report: exportUseCasePointReport,
      reportFileName: 'use-case-point-report.md',
      successMessage: '用例点度量完成，结果基于当前项目文件识别并已保存。',
      prepareAnalyze: async () => {
        const result = await fetchUseCasePointAssist(selectedProjectId.value)
        const nextForm = result?.data ? cloneForm(result.data) : createDefaultUseCasePointForm()
        useCasePointForm.value = nextForm
        useCasePointFormByProject.value = {
          ...useCasePointFormByProject.value,
          [selectedProjectId.value]: cloneForm(nextForm)
        }
      },
      beforeAnalyze: () => {
        useCasePointForm.value.technicalFactorTotal = useCaseTechnicalTotal()
        useCasePointForm.value.environmentalFactorTotal = useCaseEnvironmentalTotal()
      },
      payload: () => useCasePointForm.value
    },
    'model-analysis': {
      result: modelResult,
      loading: modelLoading,
      error: modelError,
      message: modelMessage,
      reportMessage: modelReportMessage,
      latest: fetchLatestModelResult,
      analyze: analyzeProjectModel,
      report: exportModelReport,
      reportFileName: 'model-analysis-report.md',
      successMessage: '模型文件度量完成，结果已保存到本地任务文件。',
      payload: () => analysisScopePayloadFor?.('model-analysis')
    }
  }

  function metricModule(key = activeMenu.value) {
    return metricModules[key]
  }

  function resetMetricResults() {
    Object.values(metricModules).forEach(module => {
      module.result.value = null
      module.message.value = ''
      module.error.value = ''
      if (module.reportMessage) {
        module.reportMessage.value = ''
      }
    })
  }

  function markMetricResultsStale(message, keys = uploadDependentMetricKeys) {
    Object.entries(metricModules).forEach(([key, module]) => {
      if (!keys.includes(key)) {
        return
      }
      module.result.value = null
      module.message.value = message
      module.error.value = ''
      if (module.reportMessage) {
        module.reportMessage.value = ''
      }
    })
  }

  async function loadLatestMetricResult(key = activeMenu.value) {
    const module = metricModule(key)
    if (!module || !module.latest) {
      return
    }
    module.error.value = ''
    module.message.value = ''
    module.result.value = null
    if (!selectedProjectId.value) {
      return
    }
    try {
      const result = await module.latest(selectedProjectId.value)
      module.result.value = result.data
    } catch (error) {
      module.error.value = error.message
    }
  }

  async function loadLatestActiveMetricResult() {
    await loadLatestMetricResult(activeMenu.value)
  }

  async function runMetricAnalysis(key = activeMenu.value, options = {}) {
    const module = metricModule(key)
    if (!module || !module.analyze) {
      return
    }
    module.error.value = ''
    module.message.value = ''
    if (!selectedProjectId.value) {
      module.error.value = '请先选择项目'
      return
    }
    module.loading.value = true
    try {
      if (options.useFileRefresh !== false && module.prepareAnalyze) {
        await module.prepareAnalyze()
      }
      module.beforeAnalyze?.()
      const payload = module.payload ? module.payload() : undefined
      const result = await module.analyze(selectedProjectId.value, payload)
      module.result.value = result.data
      module.message.value = module.successMessage
      activeMenu.value = key
    } catch (error) {
      module.error.value = error.message
    } finally {
      module.loading.value = false
    }
  }

  async function exportMetricMarkdown(key = activeMenu.value) {
    const module = metricModule(key)
    if (!module) {
      return
    }
    module.error.value = ''
    if (module.reportMessage) {
      module.reportMessage.value = ''
    }
    if (module.localExport) {
      module.localExport()
      return
    }
    if (!selectedProjectId.value) {
      module.error.value = '请先选择项目'
      return
    }
    try {
      const result = await module.report(selectedProjectId.value)
      downloadMarkdown(result.data.content, module.reportFileName)
      module.reportMessage.value = `Markdown 报告已生成：${result.data.reportPath}`
    } catch (error) {
      module.error.value = error.message
    }
  }

  async function runLocAnalysis() {
    return runMetricAnalysis('loc')
  }

  async function exportLocMarkdown() {
    return exportMetricMarkdown('loc')
  }

  async function runComplexityAnalysis() {
    return runMetricAnalysis('control-flow')
  }

  async function exportComplexityMarkdown() {
    return exportMetricMarkdown('control-flow')
  }

  async function runOoAnalysis() {
    return runMetricAnalysis('object-oriented')
  }

  async function exportOoMarkdown() {
    return exportMetricMarkdown('object-oriented')
  }

  async function runEstimationAnalysis() {
    return runMetricAnalysis('estimation')
  }

  async function exportEstimationMarkdown() {
    return exportMetricMarkdown('estimation')
  }

  async function runAiAnalysis() {
    return runMetricAnalysis('ai')
  }

  function exportAiMarkdown() {
    return exportMetricMarkdown('ai')
  }

  async function runFunctionPointAnalysis() {
    return runMetricAnalysis('function-point', { useFileRefresh: true })
  }

  async function runFunctionPointAnalysisCurrent() {
    return runMetricAnalysis('function-point', { useFileRefresh: false })
  }

  async function exportFunctionPointMarkdown() {
    return exportMetricMarkdown('function-point')
  }

  async function runUseCasePointAnalysis() {
    return runMetricAnalysis('use-case', { useFileRefresh: true })
  }

  async function runUseCasePointAnalysisCurrent() {
    return runMetricAnalysis('use-case', { useFileRefresh: false })
  }

  async function exportUseCasePointMarkdown() {
    return exportMetricMarkdown('use-case')
  }

  async function runModelAnalysis() {
    return runMetricAnalysis('model-analysis')
  }

  async function exportModelMarkdown() {
    return exportMetricMarkdown('model-analysis')
  }

  return {
    locResult,
    locLoading,
    locError,
    locMessage,
    locReportMessage,
    complexityResult,
    complexityLoading,
    complexityError,
    complexityMessage,
    complexityReportMessage,
    ooResult,
    ooLoading,
    ooError,
    ooMessage,
    ooReportMessage,
    estimationResult,
    estimationLoading,
    estimationError,
    estimationMessage,
    estimationReportMessage,
    aiResult,
    aiLoading,
    aiError,
    aiMessage,
    functionPointResult,
    functionPointLoading,
    functionPointError,
    functionPointMessage,
    functionPointReportMessage,
    useCasePointResult,
    useCasePointLoading,
    useCasePointError,
    useCasePointMessage,
    useCasePointReportMessage,
    modelResult,
    modelLoading,
    modelError,
    modelMessage,
    modelReportMessage,
    estimationForm,
    functionPointForm,
    useCasePointForm,
    functionPointGscTotal,
    useCaseTechnicalTotal,
    useCaseEnvironmentalTotal,
    resetMetricResults,
    markMetricResultsStale,
    loadLatestMetricResult,
    loadLatestActiveMetricResult,
    runLocAnalysis,
    exportLocMarkdown,
    runComplexityAnalysis,
    exportComplexityMarkdown,
    runOoAnalysis,
    exportOoMarkdown,
    runEstimationAnalysis,
    exportEstimationMarkdown,
    runAiAnalysis,
    exportAiMarkdown,
    runFunctionPointAnalysis,
    runFunctionPointAnalysisCurrent,
    loadFunctionPointAssist,
    exportFunctionPointMarkdown,
    runUseCasePointAnalysis,
    runUseCasePointAnalysisCurrent,
    loadUseCasePointAssist,
    exportUseCasePointMarkdown,
    runModelAnalysis,
    exportModelMarkdown
  }
}
