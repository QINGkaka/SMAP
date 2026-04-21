<script setup>
import { onMounted, ref } from 'vue'
import {
  analyzeProjectAi,
  analyzeProjectComplexity,
  analyzeProjectEstimation,
  analyzeProjectFunctionPoint,
  analyzeProjectLoc,
  analyzeProjectModel,
  analyzeProjectOo,
  analyzeProjectUseCasePoint,
  createProject,
  deleteProject,
  deleteProjectFile,
  exportComplexityReport,
  exportEstimationReport,
  exportFunctionPointReport,
  exportLocReport,
  exportModelReport,
  exportOoReport,
  exportUseCasePointReport,
  exportComprehensiveReport,
  exportProjectXml,
  fetchHealth,
  fetchLatestComplexityResult,
  fetchLatestEstimationResult,
  fetchLatestAiResult,
  fetchLatestFunctionPointResult,
  fetchLatestLocResult,
  fetchLatestModelResult,
  fetchLatestOoResult,
  fetchLatestUseCasePointResult,
  fetchProjectTasks,
  fetchProjectFiles,
  fetchProjects,
  fetchThresholds,
  saveThresholds,
  updateProject,
  uploadProjectFile
} from './services/api'

const menus = [
  { key: 'project-management', label: '项目管理', icon: '▣', implemented: true },
  { key: 'function-point', label: '功能点度量', icon: '◆', implemented: true },
  { key: 'use-case', label: '用例图度量', icon: '⬟', implemented: true },
  { key: 'model-analysis', label: '模型文件度量', icon: '◫', implemented: true },
  { key: 'object-oriented', label: '面向对象度量', icon: '▰', implemented: true },
  { key: 'control-flow', label: '控制流图度量', icon: '⌁', implemented: true },
  { key: 'loc', label: '代码行度量', icon: '≡', implemented: true },
  { key: 'estimation', label: '估算分析', icon: '◇', implemented: true },
  { key: 'ai', label: '智能分析', icon: '✦', implemented: true }
]

const activeMenu = ref('project-management')
const backendStatus = ref('检查中')
const storageRoot = ref('')
const healthError = ref('')
const projects = ref([])
const projectLoading = ref(false)
const projectError = ref('')
const projectMessage = ref('')
const selectedProjectId = ref('')
const uploadedFiles = ref([])
const selectedFile = ref(null)
const projectFileMap = ref({})
const managementSelectedFiles = ref({})
const managementUploadLoading = ref({})
const fileListState = ref({})
const projectTaskMap = ref({})
const managementMessage = ref('')
const managementError = ref('')
const editingProjectId = ref('')
const uploadLoading = ref(false)
const uploadMessage = ref('')
const uploadError = ref('')
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
const xmlExportMessage = ref('')
const xmlExportError = ref('')
const comprehensiveReportMessage = ref('')
const comprehensiveReportError = ref('')
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
const clipboardMessage = ref('')
const projectForm = ref({
  name: '',
  language: 'Java',
  description: ''
})
const thresholdConfig = ref({})
const thresholdMessage = ref('')
const thresholdError = ref('')
const estimationForm = ref({
  mode: 'ORGANIC',
  kloc: '',
  costPerPersonMonth: 20000
})
const gscLabels = [
  '数据通信', '分布式数据处理', '性能', '重用性', '联机数据输入', '操作易用性', '联机更新',
  '复杂处理', '安装方便性', '运行方便性', '多站点部署', '变更便利性', '事务率', '最终用户效率'
]
const technicalFactorLabels = [
  '分布式系统', '响应/吞吐性能', '终端用户效率', '复杂内部处理', '代码复用性', '易安装性', '易用性',
  '可移植性', '易修改性', '并发性', '安全特性', '第三方访问', '用户培训'
]
const environmentalFactorLabels = [
  '过程熟悉度', '应用经验', '面向对象经验', '主分析师能力', '团队积极性', '需求稳定性', '兼职人员', '困难语言'
]
const functionPointForm = ref({
  externalInputs: { low: 1, average: 2, high: 0 },
  externalOutputs: { low: 1, average: 1, high: 0 },
  externalInquiries: { low: 1, average: 1, high: 0 },
  internalLogicalFiles: { low: 1, average: 0, high: 0 },
  externalInterfaceFiles: { low: 0, average: 1, high: 0 },
  generalSystemCharacteristicTotal: 35,
  generalSystemCharacteristics: [3, 2, 3, 2, 3, 3, 2, 3, 2, 3, 2, 3, 2, 2]
})
const useCasePointForm = ref({
  simpleActors: 1,
  averageActors: 1,
  complexActors: 0,
  simpleUseCases: 2,
  averageUseCases: 1,
  complexUseCases: 0,
  technicalFactorTotal: 30,
  environmentalFactorTotal: 20,
  productivityHoursPerUseCasePoint: 20,
  technicalFactors: [2, 3, 2, 3, 2, 2, 3, 2, 2, 2, 2, 2, 3],
  environmentalFactors: [3, 2, 2, 3, 3, 2, 2, 3]
})

const radarAxes = ['CBO', 'NOO', 'NOC', 'NOA', 'DIT', 'CS']
const radarColors = ['blue', 'green', 'gold', 'red']

async function loadHealth() {
  try {
    const result = await fetchHealth()
    backendStatus.value = result.data.status
    storageRoot.value = result.data.storageRoot
  } catch (error) {
    backendStatus.value = '未连接'
    healthError.value = error.message
  }
}

async function loadProjects() {
  projectLoading.value = true
  projectError.value = ''
  try {
    const result = await fetchProjects()
    projects.value = result.data
    await loadAllProjectFiles(result.data)
    await loadAllProjectTasks(result.data)
    if (!selectedProjectId.value && projects.value.length > 0) {
      selectedProjectId.value = projects.value[0].id
      await loadProjectFiles()
      await loadLatestLocResult()
      await loadLatestComplexityResult()
      await loadLatestOoResult()
      await loadLatestEstimationResult()
      await loadLatestAiResult()
      await loadLatestFunctionPointResult()
      await loadLatestUseCasePointResult()
      await loadLatestModelResult()
    }
  } catch (error) {
    projectError.value = error.message
  } finally {
    projectLoading.value = false
  }
}

async function submitProject() {
  projectError.value = ''
  projectMessage.value = ''
  try {
    const result = await createProject(projectForm.value)
    projectMessage.value = `已创建项目：${result.data.name}`
    projectForm.value = {
      name: '',
      language: 'Java',
      description: ''
    }
    await loadProjects()
    selectedProjectId.value = result.data.id
    await loadProjectFiles()
    await loadFilesForProject(result.data.id)
    locResult.value = null
    complexityResult.value = null
    ooResult.value = null
    estimationResult.value = null
    aiResult.value = null
    functionPointResult.value = null
    useCasePointResult.value = null
    modelResult.value = null
    activeMenu.value = 'project-management'
  } catch (error) {
    projectError.value = error.message
  }
}

async function selectProject(projectId) {
  selectedProjectId.value = projectId
  await handleProjectSelectionChange()
}

async function removeProject(project) {
  const confirmed = window.confirm(`确认删除项目“${project.name}”吗？该项目的上传文件和分析结果也会被删除。`)
  if (!confirmed) {
    return
  }
  projectError.value = ''
  projectMessage.value = ''
  try {
    await deleteProject(project.id)
    projectMessage.value = `已删除项目：${project.name}`
    if (selectedProjectId.value === project.id) {
      selectedProjectId.value = ''
      uploadedFiles.value = []
      locResult.value = null
      complexityResult.value = null
      ooResult.value = null
      estimationResult.value = null
      aiResult.value = null
      functionPointResult.value = null
      useCasePointResult.value = null
      modelResult.value = null
    }
    await loadProjects()
    await loadAllProjectFiles()
  } catch (error) {
    projectError.value = error.message
  }
}

async function loadAllProjectFiles(projectList = projects.value) {
  if (!projectList || projectList.length === 0) {
    projectFileMap.value = {}
    return
  }
  const entries = await Promise.all(projectList.map(async project => {
    try {
      const result = await fetchProjectFiles(project.id)
      return [project.id, result.data]
    } catch {
      return [project.id, []]
    }
  }))
  projectFileMap.value = Object.fromEntries(entries)
}


async function loadAllProjectTasks(projectList = projects.value) {
  if (!projectList || projectList.length === 0) {
    projectTaskMap.value = {}
    return
  }
  const entries = await Promise.all(projectList.map(async project => {
    try {
      const result = await fetchProjectTasks(project.id)
      return [project.id, result.data]
    } catch {
      return [project.id, []]
    }
  }))
  projectTaskMap.value = Object.fromEntries(entries)
}

function projectTasks(projectId) {
  return projectTaskMap.value[projectId] || []
}

async function loadThresholdConfig() {
  thresholdError.value = ''
  try {
    const result = await fetchThresholds()
    thresholdConfig.value = result.data
  } catch (error) {
    thresholdError.value = error.message
  }
}

async function submitThresholdConfig() {
  thresholdError.value = ''
  thresholdMessage.value = ''
  try {
    const result = await saveThresholds(thresholdConfig.value)
    thresholdConfig.value = result.data
    thresholdMessage.value = '阈值配置已保存到本地 thresholds.json。'
  } catch (error) {
    thresholdError.value = error.message
  }
}

async function importSampleProject() {
  projectError.value = ''
  projectMessage.value = ''
  try {
    const result = await createProject({
      name: `JavaMetricLab 示例项目 ${projects.value.length + 1}`,
      language: 'Java',
      description: '用于课堂演示的软件度量样例项目，可继续上传 Java 或 UML/XMI 文件。'
    })
    projectMessage.value = `已导入示例项目：${result.data.name}`
    selectedProjectId.value = result.data.id
    await loadProjects()
  } catch (error) {
    projectError.value = error.message
  }
}

async function loadFilesForProject(projectId) {
  if (!projectId) {
    return
  }
  try {
    const result = await fetchProjectFiles(projectId)
    projectFileMap.value = {
      ...projectFileMap.value,
      [projectId]: result.data
    }
    if (projectId === selectedProjectId.value) {
      uploadedFiles.value = result.data
    }
  } catch (error) {
    managementError.value = error.message
  }
}

async function loadProjectFiles() {
  uploadedFiles.value = []
  uploadError.value = ''
  uploadMessage.value = ''
  if (!selectedProjectId.value) {
    return
  }
  try {
    const result = await fetchProjectFiles(selectedProjectId.value)
    uploadedFiles.value = result.data
    projectFileMap.value = {
      ...projectFileMap.value,
      [selectedProjectId.value]: result.data
    }
  } catch (error) {
    uploadError.value = error.message
  }
}

