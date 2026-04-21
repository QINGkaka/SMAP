const API_PREFIX = '/api'

async function request(path, options = {}) {
  const headers = options.body instanceof FormData
    ? options.headers || {}
    : {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      }
  const response = await fetch(`${API_PREFIX}${path}`, {
    headers,
    ...options
  })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
  const result = await response.json()
  if (!result.success) {
    throw new Error(result.message || '请求失败')
  }
  return result
}

export async function fetchHealth() {
  return request('/health')
}

export async function fetchProjects() {
  return request('/projects')
}

export async function createProject(payload) {
  return request('/projects', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function updateProject(projectId, payload) {
  return request(`/projects/${projectId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export async function deleteProject(projectId) {
  return request(`/projects/${projectId}`, {
    method: 'DELETE'
  })
}

export async function fetchProjectTasks(projectId) {
  return request(`/projects/${projectId}/tasks`)
}

export async function fetchThresholds() {
  return request('/config/thresholds')
}

export async function saveThresholds(payload) {
  return request('/config/thresholds', {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export async function fetchProjectFiles(projectId) {
  return request(`/projects/${projectId}/files`)
}

export async function uploadProjectFile(projectId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request(`/projects/${projectId}/files`, {
    method: 'POST',
    body: formData
  })
}

export async function deleteProjectFile(projectId, fileId) {
  return request(`/projects/${projectId}/files/${fileId}`, {
    method: 'DELETE'
  })
}

export async function analyzeProjectLoc(projectId) {
  return request(`/projects/${projectId}/loc/analyze`, {
    method: 'POST'
  })
}

export async function fetchLatestLocResult(projectId) {
  return request(`/projects/${projectId}/loc/latest`)
}

export async function exportLocReport(projectId) {
  return request(`/projects/${projectId}/loc/report`)
}

export async function analyzeProjectComplexity(projectId) {
  return request(`/projects/${projectId}/complexity/analyze`, {
    method: 'POST'
  })
}

export async function fetchLatestComplexityResult(projectId) {
  return request(`/projects/${projectId}/complexity/latest`)
}

export async function exportComplexityReport(projectId) {
  return request(`/projects/${projectId}/complexity/report`)
}

export async function analyzeProjectOo(projectId) {
  return request(`/projects/${projectId}/oo/analyze`, {
    method: 'POST'
  })
}

export async function fetchLatestOoResult(projectId) {
  return request(`/projects/${projectId}/oo/latest`)
}

export async function exportOoReport(projectId) {
  return request(`/projects/${projectId}/oo/report`)
}

export async function analyzeProjectEstimation(projectId, payload) {
  return request(`/projects/${projectId}/estimation/analyze`, {
    method: 'POST',
    body: JSON.stringify(payload || {})
  })
}

export async function fetchLatestEstimationResult(projectId) {
  return request(`/projects/${projectId}/estimation/latest`)
}

export async function exportEstimationReport(projectId) {
  return request(`/projects/${projectId}/estimation/report`)
}

export async function exportProjectXml(projectId) {
  return request(`/projects/${projectId}/export/xml`)
}

export async function analyzeProjectAi(projectId) {
  return request(`/projects/${projectId}/ai-analysis/analyze`, {
    method: 'POST'
  })
}

export async function fetchLatestAiResult(projectId) {
  return request(`/projects/${projectId}/ai-analysis/latest`)
}

export async function exportComprehensiveReport(projectId) {
  return request(`/projects/${projectId}/report/comprehensive`)
}

export async function analyzeProjectFunctionPoint(projectId, payload) {
  return request(`/projects/${projectId}/function-point/analyze`, {
    method: 'POST',
    body: JSON.stringify(payload || {})
  })
}

export async function fetchLatestFunctionPointResult(projectId) {
  return request(`/projects/${projectId}/function-point/latest`)
}

export async function exportFunctionPointReport(projectId) {
  return request(`/projects/${projectId}/function-point/report`)
}

export async function analyzeProjectUseCasePoint(projectId, payload) {
  return request(`/projects/${projectId}/use-case-point/analyze`, {
    method: 'POST',
    body: JSON.stringify(payload || {})
  })
}

export async function fetchLatestUseCasePointResult(projectId) {
  return request(`/projects/${projectId}/use-case-point/latest`)
}

export async function exportUseCasePointReport(projectId) {
  return request(`/projects/${projectId}/use-case-point/report`)
}

export async function analyzeProjectModel(projectId) {
  return request(`/projects/${projectId}/model-analysis/analyze`, {
    method: 'POST'
  })
}

export async function fetchLatestModelResult(projectId) {
  return request(`/projects/${projectId}/model-analysis/latest`)
}

export async function exportModelReport(projectId) {
  return request(`/projects/${projectId}/model-analysis/report`)
}
