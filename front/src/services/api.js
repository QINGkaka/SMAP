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

export async function deleteProject(projectId) {
  return request(`/projects/${projectId}`, {
    method: 'DELETE'
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