async function handleProjectSelectionChange() {
  await loadProjectFiles()
  await loadLatestLocResult()
  await loadLatestComplexityResult()
  await loadLatestOoResult()
  await loadLatestEstimationResult()
  await loadLatestAiResult()
  await loadLatestFunctionPointResult()
  await loadLatestUseCasePointResult()
  await loadLatestModelResult()
}

function handleFileChange(event) {
  selectedFile.value = event.target.files?.[0] || null
  uploadMessage.value = ''
  uploadError.value = ''
}

function handleManagementFileChange(projectId, event) {
  const file = event.target.files?.[0] || null
  managementSelectedFiles.value = {
    ...managementSelectedFiles.value,
    [projectId]: file
  }
  managementMessage.value = ''
  managementError.value = ''
}

async function submitUpload() {
  uploadError.value = ''
  uploadMessage.value = ''
  if (!selectedProjectId.value) {
    uploadError.value = '请先选择项目'
    return
  }
  if (!selectedFile.value) {
    uploadError.value = '请选择要上传的文件'
    return
  }
  uploadLoading.value = true
  try {
    const result = await uploadProjectFile(selectedProjectId.value, selectedFile.value)
    uploadMessage.value = `已上传：${result.data.originalName}`
    selectedFile.value = null
    await loadProjectFiles()
    await loadFilesForProject(selectedProjectId.value)
    locMessage.value = '文件已更新，可重新执行代码行度量。'
    complexityMessage.value = '文件已更新，可重新执行圈复杂度分析。'
    ooMessage.value = '文件已更新，可重新执行面向对象度量。'
    estimationMessage.value = '文件已更新，可重新执行估算分析。'
    aiMessage.value = '文件已更新，可重新执行智能分析。'
    functionPointMessage.value = '文件已更新，可重新执行功能点度量。'
    useCasePointMessage.value = '文件已更新，可重新执行用例点估算。'
    modelMessage.value = '文件已更新，可重新执行模型文件度量。'
  } catch (error) {
    uploadError.value = error.message
  } finally {
    uploadLoading.value = false
  }
}

async function submitManagementUpload(project) {
  managementError.value = ''
  managementMessage.value = ''
  const file = managementSelectedFiles.value[project.id]
  if (!file) {
    managementError.value = `请先为项目“${project.name}”选择文件`
    return
  }
  managementUploadLoading.value = {
    ...managementUploadLoading.value,
    [project.id]: true
  }
  try {
    const result = await uploadProjectFile(project.id, file)
    managementMessage.value = `已上传到“${project.name}”：${result.data.originalName}`
    managementSelectedFiles.value = {
      ...managementSelectedFiles.value,
      [project.id]: null
    }
    await loadFilesForProject(project.id)
    locMessage.value = '文件已更新，可重新执行代码行度量。'
    complexityMessage.value = '文件已更新，可重新执行圈复杂度分析。'
    ooMessage.value = '文件已更新，可重新执行面向对象度量。'
    estimationMessage.value = '文件已更新，可重新执行估算分析。'
    aiMessage.value = '文件已更新，可重新执行智能分析。'
    functionPointMessage.value = '文件已更新，可重新执行功能点度量。'
    useCasePointMessage.value = '文件已更新，可重新执行用例点估算。'
    modelMessage.value = '文件已更新，可重新执行模型文件度量。'
  } catch (error) {
    managementError.value = error.message
  } finally {
    managementUploadLoading.value = {
      ...managementUploadLoading.value,
      [project.id]: false
    }
  }
}

async function removeUploadedFile(file) {
  const confirmed = window.confirm(`确认删除上传文件“${file.originalName}”吗？`)
  if (!confirmed) {
    return
  }
  uploadError.value = ''
  uploadMessage.value = ''
  try {
    await deleteProjectFile(selectedProjectId.value, file.id)
    uploadMessage.value = `已删除：${file.originalName}`
    await loadProjectFiles()
    await loadFilesForProject(selectedProjectId.value)
    locMessage.value = '上传文件已变化，可重新执行代码行度量。'
    complexityMessage.value = '上传文件已变化，可重新执行圈复杂度分析。'
    ooMessage.value = '上传文件已变化，可重新执行面向对象度量。'
    estimationMessage.value = '上传文件已变化，可重新执行估算分析。'
    aiMessage.value = '上传文件已变化，可重新执行智能分析。'
    functionPointMessage.value = '上传文件已变化，可重新执行功能点度量。'
    useCasePointMessage.value = '上传文件已变化，可重新执行用例点估算。'
    modelMessage.value = '上传文件已变化，可重新执行模型文件度量。'
  } catch (error) {
    uploadError.value = error.message
  }
}

async function removeManagementFile(project, file) {
  const confirmed = window.confirm(`确认删除项目“${project.name}”中的文件“${file.originalName}”吗？`)
  if (!confirmed) {
    return
  }
  managementError.value = ''
  managementMessage.value = ''
  try {
    await deleteProjectFile(project.id, file.id)
    managementMessage.value = `已从“${project.name}”删除：${file.originalName}`
    await loadFilesForProject(project.id)
    locMessage.value = '上传文件已变化，可重新执行代码行度量。'
    complexityMessage.value = '上传文件已变化，可重新执行圈复杂度分析。'
    ooMessage.value = '上传文件已变化，可重新执行面向对象度量。'
    estimationMessage.value = '上传文件已变化，可重新执行估算分析。'
    aiMessage.value = '上传文件已变化，可重新执行智能分析。'
    functionPointMessage.value = '上传文件已变化，可重新执行功能点度量。'
    useCasePointMessage.value = '上传文件已变化，可重新执行用例点估算。'
    modelMessage.value = '上传文件已变化，可重新执行模型文件度量。'
  } catch (error) {
    managementError.value = error.message
  }
}

async function loadLatestLocResult() {
  locError.value = ''
  locMessage.value = ''
  locResult.value = null
  if (!selectedProjectId.value) {
    return
  }
  try {
    const result = await fetchLatestLocResult(selectedProjectId.value)
    locResult.value = result.data
  } catch (error) {
    locError.value = error.message
  }
}

async function loadLatestComplexityResult() {
  complexityError.value = ''
  complexityMessage.value = ''
  complexityResult.value = null
  if (!selectedProjectId.value) {
    return
  }
  try {
    const result = await fetchLatestComplexityResult(selectedProjectId.value)
    complexityResult.value = result.data
  } catch (error) {
    complexityError.value = error.message
  }
}

async function loadLatestOoResult() {
  ooError.value = ''
  ooMessage.value = ''
  ooResult.value = null
  if (!selectedProjectId.value) {
    return
  }
  try {
    const result = await fetchLatestOoResult(selectedProjectId.value)
    ooResult.value = result.data
  } catch (error) {
    ooError.value = error.message
  }
}

async function loadLatestEstimationResult() {
  estimationError.value = ''
  estimationMessage.value = ''
  estimationResult.value = null
  if (!selectedProjectId.value) {
    return
  }
  try {
    const result = await fetchLatestEstimationResult(selectedProjectId.value)
    estimationResult.value = result.data
  } catch (error) {
    estimationError.value = error.message
  }
}

async function loadLatestAiResult() {
  aiError.value = ''
  aiMessage.value = ''
  aiResult.value = null
  if (!selectedProjectId.value) {
    return
  }
  try {
    const result = await fetchLatestAiResult(selectedProjectId.value)
    aiResult.value = result.data
  } catch (error) {
    aiError.value = error.message
  }
}

async function loadLatestFunctionPointResult() {
  functionPointError.value = ''
  functionPointMessage.value = ''
  functionPointResult.value = null
  if (!selectedProjectId.value) {
    return
  }
  try {
    const result = await fetchLatestFunctionPointResult(selectedProjectId.value)
    functionPointResult.value = result.data
  } catch (error) {
    functionPointError.value = error.message
  }
}

async function loadLatestUseCasePointResult() {
  useCasePointError.value = ''
  useCasePointMessage.value = ''
  useCasePointResult.value = null
  if (!selectedProjectId.value) {
    return
  }
  try {
    const result = await fetchLatestUseCasePointResult(selectedProjectId.value)
    useCasePointResult.value = result.data
  } catch (error) {
    useCasePointError.value = error.message
  }
}

async function loadLatestModelResult() {
  modelError.value = ''
  modelMessage.value = ''
  modelResult.value = null
  if (!selectedProjectId.value) {
    return
  }
  try {
    const result = await fetchLatestModelResult(selectedProjectId.value)
    modelResult.value = result.data
  } catch (error) {
    modelError.value = error.message
  }
}

async function runLocAnalysis() {
  locError.value = ''
  locMessage.value = ''
  if (!selectedProjectId.value) {
    locError.value = '请先选择项目'
    return
  }
  locLoading.value = true
  try {
    const result = await analyzeProjectLoc(selectedProjectId.value)
    locResult.value = result.data
    locMessage.value = '代码行度量完成，结果已保存到本地任务文件。'
    activeMenu.value = 'loc'
  } catch (error) {
    locError.value = error.message
  } finally {
    locLoading.value = false
  }
}

async function exportLocMarkdown() {
  locError.value = ''
  locReportMessage.value = ''
  if (!selectedProjectId.value) {
    locError.value = '请先选择项目'
    return
  }
  try {
    const result = await exportLocReport(selectedProjectId.value)
    downloadMarkdown(result.data.content, 'loc-report.md')
    locReportMessage.value = `Markdown 报告已生成：${result.data.reportPath}`
  } catch (error) {
    locError.value = error.message
  }
}

async function runComplexityAnalysis() {
  complexityError.value = ''
  complexityMessage.value = ''
  if (!selectedProjectId.value) {
    complexityError.value = '请先选择项目'
    return
  }
  complexityLoading.value = true
  try {
    const result = await analyzeProjectComplexity(selectedProjectId.value)
    complexityResult.value = result.data
    complexityMessage.value = '圈复杂度分析完成，结果已保存到本地任务文件。'
    activeMenu.value = 'control-flow'
  } catch (error) {
    complexityError.value = error.message
  } finally {
    complexityLoading.value = false
  }
}

async function exportComplexityMarkdown() {
  complexityError.value = ''
  complexityReportMessage.value = ''
  if (!selectedProjectId.value) {
    complexityError.value = '请先选择项目'
    return
  }
  try {
    const result = await exportComplexityReport(selectedProjectId.value)
    downloadMarkdown(result.data.content, 'complexity-report.md')
    complexityReportMessage.value = `Markdown 报告已生成：${result.data.reportPath}`
  } catch (error) {
    complexityError.value = error.message
  }
}

async function runOoAnalysis() {
  ooError.value = ''
  ooMessage.value = ''
  if (!selectedProjectId.value) {
    ooError.value = '请先选择项目'
    return
  }
  ooLoading.value = true
  try {
    const result = await analyzeProjectOo(selectedProjectId.value)
    ooResult.value = result.data
    ooMessage.value = '面向对象 CK/LK 度量完成，结果已保存到本地任务文件。'
    activeMenu.value = 'object-oriented'
  } catch (error) {
    ooError.value = error.message
  } finally {
    ooLoading.value = false
  }
}

