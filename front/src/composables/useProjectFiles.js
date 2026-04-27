import { ref } from 'vue'
import {
  deleteProjectFile,
  fetchProjectFiles,
  uploadProjectFile
} from '../services/api'

export function useProjectFiles({
  selectedProjectId,
  markMetricResultsStale,
  formatDate,
  formatFileSize
}) {
  const uploadedFiles = ref([])
  const selectedFiles = ref([])
  const projectFileMap = ref({})
  const managementSelectedFiles = ref({})
  const managementUploadLoading = ref({})
  const fileListState = ref({})
  const managementMessage = ref('')
  const managementError = ref('')
  const uploadLoading = ref(false)
  const uploadMessage = ref('')
  const uploadError = ref('')

  function invalidateCurrentProjectMetrics(projectId, message) {
    if (projectId && projectId === selectedProjectId.value) {
      markMetricResultsStale(message)
    }
  }

  async function loadAllProjectFiles(projectList = []) {
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

  function handleFileChange(event) {
    selectedFiles.value = Array.from(event.target.files || [])
    uploadMessage.value = ''
    uploadError.value = ''
  }

  function handleManagementFileChange(projectId, event) {
    const files = Array.from(event.target.files || [])
    managementSelectedFiles.value = {
      ...managementSelectedFiles.value,
      [projectId]: files
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
    if (selectedFiles.value.length === 0) {
      uploadError.value = '请选择要上传的文件'
      return
    }
    uploadLoading.value = true
    try {
      const result = await uploadProjectFile(selectedProjectId.value, selectedFiles.value)
      const names = result.data.map(file => file.originalName)
      uploadMessage.value = names.length === 1
        ? `已上传：${names[0]}`
        : `已上传 ${names.length} 个文件：${names.join('、')}`
      selectedFiles.value = []
      await loadProjectFiles()
      await loadFilesForProject(selectedProjectId.value)
      invalidateCurrentProjectMetrics(selectedProjectId.value, '文件已更新，可按需重新执行当前度量。')
    } catch (error) {
      uploadError.value = error.message
    } finally {
      uploadLoading.value = false
    }
  }

  async function submitManagementUpload(project) {
    managementError.value = ''
    managementMessage.value = ''
    const files = managementSelectedFiles.value[project.id] || []
    if (files.length === 0) {
      managementError.value = `请先为项目“${project.name}”选择文件`
      return
    }
    managementUploadLoading.value = {
      ...managementUploadLoading.value,
      [project.id]: true
    }
    try {
      const result = await uploadProjectFile(project.id, files)
      const names = result.data.map(file => file.originalName)
      managementMessage.value = names.length === 1
        ? `已上传到“${project.name}”：${names[0]}`
        : `已上传到“${project.name}”共 ${names.length} 个文件`
      managementSelectedFiles.value = {
        ...managementSelectedFiles.value,
        [project.id]: []
      }
      await loadFilesForProject(project.id)
      invalidateCurrentProjectMetrics(project.id, '文件已更新，可按需重新执行当前度量。')
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
      invalidateCurrentProjectMetrics(selectedProjectId.value, '上传文件已变化，可按需重新执行当前度量。')
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
      invalidateCurrentProjectMetrics(project.id, '上传文件已变化，可按需重新执行当前度量。')
    } catch (error) {
      managementError.value = error.message
    }
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

  function pageCount(projectId) {
    const state = fileState(projectId)
    return Math.max(1, Math.ceil(filteredProjectFiles(projectId).length / state.pageSize))
  }

  function currentFilePage(projectId) {
    return Math.min(Math.max(fileState(projectId).page, 1), pageCount(projectId))
  }

  function pagedProjectFiles(projectId) {
    const state = fileState(projectId)
    const filtered = filteredProjectFiles(projectId)
    const safePage = currentFilePage(projectId)
    const start = (safePage - 1) * state.pageSize
    return filtered.slice(start, start + state.pageSize)
  }

  function changeFilePage(projectId, nextPage) {
    const bounded = Math.min(Math.max(nextPage, 1), pageCount(projectId))
    updateFileState(projectId, { page: bounded })
  }

  function displayProjectFiles(projectId) {
    return pagedProjectFiles(projectId).map(file => ({
      ...file,
      sizeLabel: formatFileSize(file.size),
      uploadedAtLabel: formatDate(file.uploadedAt)
    }))
  }

  return {
    uploadedFiles,
    selectedFiles,
    projectFileMap,
    managementSelectedFiles,
    managementUploadLoading,
    managementMessage,
    managementError,
    uploadLoading,
    uploadMessage,
    uploadError,
    loadAllProjectFiles,
    loadFilesForProject,
    loadProjectFiles,
    handleFileChange,
    handleManagementFileChange,
    submitUpload,
    submitManagementUpload,
    removeUploadedFile,
    removeManagementFile,
    projectFiles,
    displayProjectFiles,
    fileState,
    updateFileState,
    fileCategories,
    filteredProjectFiles,
    pagedProjectFiles,
    pageCount,
    currentFilePage,
    changeFilePage
  }
}
