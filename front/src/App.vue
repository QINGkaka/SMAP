<script setup>
import { computed, onMounted, ref } from 'vue'
import ProjectFilePanel from './components/file/ProjectFilePanel.vue'
import LocAnalysisView from './views/LocAnalysisView.vue'
import ComplexityAnalysisView from './views/ComplexityAnalysisView.vue'
import EstimationView from './views/EstimationView.vue'
import AiAnalysisView from './views/AiAnalysisView.vue'
import ModelAnalysisView from './views/ModelAnalysisView.vue'
import ObjectOrientedView from './views/ObjectOrientedView.vue'
import FunctionPointView from './views/FunctionPointView.vue'
import UseCasePointView from './views/UseCasePointView.vue'
import { useMetricModules } from './composables/useMetricModules'
import { useProjectFiles } from './composables/useProjectFiles'
import {
  createProject,
  deleteProject,
  exportComprehensiveReport,
  exportProjectXml,
  fetchProjectTasks,
  fetchProjects,
  fetchThresholds,
  saveThresholds,
  updateProject,
} from './services/api'

const menuGroups = [
  {
    title: '工作台',
    items: [
      { key: 'project-management', label: '项目管理', icon: '▣', implemented: true }
    ]
  },
  {
    title: '代码质量',
    items: [
      { key: 'loc', label: '代码行度量', icon: '≡', implemented: true },
      { key: 'control-flow', label: '控制流图度量', icon: '⌁', implemented: true },
      { key: 'object-oriented', label: '面向对象度量', icon: '▰', implemented: true },
      { key: 'model-analysis', label: '模型文件度量', icon: '◫', implemented: true }
    ]
  },
  {
    title: '规模与估算',
    items: [
      { key: 'function-point', label: '功能点度量', icon: '◆', implemented: true },
      { key: 'use-case', label: '用例图度量', icon: '⬟', implemented: true },
      { key: 'estimation', label: '估算分析', icon: '◇', implemented: true }
    ]
  },
  {
    title: '智能分析',
    items: [
      { key: 'ai', label: '智能分析', icon: '✦', implemented: true }
    ]
  }
]

const activeMenu = ref('project-management')
const backendStatus = ref('检查中')
const projects = ref([])
const projectLoading = ref(false)
const projectError = ref('')
const projectMessage = ref('')
const selectedProjectId = ref('')
const projectTaskMap = ref({})
const editingProjectId = ref('')
const xmlExportMessage = ref('')
const xmlExportError = ref('')
const comprehensiveReportMessage = ref('')
const comprehensiveReportError = ref('')
const clipboardMessage = ref('')
function createEmptyProjectForm() {
  return {
    name: '',
    language: 'Java',
    description: ''
  }
}

const projectForm = ref(createEmptyProjectForm())
const thresholdConfig = ref({})
const thresholdMessage = ref('')
const thresholdError = ref('')
const thresholdExpanded = ref(false)
const recentTaskLimit = 3
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
const {
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
  exportFunctionPointMarkdown,
  runUseCasePointAnalysis,
  exportUseCasePointMarkdown,
  runModelAnalysis,
  exportModelMarkdown
} = useMetricModules({
  selectedProjectId,
  activeMenu,
  downloadMarkdown
})
const {
  uploadedFiles,
  selectedFile,
  projectFileMap,
  managementSelectedFiles,
  managementUploadLoading,
  managementMessage,
  managementError,
  loadAllProjectFiles,
  loadFilesForProject,
  loadProjectFiles,
  handleManagementFileChange,
  submitManagementUpload,
  removeManagementFile,
  projectFiles,
  displayProjectFiles,
  fileState,
  updateFileState,
  fileCategories,
  filteredProjectFiles,
  pageCount,
  currentFilePage,
  changeFilePage
} = useProjectFiles({
  selectedProjectId,
  markMetricResultsStale,
  formatDate,
  formatFileSize
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
      await loadLatestActiveMetricResult()
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
    const payload = {
      name: projectForm.value.name.trim(),
      language: projectForm.value.language,
      description: projectForm.value.description.trim()
    }
    const isEditing = Boolean(editingProjectId.value)
    const result = isEditing
      ? await updateProject(editingProjectId.value, payload)
      : await createProject(payload)

    projectMessage.value = isEditing
      ? `已更新项目：${result.data.name}`
      : `已创建项目：${result.data.name}`

    cancelEditProject()
    await loadProjects()
    selectedProjectId.value = result.data.id
    await loadProjectFiles()
    await loadFilesForProject(result.data.id)
    resetMetricResults()
    activeMenu.value = 'project-management'
  } catch (error) {
    projectError.value = error.message
  }
}