async function exportOoMarkdown() {
  ooError.value = ''
  ooReportMessage.value = ''
  if (!selectedProjectId.value) {
    ooError.value = '请先选择项目'
    return
  }
  try {
    const result = await exportOoReport(selectedProjectId.value)
    downloadMarkdown(result.data.content, 'object-oriented-report.md')
    ooReportMessage.value = `Markdown 报告已生成：${result.data.reportPath}`
  } catch (error) {
    ooError.value = error.message
  }
}

async function runEstimationAnalysis() {
  estimationError.value = ''
  estimationMessage.value = ''
  if (!selectedProjectId.value) {
    estimationError.value = '请先选择项目'
    return
  }
  estimationLoading.value = true
  try {
    const payload = {
      mode: estimationForm.value.mode,
      kloc: estimationForm.value.kloc === '' ? null : Number(estimationForm.value.kloc),
      costPerPersonMonth: Number(estimationForm.value.costPerPersonMonth) || 20000
    }
    const result = await analyzeProjectEstimation(selectedProjectId.value, payload)
    estimationResult.value = result.data
    estimationMessage.value = '工作量、工期、人员和成本估算完成，结果已保存到本地任务文件。'
    activeMenu.value = 'estimation'
  } catch (error) {
    estimationError.value = error.message
  } finally {
    estimationLoading.value = false
  }
}

async function exportEstimationMarkdown() {
  estimationError.value = ''
  estimationReportMessage.value = ''
  if (!selectedProjectId.value) {
    estimationError.value = '请先选择项目'
    return
  }
  try {
    const result = await exportEstimationReport(selectedProjectId.value)
    downloadMarkdown(result.data.content, 'estimation-report.md')
    estimationReportMessage.value = `Markdown 报告已生成：${result.data.reportPath}`
  } catch (error) {
    estimationError.value = error.message
  }
}

async function runAiAnalysis() {
  aiError.value = ''
  aiMessage.value = ''
  if (!selectedProjectId.value) {
    aiError.value = '请先选择项目'
    return
  }
  aiLoading.value = true
  try {
    const result = await analyzeProjectAi(selectedProjectId.value)
    aiResult.value = result.data
    aiMessage.value = '智能质量分析完成，结果已保存为 JSON 和 Markdown。'
    activeMenu.value = 'ai'
  } catch (error) {
    aiError.value = error.message
  } finally {
    aiLoading.value = false
  }
}

function exportAiMarkdown() {
  aiError.value = ''
  if (!aiResult.value) {
    aiError.value = '当前项目还没有智能分析结果'
    return
  }
  downloadMarkdown(aiResult.value.markdown, 'ai-analysis.md')
  aiMessage.value = '智能分析 Markdown 已下载。'
}

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

function functionPointGscTotal() {
  return scoreTotal(functionPointForm.value.generalSystemCharacteristics)
}

function useCaseTechnicalTotal() {
  return scoreTotal(useCasePointForm.value.technicalFactors)
}

function useCaseEnvironmentalTotal() {
  return scoreTotal(useCasePointForm.value.environmentalFactors)
}

async function runFunctionPointAnalysis() {
  functionPointError.value = ''
  functionPointMessage.value = ''
  if (!selectedProjectId.value) {
    functionPointError.value = '请先选择项目'
    return
  }
  functionPointLoading.value = true
  try {
    functionPointForm.value.generalSystemCharacteristicTotal = functionPointGscTotal()
    const result = await analyzeProjectFunctionPoint(selectedProjectId.value, functionPointForm.value)
    functionPointResult.value = result.data
    functionPointMessage.value = '功能点度量完成，结果已保存到本地任务文件。'
    activeMenu.value = 'function-point'
  } catch (error) {
    functionPointError.value = error.message
  } finally {
    functionPointLoading.value = false
  }
}

async function exportFunctionPointMarkdown() {
  functionPointError.value = ''
  functionPointReportMessage.value = ''
  if (!selectedProjectId.value) {
    functionPointError.value = '请先选择项目'
    return
  }
  try {
    const result = await exportFunctionPointReport(selectedProjectId.value)
    downloadMarkdown(result.data.content, 'function-point-report.md')
    functionPointReportMessage.value = `Markdown 报告已生成：${result.data.reportPath}`
  } catch (error) {
    functionPointError.value = error.message
  }
}

async function runUseCasePointAnalysis() {
  useCasePointError.value = ''
  useCasePointMessage.value = ''
  if (!selectedProjectId.value) {
    useCasePointError.value = '请先选择项目'
    return
  }
  useCasePointLoading.value = true
  try {
    useCasePointForm.value.technicalFactorTotal = useCaseTechnicalTotal()
    useCasePointForm.value.environmentalFactorTotal = useCaseEnvironmentalTotal()
    const result = await analyzeProjectUseCasePoint(selectedProjectId.value, useCasePointForm.value)
    useCasePointResult.value = result.data
    useCasePointMessage.value = '用例点估算完成，结果已保存到本地任务文件。'
    activeMenu.value = 'use-case'
  } catch (error) {
    useCasePointError.value = error.message
  } finally {
    useCasePointLoading.value = false
  }
}

async function exportUseCasePointMarkdown() {
  useCasePointError.value = ''
  useCasePointReportMessage.value = ''
  if (!selectedProjectId.value) {
    useCasePointError.value = '请先选择项目'
    return
  }
  try {
    const result = await exportUseCasePointReport(selectedProjectId.value)
    downloadMarkdown(result.data.content, 'use-case-point-report.md')
    useCasePointReportMessage.value = `Markdown 报告已生成：${result.data.reportPath}`
  } catch (error) {
    useCasePointError.value = error.message
  }
}

async function runModelAnalysis() {
  modelError.value = ''
  modelMessage.value = ''
  if (!selectedProjectId.value) {
    modelError.value = '请先选择项目'
    return
  }
  modelLoading.value = true
  try {
    const result = await analyzeProjectModel(selectedProjectId.value)
    modelResult.value = result.data
    modelMessage.value = '模型文件度量完成，结果已保存到本地任务文件。'
    activeMenu.value = 'model-analysis'
  } catch (error) {
    modelError.value = error.message
  } finally {
    modelLoading.value = false
  }
}

async function exportModelMarkdown() {
  modelError.value = ''
  modelReportMessage.value = ''
  if (!selectedProjectId.value) {
    modelError.value = '请先选择项目'
    return
  }
  try {
    const result = await exportModelReport(selectedProjectId.value)
    downloadMarkdown(result.data.content, 'model-analysis-report.md')
    modelReportMessage.value = `Markdown 报告已生成：${result.data.reportPath}`
  } catch (error) {
    modelError.value = error.message
  }
}

async function exportXml() {
  xmlExportError.value = ''
  xmlExportMessage.value = ''
  if (!selectedProjectId.value) {
    xmlExportError.value = '请先选择项目'
    return
  }
  try {
    const result = await exportProjectXml(selectedProjectId.value)
    downloadText(result.data.content, 'metrics.xml', 'application/xml;charset=utf-8')
    xmlExportMessage.value = `XML 已生成：${result.data.reportPath}`
  } catch (error) {
    xmlExportError.value = error.message
  }
}

async function exportComprehensiveMarkdown() {
  comprehensiveReportError.value = ''
  comprehensiveReportMessage.value = ''
  if (!selectedProjectId.value) {
    comprehensiveReportError.value = '请先选择项目'
    return
  }
  try {
    const result = await exportComprehensiveReport(selectedProjectId.value)
    downloadMarkdown(result.data.content, 'final-metric-report.md')
    comprehensiveReportMessage.value = `综合报告已生成：${result.data.reportPath}`
  } catch (error) {
    comprehensiveReportError.value = error.message
  }
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function formatFileSize(size) {
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

async function copyProjectId(projectId) {
  clipboardMessage.value = ''
  try {
    await navigator.clipboard.writeText(projectId)
    clipboardMessage.value = `已复制项目 ID：${projectId}`
  } catch {
    clipboardMessage.value = `项目 ID：${projectId}`
  }
}

function downloadMarkdown(content, fileName) {
  downloadText(content, fileName, 'text/markdown;charset=utf-8')
}

function downloadText(content, fileName, type) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

function selectedProjectName() {
  return projects.value.find(project => project.id === selectedProjectId.value)?.name || '未选择项目'
}

function projectFiles(projectId) {
  return projectFileMap.value[projectId] || []
}

function fileState(projectId) {
  if (!fileListState.value[projectId]) {
    fileListState.value = {
      ...fileListState.value,
      [projectId]: {
        keyword: '',
        type: 'all',
        sortBy: 'uploadedAt',
        sortOrder: 'desc',
        page: 1,
        pageSize: 5
      }
    }
  }
  return fileListState.value[projectId]
}

function updateFileState(projectId, patch) {
  fileListState.value = {
    ...fileListState.value,
    [projectId]: {
      ...fileState(projectId),
      ...patch
    }
  }
}

function fileCategories(projectId) {
  const counts = projectFiles(projectId).reduce((result, file) => {
    result[file.fileType] = (result[file.fileType] || 0) + 1
    return result
  }, {})
  return [
    { key: 'all', label: `全部(${projectFiles(projectId).length})` },
    ...Object.entries(counts)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([type, count]) => ({ key: type, label: `${type.toUpperCase()}(${count})` }))
  ]
}

function filteredProjectFiles(projectId) {
  const state = fileState(projectId)
  const keyword = state.keyword.trim().toLowerCase()
  return projectFiles(projectId)
    .filter(file => state.type === 'all' || file.fileType === state.type)
    .filter(file => {
      if (!keyword) {
        return true
      }
      return file.originalName.toLowerCase().includes(keyword)
        || file.fileType.toLowerCase().includes(keyword)
        || file.id.toLowerCase().includes(keyword)
    })
    .sort((left, right) => compareFiles(left, right, state.sortBy, state.sortOrder))
}

function pagedProjectFiles(projectId) {
  const state = fileState(projectId)
  const filtered = filteredProjectFiles(projectId)
  const safePage = currentFilePage(projectId)
  const start = (safePage - 1) * state.pageSize
  return filtered.slice(start, start + state.pageSize)
}

function pageCount(projectId) {
  const state = fileState(projectId)
  return Math.max(1, Math.ceil(filteredProjectFiles(projectId).length / state.pageSize))
}

function currentFilePage(projectId) {
  return Math.min(Math.max(fileState(projectId).page, 1), pageCount(projectId))
}

function compareFiles(left, right, sortBy, sortOrder) {
  const direction = sortOrder === 'asc' ? 1 : -1
  if (sortBy === 'name') {
    return left.originalName.localeCompare(right.originalName) * direction
  }
  if (sortBy === 'type') {
    return left.fileType.localeCompare(right.fileType) * direction
  }
  if (sortBy === 'size') {
    return (left.size - right.size) * direction
  }
  return (new Date(left.uploadedAt).getTime() - new Date(right.uploadedAt).getTime()) * direction
}

function changeFilePage(projectId, nextPage) {
  const bounded = Math.min(Math.max(nextPage, 1), pageCount(projectId))
  updateFileState(projectId, { page: bounded })
}

function isLegacyComplexityResult() {
  return complexityResult.value && !Array.isArray(complexityResult.value.files)
}

function handleMenuClick(item) {
  if (!item.implemented) {
    return
  }
  activeMenu.value = item.key
}

function percent(value) {
  return `${((value || 0) * 100).toFixed(1)}%`
}

function metricCardValue(type) {
  if (activeMenu.value === 'project-management') {
    return type === 'main'
      ? Object.values(projectFileMap.value).reduce((total, files) => total + files.length, 0)
      : projects.value.length
  }
  if (activeMenu.value === 'loc') {
    return type === 'main' && locResult.value ? locResult.value.summary.sourceLines : locResult.value?.summary.totalLines || 0
  }
  if (activeMenu.value === 'control-flow') {
    return type === 'main' && complexityResult.value
      ? complexityResult.value.summary.methodCount
      : complexityResult.value?.summary.maxComplexity || 0
  }
  if (activeMenu.value === 'object-oriented') {
    return type === 'main' && ooResult.value
      ? ooResult.value.summary.classCount + ooResult.value.summary.interfaceCount
      : ooResult.value?.summary.highRiskClassCount || 0
  }
  if (activeMenu.value === 'estimation') {
    return type === 'main' && estimationResult.value
      ? estimationResult.value.effortPersonMonths
      : estimationResult.value?.estimatedCost || 0
  }
  if (activeMenu.value === 'function-point') {
    return type === 'main' && functionPointResult.value
      ? functionPointResult.value.adjustedFunctionPoints
      : functionPointResult.value?.unadjustedFunctionPoints || 0
  }
  if (activeMenu.value === 'use-case') {
    return type === 'main' && useCasePointResult.value
      ? useCasePointResult.value.useCasePoints
      : useCasePointResult.value?.estimatedPersonMonths || 0
  }
  if (activeMenu.value === 'model-analysis') {
    return type === 'main' && modelResult.value
      ? modelResult.value.summary.classCount + modelResult.value.summary.interfaceCount
      : modelResult.value?.summary.highRiskClassCount || 0
  }
  if (activeMenu.value === 'ai') {
    return type === 'main' && aiResult.value
      ? aiResult.value.riskItems.length
      : aiResult.value?.refactoringSuggestions.length || 0
  }
  return 0
}

function metricLabel(type) {
  if (activeMenu.value === 'loc') {
    return type === 'main' ? '有效代码行' : '总行数'
  }
  if (activeMenu.value === 'control-flow') {
    return type === 'main' ? '方法数量' : '最高复杂度'
  }
  if (activeMenu.value === 'object-oriented') {
    return type === 'main' ? '类/接口数量' : '风险类'
  }
  if (activeMenu.value === 'estimation') {
    return type === 'main' ? '工作量' : '估算成本'
  }
  if (activeMenu.value === 'function-point') {
    return type === 'main' ? '调整后功能点' : '未调整功能点'
  }
  if (activeMenu.value === 'use-case') {
    return type === 'main' ? '用例点' : '估算人月'
  }
  if (activeMenu.value === 'model-analysis') {
    return type === 'main' ? '模型类/接口' : '高风险类'
  }
  if (activeMenu.value === 'ai') {
    return type === 'main' ? '风险项' : '改进建议'
  }
  return type === 'main' ? '文件数量' : '项目数量'
}

function metricNote(type) {
  if (activeMenu.value === 'loc') {
    return type === 'main' ? '排除空行和纯注释行' : '来自已上传 Java 文件'
  }
  if (activeMenu.value === 'control-flow') {
    return type === 'main' ? '从 Java 方法定义中识别' : '按 McCabe 圈复杂度阈值标记'
  }
  if (activeMenu.value === 'object-oriented') {
    return type === 'main' ? '来自 Java 类、接口、枚举、记录' : '按 CBO/DIT/WMC/LCOM 阈值标记'
  }
  if (activeMenu.value === 'estimation') {
    return type === 'main' ? '单位：人月' : '由人月成本换算'
  }
  if (activeMenu.value === 'function-point') {
    return type === 'main' ? 'AFP = UFP × VAF' : 'EI/EO/EQ/ILF/EIF 加权'
  }
  if (activeMenu.value === 'use-case') {
    return type === 'main' ? 'UCP = UUCP × TCF × ECF' : '按 160 小时/人月换算'
  }
  if (activeMenu.value === 'model-analysis') {
    return type === 'main' ? '来自 XML/XMI/OOM 模型文件' : '按规模和继承深度识别'
  }
  if (activeMenu.value === 'ai') {
    return type === 'main' ? '由本地规则汇总' : '面向重构和测试'
  }
  return type === 'main' ? '当前项目上传文件总数' : '本地 projects.json'
}

function riskLabel(level) {
  const labels = {
    LOW: '低风险',
    MEDIUM: '中风险',
    HIGH: '高风险',
    EXTREME: '极高风险'
  }
  return labels[level] || level || '-'
}

function radarSeries() {
  if (!ooResult.value?.classes?.length) {
    return []
  }
  return ooResult.value.classes
    .slice()
    .sort((left, right) => (right.cbo + right.wmc + right.lcom) - (left.cbo + left.wmc + left.lcom))
    .slice(0, 4)
    .map((item, index) => ({
      name: item.className,
      color: radarColors[index % radarColors.length],
      points: radarPoints([
        normalizeRadar(item.cbo, 20),
        normalizeRadar(item.noo, 30),
        normalizeRadar(item.noc, 10),
        normalizeRadar(item.noa, 30),
        normalizeRadar(item.dit, 6),
        normalizeRadar(item.cs, 220)
      ])
    }))
}

function normalizeRadar(value, max) {
  return Math.min(Math.max(Number(value) || 0, 0), max) / max
}

function radarPoints(values) {
  const center = 120
  const radius = 86
  return values.map((value, index) => {
    const angle = -Math.PI / 2 + index * (Math.PI * 2 / values.length)
    const x = center + Math.cos(angle) * radius * value
    const y = center + Math.sin(angle) * radius * value
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

function activeTitle() {
  if (activeMenu.value === 'project-management') {
    return '项目管理'
  }
  if (activeMenu.value === 'loc') {
    return '代码行度量'
  }
  if (activeMenu.value === 'control-flow') {
    return '控制流图度量'
  }
  if (activeMenu.value === 'estimation') {
    return '估算分析'
  }
  if (activeMenu.value === 'function-point') {
    return '功能点度量'
  }
  if (activeMenu.value === 'use-case') {
    return '用例图度量'
  }
  if (activeMenu.value === 'model-analysis') {
    return '模型文件度量'
  }
  if (activeMenu.value === 'ai') {
    return '智能分析'
  }
  return '面向对象度量'
}

function activeEyebrow() {
  if (activeMenu.value === 'project-management') {
    return 'Project Workspace'
  }
  if (activeMenu.value === 'loc') {
    return 'Lines of Code Metrics'
  }
  if (activeMenu.value === 'control-flow') {
    return 'Cyclomatic Complexity Metrics'
  }
  if (activeMenu.value === 'estimation') {
    return 'Effort and Cost Estimation'
  }
  if (activeMenu.value === 'function-point') {
    return 'Function Point Metrics'
  }
  if (activeMenu.value === 'use-case') {
    return 'Use Case Point Estimation'
  }
  if (activeMenu.value === 'model-analysis') {
    return 'UML / XMI Model Metrics'
  }
  if (activeMenu.value === 'ai') {
    return 'Quality Recommendation'
  }
  return 'Object-Oriented Metrics'
}

onMounted(async () => {
  await Promise.all([loadHealth(), loadProjects(), loadThresholdConfig()])
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark"></span>
        <span>自动化软件度量平台</span>
      </div>
      <div class="status-pill" :class="{ offline: backendStatus !== 'UP' }">
        后端：{{ backendStatus }}
      </div>
    </header>

    <main class="workspace">
      <aside class="sidebar">
        <button class="collapse-button" type="button">☰</button>
        <nav class="metric-menu">
          <button
            v-for="item in menus"
            :key="item.key"
            class="menu-item"
            :class="{ active: activeMenu === item.key, disabled: !item.implemented }"
            type="button"
            :disabled="!item.implemented"
            :title="item.implemented ? item.label : '该功能暂不可用'"
            @click="handleMenuClick(item)"
          >
            <span class="menu-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
            <span v-if="!item.implemented" class="menu-badge">未完成</span>
          </button>
        </nav>
      </aside>

      <section class="content-panel">
        <div class="section-header">
          <div>
            <p class="eyebrow">{{ activeEyebrow() }}</p>
            <h1>{{ activeTitle() }}</h1>
          </div>
          <div class="header-actions">
            <button type="button" class="ghost-button" @click="importSampleProject">
              导入示例
            </button>
            <button
              type="button"
              class="primary-button"
              @click="activeMenu = 'project-management'; projectForm.name = 'JavaMetricLab 示例项目'"
            >
              新建项目
            </button>
          </div>
        </div>

        <template v-if="activeMenu === 'project-management'">
        <div class="project-section">
          <form class="project-form" @submit.prevent="submitProject">
            <label>
              <span>项目名称</span>
              <input v-model="projectForm.name" type="text" placeholder="例如：JavaMetricLab" />
            </label>
            <label>
              <span>语言</span>
              <select v-model="projectForm.language">
                <option>Java</option>
                <option>Vue</option>
                <option>Mixed</option>
              </select>
            </label>
            <label class="description-field">
              <span>项目描述</span>
              <input v-model="projectForm.description" type="text" placeholder="用于记录项目背景，可选" />
            </label>
            <button type="submit" class="primary-button">{{ editingProjectId ? '更新项目' : '保存项目' }}</button>
            <button v-if="editingProjectId" type="button" class="ghost-button" @click="cancelEditProject">取消编辑</button>
          </form>
          <p v-if="projectMessage" class="form-message success">{{ projectMessage }}</p>
          <p v-if="projectError" class="form-message error">{{ projectError }}</p>
          <p v-if="clipboardMessage" class="form-message success">{{ clipboardMessage }}</p>

          <div class="project-list">
            <div class="project-list-header">
              <strong>项目列表</strong>
              <button type="button" class="text-button" @click="loadProjects">刷新</button>
            </div>
            <div v-if="projectLoading" class="empty-state">正在读取项目文件...</div>
            <div v-else-if="projects.length === 0" class="empty-state">暂无项目，先创建一个项目。</div>
            <template v-else>
              <article
                v-for="project in projects"
                :key="project.id"
                class="project-item"
                :class="{ selected: selectedProjectId === project.id }"
                @click="selectProject(project.id)"
              >
                <div>
                  <strong>{{ project.name }}</strong>
                  <small>{{ project.description || '暂无描述' }}</small>
                  <code class="project-id">{{ project.id }}</code>
                </div>
                <div class="project-meta">
                  <span>{{ project.language }}</span>
                  <span>{{ formatDate(project.createdAt) }}</span>
                  <div class="item-actions">
                    <button type="button" class="mini-button" @click.stop="startEditProject(project)">编辑</button>
                    <button type="button" class="mini-button" @click.stop="copyProjectId(project.id)">复制 ID</button>
                    <button type="button" class="mini-button danger" @click.stop="removeProject(project)">删除</button>
                  </div>
                </div>
              </article>
            </template>
          </div>
        </div>

        <section class="project-manager">
          <div class="manager-header">
            <div>
              <p class="eyebrow">Local File Storage</p>
              <h2>项目文件管理</h2>
            </div>
            <button type="button" class="secondary-button" @click="loadProjects">刷新项目和文件</button>
          </div>
          <p v-if="managementMessage" class="form-message success">{{ managementMessage }}</p>
          <p v-if="managementError" class="form-message error">{{ managementError }}</p>
          <div v-if="projectLoading" class="empty-state">正在读取项目和文件...</div>
          <div v-else-if="projects.length === 0" class="empty-state">暂无项目，先在上方创建一个项目。</div>
          <div v-else class="project-board">
            <article
              v-for="project in projects"
              :key="project.id"
              class="project-detail-card"
              :class="{ selected: selectedProjectId === project.id }"
            >
              <div class="detail-card-header">
                <div>
                  <strong>{{ project.name }}</strong>
                  <small>{{ project.description || '暂无描述' }}</small>
                  <code class="project-id">{{ project.id }}</code>
                </div>
                <div class="detail-actions">
                  <button type="button" class="mini-button" @click="selectProject(project.id)">设为当前</button>
                  <button type="button" class="mini-button" @click="startEditProject(project)">编辑项目</button>
                  <button type="button" class="mini-button" @click="copyProjectId(project.id)">复制 ID</button>
                  <button type="button" class="mini-button danger" @click="removeProject(project)">删除项目</button>
                </div>
              </div>

              <div class="project-stats">
                <article>
                  <span>语言</span>
                  <strong>{{ project.language }}</strong>
                </article>
                <article>
                  <span>文件数</span>
                  <strong>{{ projectFiles(project.id).length }}</strong>
                </article>
                <article>
                  <span>任务数</span>
                  <strong>{{ projectTasks(project.id).length }}</strong>
                </article>
              </div>

              <div class="task-history-list">
                <div class="task-history-header">
                  <strong>历史度量任务</strong>
                  <span>最近 {{ projectTasks(project.id).length }} 条</span>
                </div>
                <div v-if="projectTasks(project.id).length === 0" class="empty-state compact">暂无历史任务</div>
                <article v-for="task in projectTasks(project.id).slice(0, 5)" :key="task.taskId" class="task-history-item">
                  <div>
                    <strong>{{ task.type }}</strong>
                    <small>{{ task.taskId }}</small>
                  </div>
                  <span>{{ task.status }} · {{ formatDate(task.createdAt) }}</span>
                </article>
              </div>

              <div class="project-upload-row">
                <label class="upload-button compact-upload">
                  选择文件
                  <input
                    type="file"
                    accept=".zip,.java,.oom,.xml,.xmi"
                    hidden
                    @change="handleManagementFileChange(project.id, $event)"
                  />
                </label>
                <span>{{ managementSelectedFiles[project.id]?.name || '支持 zip、java、oom、xml、xmi' }}</span>
                <button
                  type="button"
                  class="primary-button"
                  :disabled="managementUploadLoading[project.id]"
                  @click="submitManagementUpload(project)"
                >
                  {{ managementUploadLoading[project.id] ? '上传中...' : '上传到此项目' }}
                </button>
              </div>

              <div class="managed-file-list">
                <div class="file-toolbar">
                  <input
                    :value="fileState(project.id).keyword"
                    type="search"
                    placeholder="搜索文件名、类型、ID"
                    @input="updateFileState(project.id, { keyword: $event.target.value, page: 1 })"
                  />
                  <select
                    :value="fileState(project.id).sortBy"
                    @change="updateFileState(project.id, { sortBy: $event.target.value, page: 1 })"
                  >
                    <option value="uploadedAt">按上传时间</option>
                    <option value="name">按文件名</option>
                    <option value="type">按类型</option>
                    <option value="size">按大小</option>
                  </select>
                  <select
                    :value="fileState(project.id).sortOrder"
                    @change="updateFileState(project.id, { sortOrder: $event.target.value, page: 1 })"
                  >
                    <option value="desc">降序</option>
                    <option value="asc">升序</option>
                  </select>
                  <select
                    :value="fileState(project.id).pageSize"
                    @change="updateFileState(project.id, { pageSize: Number($event.target.value), page: 1 })"
                  >
                    <option :value="5">每页 5 条</option>
                    <option :value="10">每页 10 条</option>
                    <option :value="20">每页 20 条</option>
                  </select>
                </div>
                <div class="file-category-tabs">
                  <button
                    v-for="category in fileCategories(project.id)"
                    :key="category.key"
                    type="button"
                    class="file-category-button"
                    :class="{ active: fileState(project.id).type === category.key }"
                    @click="updateFileState(project.id, { type: category.key, page: 1 })"
                  >
                    {{ category.label }}
                  </button>
                </div>
                <div class="file-page-info">
                  共 {{ filteredProjectFiles(project.id).length }} 条，当前第 {{ currentFilePage(project.id) }} / {{ pageCount(project.id) }} 页
                </div>
                <div v-if="projectFiles(project.id).length === 0" class="empty-state compact">该项目暂无上传文件</div>
                <div v-else-if="filteredProjectFiles(project.id).length === 0" class="empty-state compact">没有匹配当前筛选条件的文件</div>
                <article v-for="file in pagedProjectFiles(project.id)" :key="file.id" class="managed-file-item">
                  <div>
                    <strong>{{ file.originalName }}</strong>
                    <small>{{ file.fileType }} · {{ formatFileSize(file.size) }} · {{ formatDate(file.uploadedAt) }}</small>
                  </div>
                  <button type="button" class="mini-button danger" @click="removeManagementFile(project, file)">删除</button>
                </article>
                <div v-if="filteredProjectFiles(project.id).length > 0" class="file-pagination">
                  <button type="button" class="mini-button" :disabled="currentFilePage(project.id) <= 1" @click="changeFilePage(project.id, currentFilePage(project.id) - 1)">
                    上一页
                  </button>
                  <span>第 {{ currentFilePage(project.id) }} / {{ pageCount(project.id) }} 页</span>
                  <button type="button" class="mini-button" :disabled="currentFilePage(project.id) >= pageCount(project.id)" @click="changeFilePage(project.id, currentFilePage(project.id) + 1)">
                    下一页
                  </button>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section class="project-section threshold-section">
          <div class="manager-header">
            <div>
              <p class="eyebrow">Risk Thresholds</p>
              <h2>指标阈值配置</h2>
            </div>
            <button type="button" class="secondary-button" @click="loadThresholdConfig">重新读取</button>
          </div>
          <p v-if="thresholdMessage" class="form-message success">{{ thresholdMessage }}</p>
          <p v-if="thresholdError" class="form-message error">{{ thresholdError }}</p>
          <div class="threshold-grid">
            <article v-for="(value, metric) in thresholdConfig" :key="metric">
              <h3>{{ metric }}</h3>
              <label><span>低风险上限</span><input v-model.number="value.low" type="number" min="0" step="0.01" /></label>
              <label><span>中风险上限</span><input v-model.number="value.medium" type="number" min="0" step="0.01" /></label>
              <label><span>高风险上限</span><input v-model.number="value.high" type="number" min="0" step="0.01" /></label>
            </article>
          </div>
          <button type="button" class="primary-button" @click="submitThresholdConfig">保存阈值配置</button>
        </section>
        </template>

        <template v-else>
        <div class="metric-grid">
          <article class="metric-card">
            <span class="metric-label">项目数量</span>
            <strong>{{ projects.length }}</strong>
            <small>保存于本地 projects.json</small>
          </article>
          <article class="metric-card">
            <span class="metric-label">{{ metricLabel('main') }}</span>
            <strong>{{ metricCardValue('main') }}</strong>
            <small>{{ metricNote('main') }}</small>
          </article>
          <article class="metric-card">
            <span class="metric-label">{{ metricLabel('secondary') }}</span>
            <strong>{{ metricCardValue('secondary') }}</strong>
            <small>{{ metricNote('secondary') }}</small>
          </article>
          <article class="metric-card">
            <span class="metric-label">数据目录</span>
            <strong>{{ storageRoot ? '已初始化' : '待检查' }}</strong>
            <small>{{ storageRoot || healthError || '正在检查后端连接' }}</small>
          </article>
        </div>

        <div class="analysis-layout">
          <section class="chart-area">
            <template v-if="activeMenu === 'loc'">
              <div class="loc-header">
                <div>
                  <p class="eyebrow">LoC Result</p>
                  <h2>代码行统计结果</h2>
                </div>
                <button type="button" class="primary-button" :disabled="locLoading" @click="runLocAnalysis">
                  {{ locLoading ? '分析中...' : '开始 LoC 分析' }}
                </button>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="!locResult"
                  @click="exportLocMarkdown"
                >
                  导出 Markdown
                </button>
              </div>
              <p v-if="locMessage" class="form-message success loc-message">{{ locMessage }}</p>
              <p v-if="locReportMessage" class="form-message success loc-message">{{ locReportMessage }}</p>
              <p v-if="locError" class="form-message error loc-message">{{ locError }}</p>
              <div v-if="!locResult" class="empty-state loc-empty">
                暂无代码行度量结果。请先上传 `.java` 文件或包含 Java 文件的 `.zip`，然后点击“开始 LoC 分析”。
              </div>
              <div v-else class="loc-result">
                <div class="loc-summary-grid">
                  <article>
                    <span>Java 文件数</span>
                    <strong>{{ locResult.summary.fileCount }}</strong>
                  </article>
                  <article>
                    <span>总行数</span>
                    <strong>{{ locResult.summary.totalLines }}</strong>
                  </article>
                  <article>
                    <span>有效代码行</span>
                    <strong>{{ locResult.summary.sourceLines }}</strong>
                  </article>
                  <article>
                    <span>注释率</span>
                    <strong>{{ percent(locResult.summary.commentRate) }}</strong>
                  </article>
                </div>
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
                      <tr v-for="file in locResult.files" :key="`${file.sourceUploadName}-${file.fileName}`">
                        <td>{{ file.fileName }}</td>
                        <td>{{ file.sourceUploadName }}</td>
                        <td>{{ file.totalLines }}</td>
                        <td>{{ file.sourceLines }}</td>
                        <td>{{ file.commentLines }}</td>
                        <td>{{ file.blankLines }}</td>
                        <td>{{ percent(file.commentRate) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </template>

            <template v-else-if="activeMenu === 'control-flow'">
              <div class="loc-header">
                <div>
                  <p class="eyebrow">Cyclomatic Complexity Result</p>
                  <h2>控制流复杂度分析结果</h2>
                </div>
                <button type="button" class="primary-button" :disabled="complexityLoading" @click="runComplexityAnalysis">
                  {{ complexityLoading ? '分析中...' : '开始复杂度分析' }}
                </button>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="!complexityResult"
                  @click="exportComplexityMarkdown"
                >
                  导出 Markdown
                </button>
              </div>
              <p v-if="complexityMessage" class="form-message success loc-message">{{ complexityMessage }}</p>
              <p v-if="complexityReportMessage" class="form-message success loc-message">{{ complexityReportMessage }}</p>
              <p v-if="complexityError" class="form-message error loc-message">{{ complexityError }}</p>
              <p v-if="isLegacyComplexityResult()" class="form-message warning loc-message">
                当前读取到的是旧版本复杂度结果，缺少文件扫描明细。请重启后端并重新点击“开始复杂度分析”。
              </p>
              <div v-if="!complexityResult" class="empty-state loc-empty">
                暂无圈复杂度分析结果。请先上传 `.java` 文件或包含 Java 文件的 `.zip`，然后点击“开始复杂度分析”。
              </div>
              <div v-else class="loc-result">
                <div class="loc-summary-grid">
                  <article>
                    <span>Java 文件数</span>
                    <strong>{{ complexityResult.summary.fileCount }}</strong>
                  </article>
                  <article>
                    <span>方法数</span>
                    <strong>{{ complexityResult.summary.methodCount }}</strong>
                  </article>
                  <article>
                    <span>平均复杂度</span>
                    <strong>{{ complexityResult.summary.averageComplexity }}</strong>
                  </article>
                  <article>
                    <span>需关注方法</span>
                    <strong>{{ complexityResult.summary.highRiskMethodCount }}</strong>
                  </article>
                </div>
                <div class="loc-table-wrap">
                  <table class="loc-table complexity-table">
                    <thead>
                      <tr>
                        <th>文件</th>
                        <th>来源</th>
                        <th>声明方法数</th>
                        <th>可分析方法体</th>
                        <th>扫描状态</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="file in complexityResult.files || []"
                        :key="`${file.sourceUploadName}-${file.fileName}`"
                      >
                        <td>{{ file.fileName }}</td>
                        <td>{{ file.sourceUploadName }}</td>
                        <td>{{ file.declaredMethodCount }}</td>
                        <td>{{ file.executableMethodCount }}</td>
                        <td>{{ file.status }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-if="complexityResult.methods.length === 0" class="empty-state compact">
                  已扫描 Java 文件，但未发现带方法体的可执行方法。接口和抽象声明没有控制流分支，圈复杂度不单独计算。
                </div>
                <div v-else class="loc-table-wrap">
                  <table class="loc-table complexity-table">
                    <thead>
                      <tr>
                        <th>方法</th>
                        <th>文件</th>
                        <th>来源</th>
                        <th>行号</th>
                        <th>圈复杂度</th>
                        <th>风险等级</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="method in complexityResult.methods"
                        :key="`${method.sourceUploadName}-${method.fileName}-${method.startLine}-${method.methodName}`"
                      >
                        <td>{{ method.methodName }}</td>
                        <td>{{ method.fileName }}</td>
                        <td>{{ method.sourceUploadName }}</td>
                        <td>{{ method.startLine }}-{{ method.endLine }}</td>
                        <td>{{ method.cyclomaticComplexity }}</td>
                        <td>
                          <span class="risk-badge" :class="method.riskLevel.toLowerCase()">
                            {{ riskLabel(method.riskLevel) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </template>

            <template v-else-if="activeMenu === 'function-point'">
              <div class="loc-header">
                <div>
                  <p class="eyebrow">IFPUG Function Point</p>
                  <h2>功能点度量结果</h2>
                </div>
                <button type="button" class="primary-button" :disabled="functionPointLoading" @click="runFunctionPointAnalysis">
                  {{ functionPointLoading ? '计算中...' : '计算功能点' }}
                </button>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="!functionPointResult"
                  @click="exportFunctionPointMarkdown"
                >
                  导出 Markdown
                </button>
              </div>
              <div class="function-point-form">
                <article>
                  <h3>外部输入 EI</h3>
                  <input v-model.number="functionPointForm.externalInputs.low" type="number" min="0" placeholder="低" />
                  <input v-model.number="functionPointForm.externalInputs.average" type="number" min="0" placeholder="中" />
                  <input v-model.number="functionPointForm.externalInputs.high" type="number" min="0" placeholder="高" />
                </article>
                <article>
                  <h3>外部输出 EO</h3>
                  <input v-model.number="functionPointForm.externalOutputs.low" type="number" min="0" placeholder="低" />
                  <input v-model.number="functionPointForm.externalOutputs.average" type="number" min="0" placeholder="中" />
                  <input v-model.number="functionPointForm.externalOutputs.high" type="number" min="0" placeholder="高" />
                </article>
                <article>
                  <h3>外部查询 EQ</h3>
                  <input v-model.number="functionPointForm.externalInquiries.low" type="number" min="0" placeholder="低" />
                  <input v-model.number="functionPointForm.externalInquiries.average" type="number" min="0" placeholder="中" />
                  <input v-model.number="functionPointForm.externalInquiries.high" type="number" min="0" placeholder="高" />
                </article>
                <article>
                  <h3>内部逻辑文件 ILF</h3>
                  <input v-model.number="functionPointForm.internalLogicalFiles.low" type="number" min="0" placeholder="低" />
                  <input v-model.number="functionPointForm.internalLogicalFiles.average" type="number" min="0" placeholder="中" />
                  <input v-model.number="functionPointForm.internalLogicalFiles.high" type="number" min="0" placeholder="高" />
                </article>
                <article>
                  <h3>外部接口文件 EIF</h3>
                  <input v-model.number="functionPointForm.externalInterfaceFiles.low" type="number" min="0" placeholder="低" />
                  <input v-model.number="functionPointForm.externalInterfaceFiles.average" type="number" min="0" placeholder="中" />
                  <input v-model.number="functionPointForm.externalInterfaceFiles.high" type="number" min="0" placeholder="高" />
                </article>
                <article>
                  <h3>通用系统特征总分</h3>
                  <input :value="functionPointGscTotal()" type="number" min="0" max="70" readonly />
                  <button type="button" class="primary-button" :disabled="functionPointLoading" @click="runFunctionPointAnalysis">
                    {{ functionPointLoading ? '计算中...' : '计算' }}
                  </button>
                </article>
              </div>
              <div class="factor-score-panel">
                <div class="factor-score-header">
                  <div>
                    <h3>14 项通用系统特征 GSC</h3>
                    <p>每项 0-5 分，系统按 IFPUG 公式自动汇总为 VAF 调整因子。</p>
                  </div>
                  <strong>总分 {{ functionPointGscTotal() }} / 70</strong>
                </div>
                <div class="factor-score-grid">
                  <label v-for="(label, index) in gscLabels" :key="label">
                    <span>{{ index + 1 }}. {{ label }}</span>
                    <input v-model.number="functionPointForm.generalSystemCharacteristics[index]" type="number" min="0" max="5" />
                  </label>
                </div>
              </div>
              <p v-if="functionPointMessage" class="form-message success loc-message">{{ functionPointMessage }}</p>
              <p v-if="functionPointReportMessage" class="form-message success loc-message">{{ functionPointReportMessage }}</p>
              <p v-if="functionPointError" class="form-message error loc-message">{{ functionPointError }}</p>
              <div v-if="!functionPointResult" class="empty-state loc-empty">
                暂无功能点结果。录入 EI、EO、EQ、ILF、EIF 的低/中/高复杂度数量后即可计算。
              </div>
              <div v-else class="loc-result">
                <div class="loc-summary-grid">
                  <article>
                    <span>UFP</span>
                    <strong>{{ functionPointResult.unadjustedFunctionPoints }}</strong>
                  </article>
                  <article>
                    <span>VAF</span>
                    <strong>{{ functionPointResult.valueAdjustmentFactor }}</strong>
                  </article>
                  <article>
                    <span>AFP</span>
                    <strong>{{ functionPointResult.adjustedFunctionPoints }}</strong>
                  </article>
                  <article>
                    <span>GSC 总分</span>
                    <strong>{{ functionPointResult.generalSystemCharacteristicTotal }}</strong>
                  </article>
                </div>
                <div class="estimation-result-grid">
                  <article>
                    <span>外部输入 EI</span>
                    <strong>{{ functionPointResult.externalInputs }}</strong>
                  </article>
                  <article>
                    <span>外部输出 EO</span>
                    <strong>{{ functionPointResult.externalOutputs }}</strong>
                  </article>
                  <article>
                    <span>外部查询 EQ</span>
                    <strong>{{ functionPointResult.externalInquiries }}</strong>
                  </article>
                  <article>
                    <span>逻辑文件</span>
                    <strong>{{ functionPointResult.internalLogicalFiles + functionPointResult.externalInterfaceFiles }}</strong>
                  </article>
                </div>
              </div>
            </template>

            <template v-else-if="activeMenu === 'use-case'">
              <div class="loc-header">
                <div>
                  <p class="eyebrow">Use Case Point</p>
                  <h2>用例点估算结果</h2>
                </div>
                <button type="button" class="primary-button" :disabled="useCasePointLoading" @click="runUseCasePointAnalysis">
                  {{ useCasePointLoading ? '估算中...' : '计算用例点' }}
                </button>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="!useCasePointResult"
                  @click="exportUseCasePointMarkdown"
                >
                  导出 Markdown
                </button>
              </div>
              <div class="use-case-form">
                <article>
                  <h3>参与者复杂度</h3>
                  <label><span>简单</span><input v-model.number="useCasePointForm.simpleActors" type="number" min="0" /></label>
                  <label><span>一般</span><input v-model.number="useCasePointForm.averageActors" type="number" min="0" /></label>
                  <label><span>复杂</span><input v-model.number="useCasePointForm.complexActors" type="number" min="0" /></label>
                </article>
                <article>
                  <h3>用例复杂度</h3>
                  <label><span>简单</span><input v-model.number="useCasePointForm.simpleUseCases" type="number" min="0" /></label>
                  <label><span>一般</span><input v-model.number="useCasePointForm.averageUseCases" type="number" min="0" /></label>
                  <label><span>复杂</span><input v-model.number="useCasePointForm.complexUseCases" type="number" min="0" /></label>
                </article>
                <article>
                  <h3>调整参数</h3>
                  <label><span>技术因子总分</span><input :value="useCaseTechnicalTotal()" type="number" min="0" max="65" readonly /></label>
                  <label><span>环境因子总分</span><input :value="useCaseEnvironmentalTotal()" type="number" min="0" max="40" readonly /></label>
                  <label><span>小时/用例点</span><input v-model.number="useCasePointForm.productivityHoursPerUseCasePoint" type="number" min="1" /></label>
                </article>
              </div>
              <div class="factor-score-panel">
                <div class="factor-score-header">
                  <div>
                    <h3>13 项技术复杂度因子 TCF</h3>
                    <p>每项 0-5 分，系统自动按 TCF = 0.6 + 0.01 × 技术因子总分计算。</p>
                  </div>
                  <strong>总分 {{ useCaseTechnicalTotal() }} / 65</strong>
                </div>
                <div class="factor-score-grid">
                  <label v-for="(label, index) in technicalFactorLabels" :key="label">
                    <span>{{ index + 1 }}. {{ label }}</span>
                    <input v-model.number="useCasePointForm.technicalFactors[index]" type="number" min="0" max="5" />
                  </label>
                </div>
              </div>
              <div class="factor-score-panel compact">
                <div class="factor-score-header">
                  <div>
                    <h3>8 项环境复杂度因子 ECF</h3>
                    <p>每项 0-5 分，系统自动按 ECF = 1.4 - 0.03 × 环境因子总分计算。</p>
                  </div>
                  <strong>总分 {{ useCaseEnvironmentalTotal() }} / 40</strong>
                </div>
                <div class="factor-score-grid">
                  <label v-for="(label, index) in environmentalFactorLabels" :key="label">
                    <span>{{ index + 1 }}. {{ label }}</span>
                    <input v-model.number="useCasePointForm.environmentalFactors[index]" type="number" min="0" max="5" />
                  </label>
                </div>
              </div>
              <p v-if="useCasePointMessage" class="form-message success loc-message">{{ useCasePointMessage }}</p>
              <p v-if="useCasePointReportMessage" class="form-message success loc-message">{{ useCasePointReportMessage }}</p>
              <p v-if="useCasePointError" class="form-message error loc-message">{{ useCasePointError }}</p>
              <div v-if="!useCasePointResult" class="empty-state loc-empty">
                暂无用例点结果。录入参与者、用例和调整因子后即可估算用例点与工作量。
              </div>
              <div v-else class="loc-result">
                <div class="loc-summary-grid">
                  <article>
                    <span>UCP</span>
                    <strong>{{ useCasePointResult.useCasePoints }}</strong>
                  </article>
                  <article>
                    <span>估算工时</span>
                    <strong>{{ useCasePointResult.estimatedHours }}</strong>
                  </article>
                  <article>
                    <span>估算人月</span>
                    <strong>{{ useCasePointResult.estimatedPersonMonths }}</strong>
                  </article>
                  <article>
                    <span>UUCP</span>
                    <strong>{{ useCasePointResult.unadjustedUseCasePoints }}</strong>
                  </article>
                </div>
                <div class="estimation-result-grid">
                  <article>
                    <span>UAW</span>
                    <strong>{{ useCasePointResult.actorWeight }}</strong>
                  </article>
                  <article>
                    <span>UUCW</span>
                    <strong>{{ useCasePointResult.useCaseWeight }}</strong>
                  </article>
                  <article>
                    <span>TCF</span>
                    <strong>{{ useCasePointResult.technicalComplexityFactor }}</strong>
                  </article>
                  <article>
                    <span>ECF</span>
                    <strong>{{ useCasePointResult.environmentalComplexityFactor }}</strong>
                  </article>
                </div>
              </div>
            </template>

            <template v-else-if="activeMenu === 'model-analysis'">
              <div class="loc-header">
                <div>
                  <p class="eyebrow">UML / XMI / OOM</p>
                  <h2>模型文件度量结果</h2>
                </div>
                <button type="button" class="primary-button" :disabled="modelLoading" @click="runModelAnalysis">
                  {{ modelLoading ? '分析中...' : '开始模型分析' }}
                </button>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="!modelResult"
                  @click="exportModelMarkdown"
                >
                  导出 Markdown
                </button>
              </div>
              <p v-if="modelMessage" class="form-message success loc-message">{{ modelMessage }}</p>
              <p v-if="modelReportMessage" class="form-message success loc-message">{{ modelReportMessage }}</p>
              <p v-if="modelError" class="form-message error loc-message">{{ modelError }}</p>
              <div v-if="!modelResult" class="empty-state loc-empty">
                暂无模型度量结果。请上传 `.xml`、`.xmi` 或 `.oom` 模型文件，然后点击“开始模型分析”。
              </div>
              <div v-else class="loc-result">
                <div class="loc-summary-grid">
                  <article>
                    <span>模型文件数</span>
                    <strong>{{ modelResult.summary.fileCount }}</strong>
                  </article>
                  <article>
                    <span>类/接口数</span>
                    <strong>{{ modelResult.summary.classCount + modelResult.summary.interfaceCount }}</strong>
                  </article>
                  <article>
                    <span>属性/操作</span>
                    <strong>{{ modelResult.summary.attributeCount + modelResult.summary.operationCount }}</strong>
                  </article>
                  <article>
                    <span>高风险类</span>
                    <strong>{{ modelResult.summary.highRiskClassCount }}</strong>
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
                      <tr v-for="item in modelResult.classes" :key="`${item.sourceUploadName}-${item.className}`">
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
                            {{ riskLabel(item.riskLevel) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </template>

            <template v-else-if="activeMenu === 'object-oriented'">
              <div class="loc-header">
                <div>
                  <p class="eyebrow">CK / LK Result</p>
                  <h2>面向对象度量结果</h2>
                </div>
                <button type="button" class="primary-button" :disabled="ooLoading" @click="runOoAnalysis">
                  {{ ooLoading ? '分析中...' : '开始 CK/LK 分析' }}
                </button>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="!ooResult"
                  @click="exportOoMarkdown"
                >
                  导出 Markdown
                </button>
              </div>
              <p v-if="ooMessage" class="form-message success loc-message">{{ ooMessage }}</p>
              <p v-if="ooReportMessage" class="form-message success loc-message">{{ ooReportMessage }}</p>
              <p v-if="ooError" class="form-message error loc-message">{{ ooError }}</p>
              <div v-if="!ooResult" class="empty-state loc-empty">
                暂无面向对象度量结果。请先上传 `.java` 文件或包含 Java 文件的 `.zip`，然后点击“开始 CK/LK 分析”。
              </div>
              <div v-else class="loc-result">
                <div class="loc-summary-grid">
                  <article>
                    <span>Java 文件数</span>
                    <strong>{{ ooResult.summary.fileCount }}</strong>
                  </article>
                  <article>
                    <span>类/接口数</span>
                    <strong>{{ ooResult.summary.classCount + ooResult.summary.interfaceCount }}</strong>
                  </article>
                  <article>
                    <span>平均 CBO</span>
                    <strong>{{ ooResult.summary.averageCbo }}</strong>
                  </article>
                  <article>
                    <span>风险类</span>
                    <strong>{{ ooResult.summary.highRiskClassCount }}</strong>
                  </article>
                </div>
                <div class="oo-explain">
                  <span>CK：CBO、DIT、NOC、WMC、LCOM</span>
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
                        v-for="series in radarSeries()"
                        :key="series.name"
                        class="data-line"
                        :class="series.color"
                        :points="series.points"
                      />
                    </svg>
                  </div>
                  <div class="legend">
                    <span v-for="series in radarSeries()" :key="series.name">
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
                      <tr v-for="item in ooResult.classes" :key="`${item.sourceUploadName}-${item.className}`">
                        <td>{{ item.className }}</td>
                        <td>{{ item.type }}</td>
                        <td>{{ item.fileName }}</td>
                        <td>{{ item.cbo }}</td>
                        <td>{{ item.dit }}</td>
                        <td>{{ item.noc }}</td>
                        <td>{{ item.noa }}</td>
                        <td>{{ item.noo }}</td>
                        <td>{{ item.cs }}</td>
                        <td>{{ item.wmc }}</td>
                        <td>{{ item.lcom }}</td>
                        <td>
                          <span class="risk-badge" :class="item.riskLevel.toLowerCase()">
                            {{ riskLabel(item.riskLevel) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </template>

            <template v-else-if="activeMenu === 'ai'">
              <div class="loc-header">
                <div>
                  <p class="eyebrow">LocalRuleAnalyzer</p>
                  <h2>智能质量分析结果</h2>
                </div>
                <button type="button" class="primary-button" :disabled="aiLoading" @click="runAiAnalysis">
                  {{ aiLoading ? '分析中...' : '生成智能建议' }}
                </button>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="!aiResult"
                  @click="exportAiMarkdown"
                >
                  导出 Markdown
                </button>
              </div>
              <p v-if="aiMessage" class="form-message success loc-message">{{ aiMessage }}</p>
              <p v-if="aiError" class="form-message error loc-message">{{ aiError }}</p>
              <div v-if="!aiResult" class="empty-state loc-empty">
                暂无智能分析结果。点击“生成智能建议”后，系统会基于 LoC、圈复杂度、CK/LK 和估算结果生成质量评价。
              </div>
              <div v-else class="ai-result">
                <article class="analysis-card">
                  <span>总体评价</span>
                  <p>{{ aiResult.overallAssessment }}</p>
                  <small>分析器：{{ aiResult.modelName }} · {{ formatDate(aiResult.analyzedAt) }}</small>
                </article>
                <div class="analysis-columns">
                  <article>
                    <h3>主要风险</h3>
                    <ul>
                      <li v-for="item in aiResult.riskItems" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                  <article>
                    <h3>重构建议</h3>
                    <ul>
                      <li v-for="item in aiResult.refactoringSuggestions" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                  <article>
                    <h3>测试建议</h3>
                    <ul>
                      <li v-for="item in aiResult.testSuggestions" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                </div>
              </div>
            </template>

            <template v-else-if="activeMenu === 'estimation'">
              <div class="loc-header">
                <div>
                  <p class="eyebrow">COCOMO Basic Result</p>
                  <h2>工作量与成本估算结果</h2>
                </div>
                <button type="button" class="primary-button" :disabled="estimationLoading" @click="runEstimationAnalysis">
                  {{ estimationLoading ? '估算中...' : '开始估算' }}
                </button>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="!estimationResult"
                  @click="exportEstimationMarkdown"
                >
                  导出 Markdown
                </button>
              </div>
              <div class="estimation-form">
                <label>
                  <span>项目模式</span>
                  <select v-model="estimationForm.mode">
                    <option value="ORGANIC">有机型：小型、经验充足、约束少</option>
                    <option value="SEMIDETACHED">半独立型：中等规模、约束适中</option>
                    <option value="EMBEDDED">嵌入型：强约束、复杂环境</option>
                  </select>
                </label>
                <label>
                  <span>KLOC</span>
                  <input v-model="estimationForm.kloc" type="number" min="0" step="0.01" placeholder="留空则使用最新 LoC" />
                </label>
                <label>
                  <span>人月成本</span>
                  <input v-model="estimationForm.costPerPersonMonth" type="number" min="0" step="100" />
                </label>
                <button type="button" class="primary-button" :disabled="estimationLoading" @click="runEstimationAnalysis">
                  {{ estimationLoading ? '估算中...' : '计算估算值' }}
                </button>
              </div>
              <p v-if="estimationMessage" class="form-message success loc-message">{{ estimationMessage }}</p>
              <p v-if="estimationReportMessage" class="form-message success loc-message">{{ estimationReportMessage }}</p>
              <p v-if="estimationError" class="form-message error loc-message">{{ estimationError }}</p>
              <div v-if="!estimationResult" class="empty-state loc-empty">
                暂无估算结果。可以留空 KLOC，系统会自动使用最新 LoC 结果；如果没有 LoC 结果，后端会先自动执行 LoC 分析。
              </div>
              <div v-else class="loc-result">
                <div class="loc-summary-grid">
                  <article>
                    <span>规模 KLOC</span>
                    <strong>{{ estimationResult.kloc }}</strong>
                  </article>
                  <article>
                    <span>工作量</span>
                    <strong>{{ estimationResult.effortPersonMonths }}</strong>
                  </article>
                  <article>
                    <span>开发周期</span>
                    <strong>{{ estimationResult.developmentMonths }}</strong>
                  </article>
                  <article>
                    <span>平均人员</span>
                    <strong>{{ estimationResult.averageStaff }}</strong>
                  </article>
                </div>
                <div class="estimation-result-grid">
                  <article>
                    <span>项目模式</span>
                    <strong>{{ estimationResult.modeLabel }}</strong>
                  </article>
                  <article>
                    <span>规模来源</span>
                    <strong>{{ estimationResult.scaleSource }}</strong>
                  </article>
                  <article>
                    <span>人月成本</span>
                    <strong>{{ estimationResult.costPerPersonMonth }}</strong>
                  </article>
                  <article>
                    <span>估算成本</span>
                    <strong>{{ estimationResult.estimatedCost }}</strong>
                  </article>
                </div>
              </div>
            </template>
          </section>

          <aside class="operation-panel">
            <label class="panel-label">
              <span>当前项目</span>
              <select v-model="selectedProjectId" @change="handleProjectSelectionChange">
                <option value="">请选择项目</option>
                <option v-for="project in projects" :key="project.id" :value="project.id">
                  {{ project.name }}
                </option>
              </select>
              <small>当前：{{ selectedProjectName() }}</small>
            </label>
            <label class="upload-button">
              点击上传
              <input
                type="file"
                accept=".zip,.java,.oom,.xml,.xmi"
                hidden
                @change="handleFileChange"
              />
            </label>
            <div class="file-status">
              <span>{{ selectedFile ? selectedFile.name : '支持 zip、java、oom、xml、xmi 文件' }}</span>
              <span class="success-dot"></span>
            </div>
            <button type="button" class="primary-button" :disabled="uploadLoading" @click="submitUpload">
              {{ uploadLoading ? '上传中...' : '保存到项目' }}
            </button>
            <p v-if="uploadMessage" class="form-message success">{{ uploadMessage }}</p>
            <p v-if="uploadError" class="form-message error">{{ uploadError }}</p>
            <div class="upload-list">
              <div class="project-list-header">
                <strong>已上传文件</strong>
                <button type="button" class="text-button" @click="loadProjectFiles">刷新</button>
              </div>
              <div v-if="!selectedProjectId" class="empty-state compact">请先选择项目</div>
              <template v-else>
                <div class="file-toolbar">
                  <input
                    :value="fileState(selectedProjectId).keyword"
                    type="search"
                    placeholder="搜索文件名、类型、ID"
                    @input="updateFileState(selectedProjectId, { keyword: $event.target.value, page: 1 })"
                  />
                  <select
                    :value="fileState(selectedProjectId).sortBy"
                    @change="updateFileState(selectedProjectId, { sortBy: $event.target.value, page: 1 })"
                  >
                    <option value="uploadedAt">按上传时间</option>
                    <option value="name">按文件名</option>
                    <option value="type">按类型</option>
                    <option value="size">按大小</option>
                  </select>
                  <select
                    :value="fileState(selectedProjectId).sortOrder"
                    @change="updateFileState(selectedProjectId, { sortOrder: $event.target.value, page: 1 })"
                  >
                    <option value="desc">降序</option>
                    <option value="asc">升序</option>
                  </select>
                  <select
                    :value="fileState(selectedProjectId).pageSize"
                    @change="updateFileState(selectedProjectId, { pageSize: Number($event.target.value), page: 1 })"
                  >
                    <option :value="5">每页 5 条</option>
                    <option :value="10">每页 10 条</option>
                    <option :value="20">每页 20 条</option>
                  </select>
                </div>
                <div class="file-category-tabs">
                  <button
                    v-for="category in fileCategories(selectedProjectId)"
                    :key="category.key"
                    type="button"
                    class="file-category-button"
                    :class="{ active: fileState(selectedProjectId).type === category.key }"
                    @click="updateFileState(selectedProjectId, { type: category.key, page: 1 })"
                  >
                    {{ category.label }}
                  </button>
                </div>
                <div class="file-page-info">
                  共 {{ filteredProjectFiles(selectedProjectId).length }} 条，当前第 {{ currentFilePage(selectedProjectId) }} / {{ pageCount(selectedProjectId) }} 页
                </div>
                <div v-if="projectFiles(selectedProjectId).length === 0" class="empty-state compact">暂无上传文件</div>
                <div v-else-if="filteredProjectFiles(selectedProjectId).length === 0" class="empty-state compact">没有匹配当前筛选条件的文件</div>
                <article v-for="file in pagedProjectFiles(selectedProjectId)" :key="file.id" class="upload-item">
                  <div>
                    <strong>{{ file.originalName }}</strong>
                    <small>{{ file.fileType }} · {{ formatFileSize(file.size) }}</small>
                  </div>
                  <div class="upload-meta">
                    <span>{{ formatDate(file.uploadedAt) }}</span>
                    <button type="button" class="mini-button danger" @click="removeUploadedFile(file)">删除</button>
                  </div>
                </article>
                <div v-if="filteredProjectFiles(selectedProjectId).length > 0" class="file-pagination">
                  <button type="button" class="mini-button" :disabled="currentFilePage(selectedProjectId) <= 1" @click="changeFilePage(selectedProjectId, currentFilePage(selectedProjectId) - 1)">
                    上一页
                  </button>
                  <span>第 {{ currentFilePage(selectedProjectId) }} / {{ pageCount(selectedProjectId) }} 页</span>
                  <button type="button" class="mini-button" :disabled="currentFilePage(selectedProjectId) >= pageCount(selectedProjectId)" @click="changeFilePage(selectedProjectId, currentFilePage(selectedProjectId) + 1)">
                    下一页
                  </button>
                </div>
              </template>
            </div>
            <div class="button-row">
              <button type="button" class="secondary-button" @click="exportXml">
                导出 XML 格式
              </button>
              <button type="button" class="secondary-button" @click="exportComprehensiveMarkdown">
                导出综合报告
              </button>
              <button
                v-if="activeMenu === 'loc'"
                type="button"
                class="primary-button"
                :disabled="locLoading"
                @click="runLocAnalysis"
              >
                {{ locLoading ? '分析中...' : '开始 LoC 分析' }}
              </button>
              <button
                v-else-if="activeMenu === 'control-flow'"
                type="button"
                class="primary-button"
                :disabled="complexityLoading"
                @click="runComplexityAnalysis"
              >
                {{ complexityLoading ? '分析中...' : '开始复杂度分析' }}
              </button>
              <button
                v-else-if="activeMenu === 'object-oriented'"
                type="button"
                class="primary-button"
                :disabled="ooLoading"
                @click="runOoAnalysis"
              >
                {{ ooLoading ? '分析中...' : '开始 CK/LK 分析' }}
              </button>
              <button
                v-else-if="activeMenu === 'model-analysis'"
                type="button"
                class="primary-button"
                :disabled="modelLoading"
                @click="runModelAnalysis"
              >
                {{ modelLoading ? '分析中...' : '模型文件度量' }}
              </button>
              <button
                v-else-if="activeMenu === 'function-point'"
                type="button"
                class="primary-button"
                :disabled="functionPointLoading"
                @click="runFunctionPointAnalysis"
              >
                {{ functionPointLoading ? '计算中...' : '功能点度量' }}
              </button>
              <button
                v-else-if="activeMenu === 'use-case'"
                type="button"
                class="primary-button"
                :disabled="useCasePointLoading"
                @click="runUseCasePointAnalysis"
              >
                {{ useCasePointLoading ? '估算中...' : '用例点估算' }}
              </button>
              <button
                v-else-if="activeMenu === 'estimation'"
                type="button"
                class="primary-button"
                :disabled="estimationLoading"
                @click="runEstimationAnalysis"
              >
                {{ estimationLoading ? '估算中...' : '开始估算' }}
              </button>
              <button
                v-else-if="activeMenu === 'ai'"
                type="button"
                class="primary-button"
                :disabled="aiLoading"
                @click="runAiAnalysis"
              >
                {{ aiLoading ? '分析中...' : 'AI 智能分析' }}
              </button>
              <button v-else type="button" class="primary-button disabled-button" disabled title="请选择一个分析模块">
                开始分析
              </button>
            </div>
            <p v-if="xmlExportMessage" class="form-message success">{{ xmlExportMessage }}</p>
            <p v-if="xmlExportError" class="form-message error">{{ xmlExportError }}</p>
            <p v-if="comprehensiveReportMessage" class="form-message success">{{ comprehensiveReportMessage }}</p>
            <p v-if="comprehensiveReportError" class="form-message error">{{ comprehensiveReportError }}</p>
            <button type="button" class="ai-button" :disabled="aiLoading" @click="runAiAnalysis">
              {{ aiLoading ? '智能分析中...' : 'AI 智能分析' }}
            </button>
            <p class="todo-hint">智能分析默认使用本地规则生成建议，可在后续扩展为外部大模型接口。</p>
          </aside>
        </div>
        </template>
      </section>
    </main>
  </div>
</template>