async function selectProject(projectId) {
  selectedProjectId.value = projectId
  await handleProjectSelectionChange()
}

function beginCreateProject() {
  activeMenu.value = 'project-management'
  cancelEditProject()
}

function startEditProject(project) {
  editingProjectId.value = project.id
  projectForm.value = {
    name: project.name || '',
    language: project.language || 'Java',
    description: project.description || ''
  }
  projectMessage.value = ''
  projectError.value = ''
  activeMenu.value = 'project-management'
}

function cancelEditProject() {
  editingProjectId.value = ''
  projectForm.value = createEmptyProjectForm()
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
    if (editingProjectId.value === project.id) {
      cancelEditProject()
    }
    await loadProjects()
    await loadAllProjectFiles()
  } catch (error) {
    projectError.value = error.message
  }
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
    cancelEditProject()
    await loadProjects()
  } catch (error) {
    projectError.value = error.message
  }
}

async function handleProjectSelectionChange() {
  await loadProjectFiles()
  await loadLatestActiveMetricResult()
}

function updateEstimationForm(patch) {
  estimationForm.value = {
    ...estimationForm.value,
    ...patch
  }
}

function updateFunctionPointCount({ section, field, value }) {
  functionPointForm.value = {
    ...functionPointForm.value,
    [section]: {
      ...functionPointForm.value[section],
      [field]: value
    }
  }
}

function updateFunctionPointGsc({ index, value }) {
  const next = [...functionPointForm.value.generalSystemCharacteristics]
  next[index] = value
  functionPointForm.value = {
    ...functionPointForm.value,
    generalSystemCharacteristics: next
  }
}

function updateUseCaseField({ field, value }) {
  useCasePointForm.value = {
    ...useCasePointForm.value,
    [field]: value
  }
}

function updateUseCaseTechnical({ index, value }) {
  const next = [...useCasePointForm.value.technicalFactors]
  next[index] = value
  useCasePointForm.value = {
    ...useCasePointForm.value,
    technicalFactors: next
  }
}

function updateUseCaseEnvironmental({ index, value }) {
  const next = [...useCasePointForm.value.environmentalFactors]
  next[index] = value
  useCasePointForm.value = {
    ...useCasePointForm.value,
    environmentalFactors: next
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

const currentProject = computed(() =>
  projects.value.find(project => project.id === selectedProjectId.value) || null
)

const totalProjectFiles = computed(() =>
  Object.values(projectFileMap.value).reduce((total, files) => total + files.length, 0)
)

const totalProjectTasks = computed(() =>
  Object.values(projectTaskMap.value).reduce((total, tasks) => total + tasks.length, 0)
)

function isLegacyComplexityResult() {
  return complexityResult.value && !Array.isArray(complexityResult.value.files)
}

async function handleMenuClick(item) {
  if (!item.implemented) {
    return
  }
  activeMenu.value = item.key
  await loadLatestActiveMetricResult()
}

function percent(value) {
  return `${((value || 0) * 100).toFixed(1)}%`
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
    return '项目与文件'
  }
  if (activeMenu.value === 'loc') {
    return '代码行统计'
  }
  if (activeMenu.value === 'control-flow') {
    return '复杂度分析'
  }
  if (activeMenu.value === 'estimation') {
    return '工作量与成本'
  }
  if (activeMenu.value === 'function-point') {
    return '功能点分析'
  }
  if (activeMenu.value === 'use-case') {
    return '用例点分析'
  }
  if (activeMenu.value === 'model-analysis') {
    return '模型文件分析'
  }
  if (activeMenu.value === 'ai') {
    return '智能分析结果'
  }
  return '面向对象分析'
}

function activeDescription() {
  if (activeMenu.value === 'project-management') {
    return '在一个页面里完成项目创建、文件管理和任务查看。'
  }
  if (activeMenu.value === 'function-point') {
    return '录入 EI、EO、EQ、ILF、EIF 与 GSC 后，系统会自动生成 UFP、VAF 与 AFP。'
  }
  return '先选择项目并上传文件，再执行当前度量或导出报告。'
}

onMounted(async () => {
  await Promise.all([loadProjects(), loadThresholdConfig()])
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar" :class="{ 'topbar-minimal': activeMenu === 'control-flow' }">
      <div class="topbar-left">
        <div class="topbar-path">
          <span>数据可视化</span>
          <i>/</i>
          <strong>{{ activeTitle() }}</strong>
        </div>
        <small v-if="activeMenu !== 'control-flow'" class="topbar-caption">{{ activeDescription() }}</small>
      </div>
      <div v-if="activeMenu !== 'control-flow'" class="topbar-tools">
        <button type="button" class="topbar-icon-button" aria-label="search">⌕</button>
        <button type="button" class="topbar-icon-button" aria-label="language">文</button>
        <button type="button" class="topbar-icon-button" aria-label="theme">☼</button>
        <div class="status-pill" :class="{ offline: backendStatus !== 'UP' }">
          {{ backendStatus }}
        </div>
        <small v-if="storageRoot" class="storage-note">{{ storageRoot }}</small>
        <small v-else-if="healthError" class="storage-note error">{{ healthError }}</small>
        <button type="button" class="topbar-avatar" aria-label="profile">SM</button>
      </div>
    </header>

    <main class="workspace">
      <aside class="sidebar">
        <div class="sidebar-brand">
          <div class="brand">
            <span class="brand-mark"></span>
            <span>SMAP</span>
          </div>
          <small>软件度量平台</small>
        </div>
        <div class="sidebar-intro">
          <p>本地项目</p>
          <strong>{{ projects.length }} 个项目</strong>
          <small>当前：{{ selectedProjectName() }}</small>
        </div>
        <div class="metric-menu">
          <section v-for="group in menuGroups" :key="group.title" class="menu-group">
            <p class="menu-group-title">{{ group.title }}</p>
            <nav class="menu-group-items">
              <button
                v-for="item in group.items"
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
          </section>
        </div>
      </aside>

      <section class="content-panel" :class="{ 'content-panel-compact': activeMenu === 'control-flow' }">
        <div class="content-breadcrumb">
          <span>仪表盘</span>
          <i>/</i>
          <span>{{ activeEyebrow() }}</span>
          <i>/</i>
          <strong>{{ activeTitle() }}</strong>
        </div>
        <div v-if="activeMenu !== 'control-flow'" class="section-header" :class="{ 'hero-card': activeMenu === 'function-point' }">
          <div class="section-header-copy">
            <p class="eyebrow">{{ activeEyebrow() }}</p>
            <h1>{{ activeTitle() }}</h1>
            <p class="section-description">{{ activeDescription() }}</p>
          </div>
          <div class="section-header-meta">
            <article class="header-meta-card">
              <span>当前项目</span>
              <strong>{{ selectedProjectName() }}</strong>
            </article>
            <article class="header-meta-card">
              <span>累计文件</span>
              <strong>{{ totalProjectFiles }}</strong>
            </article>
            <article class="header-meta-card">
              <span>累计任务</span>
              <strong>{{ totalProjectTasks }}</strong>
            </article>
          </div>
          <div class="header-actions">
            <button type="button" class="ghost-button" @click="importSampleProject">
              导入示例
            </button>
            <button type="button" class="primary-button" @click="beginCreateProject">
              新建项目
            </button>
          </div>
        </div>

        <template v-if="activeMenu === 'project-management'">
          <div class="project-dashboard">
          <div class="project-workspace-grid">
            <section class="project-section">
              <div class="manager-header compact">
                <div>
                  <p class="eyebrow">项目表单</p>
                  <h2>{{ editingProjectId ? '编辑项目' : '新建项目' }}</h2>
                </div>
              </div>
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
            </section>

            <section class="project-section project-list-section">
              <div class="project-list-header">
                <div>
                  <strong>项目切换</strong>
                </div>
                <button type="button" class="text-button" @click="loadProjects">刷新</button>
              </div>
              <div v-if="projectLoading" class="empty-state">正在读取项目文件...</div>
              <div v-else-if="projects.length === 0" class="empty-state">暂无项目，先创建一个项目。</div>
              <div v-else class="project-list compact">
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
                    <button type="button" class="mini-button" @click.stop="startEditProject(project)">编辑</button>
                  </div>
                </article>
              </div>
            </section>
          </div>

          <section class="project-manager">
            <div class="manager-header">
              <div>
                <p class="eyebrow">文件与任务</p>
                <h2>当前项目</h2>
              </div>
              <button type="button" class="secondary-button" @click="loadProjects">刷新项目和文件</button>
            </div>
            <p v-if="managementMessage" class="form-message success">{{ managementMessage }}</p>
            <p v-if="managementError" class="form-message error">{{ managementError }}</p>
            <div v-if="projectLoading" class="empty-state">正在读取项目和文件...</div>
            <div v-else-if="!currentProject" class="empty-state">请选择一个项目，再查看文件和任务。</div>
            <article v-else class="project-detail-card current-project-card">
              <div class="detail-card-header">
                <div>
                  <strong>{{ currentProject.name }}</strong>
                  <small>{{ currentProject.description || '暂无描述' }}</small>
                  <code class="project-id">{{ currentProject.id }}</code>
                </div>
                <div class="detail-actions">
                  <button type="button" class="mini-button" @click="startEditProject(currentProject)">编辑项目</button>
                  <button type="button" class="mini-button" @click="copyProjectId(currentProject.id)">复制 ID</button>
                  <button type="button" class="mini-button danger" @click="removeProject(currentProject)">删除项目</button>
                </div>
              </div>

              <div class="project-stats">
                <article>
                  <span>语言</span>
                  <strong>{{ currentProject.language }}</strong>
                </article>
                <article>
                  <span>文件数</span>
                  <strong>{{ projectFiles(currentProject.id).length }}</strong>
                </article>
                <article>
                  <span>任务数</span>
                  <strong>{{ projectTasks(currentProject.id).length }}</strong>
                </article>
              </div>

              <div class="task-history-list">
                <div class="task-history-header">
                  <strong>历史度量任务</strong>
                  <span>最近 {{ Math.min(projectTasks(currentProject.id).length, recentTaskLimit) }} 条</span>
                </div>
                <div v-if="projectTasks(currentProject.id).length === 0" class="empty-state compact">暂无历史任务</div>
                <article v-for="task in projectTasks(currentProject.id).slice(0, recentTaskLimit)" :key="task.taskId" class="task-history-item">
                  <div>
                    <strong>{{ task.type }}</strong>
                    <small>{{ task.taskId }}</small>
                  </div>
                  <span>{{ task.status }} · {{ formatDate(task.createdAt) }}</span>
                </article>
              </div>

              <ProjectFilePanel
                :project-id="currentProject.id"
                :files="projectFiles(currentProject.id)"
                :state="fileState(currentProject.id)"
                :categories="fileCategories(currentProject.id)"
                :paged-files="displayProjectFiles(currentProject.id)"
                :filtered-count="filteredProjectFiles(currentProject.id).length"
                :current-page="currentFilePage(currentProject.id)"
                :page-count="pageCount(currentProject.id)"
                :show-upload-area="true"
                :selected-file-name="managementSelectedFiles[currentProject.id]?.name || ''"
                :upload-loading="Boolean(managementUploadLoading[currentProject.id])"
                upload-label="选择文件"
                upload-button-text="上传到当前项目"
                item-class="managed-file-item"
                empty-message="当前项目暂无上传文件"
                @file-change="handleManagementFileChange(currentProject.id, $event)"
                @upload="submitManagementUpload(currentProject)"
                @update-state="updateFileState(currentProject.id, $event)"
                @change-page="changeFilePage(currentProject.id, $event)"
                @remove-file="removeManagementFile(currentProject, $event)"
              />
            </article>
          </section>

          <section class="project-section threshold-section">
            <div class="manager-header">
              <div>
                <p class="eyebrow">风险阈值</p>
                <h2>指标阈值配置</h2>
              </div>
              <div class="button-row">
                <button type="button" class="ghost-button" @click="thresholdExpanded = !thresholdExpanded">
                  {{ thresholdExpanded ? '收起' : '展开' }}
                </button>
                <button v-if="thresholdExpanded" type="button" class="secondary-button" @click="loadThresholdConfig">重新读取</button>
              </div>
            </div>
            <div v-if="thresholdExpanded" class="section-toggle-panel">
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
              <button type="button" class="primary-button threshold-save-button" @click="submitThresholdConfig">保存阈值配置</button>
            </div>
          </section>
          </div>
        </template>

        <template v-else>
          <div class="analysis-layout" :class="{ 'control-flow-layout': activeMenu === 'control-flow' }">
            <section class="chart-area">
              <template v-if="activeMenu === 'loc'">
                <LocAnalysisView
                  :result="locResult"
                  :loading="locLoading"
                  :message="locMessage"
                  :report-message="locReportMessage"
                  :error-message="locError"
                  :format-percent="percent"
                  @analyze="runLocAnalysis"
                  @export="exportLocMarkdown"
                />
              </template>

              <template v-else-if="activeMenu === 'control-flow'">
                <ComplexityAnalysisView
                  :result="complexityResult"
                  :loading="complexityLoading"
                  :message="complexityMessage"
                  :report-message="complexityReportMessage"
                  :error-message="complexityError"
                  :legacy-warning-message="isLegacyComplexityResult() ? '当前读取到的是旧版本复杂度结果，缺少文件扫描明细。请重启后端并重新点击“开始复杂度分析”。' : ''"
                  :format-risk-label="riskLabel"
                  @analyze="runComplexityAnalysis"
                  @export="exportComplexityMarkdown"
                />
              </template>

              <template v-else-if="activeMenu === 'function-point'">
                <FunctionPointView
                  :result="functionPointResult"
                  :loading="functionPointLoading"
                  :message="functionPointMessage"
                  :report-message="functionPointReportMessage"
                  :error-message="functionPointError"
                  :form="functionPointForm"
                  :gsc-labels="gscLabels"
                  :gsc-total="functionPointGscTotal()"
                  @analyze="runFunctionPointAnalysis"
                  @export="exportFunctionPointMarkdown"
                  @update-count="updateFunctionPointCount"
                  @update-gsc="updateFunctionPointGsc"
                />
              </template>

              <template v-else-if="activeMenu === 'use-case'">
                <UseCasePointView
                  :result="useCasePointResult"
                  :loading="useCasePointLoading"
                  :message="useCasePointMessage"
                  :report-message="useCasePointReportMessage"
                  :error-message="useCasePointError"
                  :form="useCasePointForm"
                  :technical-labels="technicalFactorLabels"
                  :environmental-labels="environmentalFactorLabels"
                  :technical-total="useCaseTechnicalTotal()"
                  :environmental-total="useCaseEnvironmentalTotal()"
                  @analyze="runUseCasePointAnalysis"
                  @export="exportUseCasePointMarkdown"
                  @update-field="updateUseCaseField"
                  @update-technical="updateUseCaseTechnical"
                  @update-environmental="updateUseCaseEnvironmental"
                />
              </template>

              <template v-else-if="activeMenu === 'model-analysis'">
                <ModelAnalysisView
                  :result="modelResult"
                  :loading="modelLoading"
                  :message="modelMessage"
                  :report-message="modelReportMessage"
                  :error-message="modelError"
                  :format-risk-label="riskLabel"
                  @analyze="runModelAnalysis"
                  @export="exportModelMarkdown"
                />
              </template>

              <template v-else-if="activeMenu === 'object-oriented'">
                <ObjectOrientedView
                  :result="ooResult"
                  :loading="ooLoading"
                  :message="ooMessage"
                  :report-message="ooReportMessage"
                  :error-message="ooError"
                  :radar-axes="radarAxes"
                  :radar-series="radarSeries()"
                  :format-risk-label="riskLabel"
                  @analyze="runOoAnalysis"
                  @export="exportOoMarkdown"
                />
              </template>

              <template v-else-if="activeMenu === 'ai'">
                <AiAnalysisView
                  :result="aiResult"
                  :loading="aiLoading"
                  :message="aiMessage"
                  :error-message="aiError"
                  :format-date-value="formatDate"
                  @analyze="runAiAnalysis"
                  @export="exportAiMarkdown"
                />
              </template>

              <template v-else-if="activeMenu === 'estimation'">
                <EstimationView
                  :result="estimationResult"
                  :loading="estimationLoading"
                  :message="estimationMessage"
                  :report-message="estimationReportMessage"
                  :error-message="estimationError"
                  :form="estimationForm"
                  @analyze="runEstimationAnalysis"
                  @export="exportEstimationMarkdown"
                  @update-form="updateEstimationForm"
                />
              </template>
            </section>

            <aside class="operation-panel">
              <section class="operation-section operation-section-primary">
                <div class="operation-panel-header" :class="{ 'operation-panel-header-compact': activeMenu === 'control-flow' }">
                  <p class="eyebrow">分析准备</p>
                  <h2>{{ selectedProjectName() }}</h2>
                  <small>
                    {{ currentProject ? `${currentProject.language} · ${projectFiles(currentProject.id).length} 个文件` : '请先选择项目' }}
                  </small>
                </div>
                <label class="panel-label">
                  <span>当前项目</span>
                  <select v-model="selectedProjectId" @change="handleProjectSelectionChange">
                    <option value="">请选择项目</option>
                    <option v-for="project in projects" :key="project.id" :value="project.id">
                      {{ project.name }}
                    </option>
                  </select>
                </label>
                <p v-if="uploadMessage" class="form-message success">{{ uploadMessage }}</p>
                <p v-if="uploadError" class="form-message error">{{ uploadError }}</p>
                <ProjectFilePanel
                  :project-id="selectedProjectId"
                  :files="projectFiles(selectedProjectId)"
                  :state="fileState(selectedProjectId)"
                  :categories="fileCategories(selectedProjectId)"
                  :paged-files="filteredProjectFiles(selectedProjectId)"
                  :filtered-count="filteredProjectFiles(selectedProjectId).length"
                  :current-page="1"
                  :page-count="1"
                  :show-date="true"
                  :show-refresh="false"
                  :show-upload-area="true"
                  :show-toolbar="false"
                  :show-category-tabs="false"
                  :show-page-info="false"
                  :show-pagination="false"
                  :selected-file-name="selectedFile?.name || ''"
                  :upload-loading="uploadLoading"
                  panel-title="文件"
                  upload-label="上传"
                  upload-hint="zip / java / oom / xml"
                  upload-button-text="保存"
                  item-class="upload-item"
                  empty-message="暂无上传文件"
                  :require-project-selection="true"
                  @file-change="handleFileChange"
                  @upload="submitUpload"
                  @update-state="updateFileState(selectedProjectId, $event)"
                  @change-page="changeFilePage(selectedProjectId, $event)"
                  @remove-file="removeUploadedFile($event)"
                />
              </section>

              <section class="operation-section operation-section-footer">
                <div class="operation-actions">
                  <button type="button" class="secondary-button" @click="exportXml">
                    XML
                  </button>
                  <button type="button" class="secondary-button" @click="exportComprehensiveMarkdown">
                    报告
                  </button>
                </div>
                <p v-if="xmlExportMessage" class="form-message success">{{ xmlExportMessage }}</p>
                <p v-if="xmlExportError" class="form-message error">{{ xmlExportError }}</p>
                <p v-if="comprehensiveReportMessage" class="form-message success">{{ comprehensiveReportMessage }}</p>
                <p v-if="comprehensiveReportError" class="form-message error">{{ comprehensiveReportError }}</p>
              </section>
            </aside>
          </div>
        </template>
      </section>
    </main>
  </div>
</template>
