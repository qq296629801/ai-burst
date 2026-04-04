import http from './http'

export function magProjectsPage(params) {
  return http.get('/mag/projects', { params })
}

export function magCreateProject(data) {
  return http.post('/mag/projects', data)
}

export function magGetProject(projectId) {
  return http.get(`/mag/projects/${projectId}`)
}

export function magUpdateProject(projectId, data) {
  return http.put(`/mag/projects/${projectId}`, data)
}

export function magArchiveProject(projectId) {
  return http.delete(`/mag/projects/${projectId}`)
}

export function magListMembers(projectId) {
  return http.get(`/mag/projects/${projectId}/members`)
}

export function magAddMember(projectId, data) {
  return http.post(`/mag/projects/${projectId}/members`, data)
}

export function magRemoveMember(projectId, userId) {
  return http.delete(`/mag/projects/${projectId}/members/${userId}`)
}

export function magListAgents(projectId) {
  return http.get(`/mag/projects/${projectId}/agents`)
}

export function magCreateAgent(projectId, data) {
  return http.post(`/mag/projects/${projectId}/agents`, data)
}

export function magUpdateAgent(agentId, data) {
  return http.put(`/mag/agents/${agentId}`, data)
}

export function magListTasks(projectId) {
  return http.get(`/mag/projects/${projectId}/tasks`)
}

export function magCreateTask(projectId, data) {
  return http.post(`/mag/projects/${projectId}/tasks`, data)
}

export function magStartTask(taskId) {
  return http.post(`/mag/tasks/${taskId}/start`)
}

export function magSubmitComplete(taskId, data) {
  return http.post(`/mag/tasks/${taskId}/submit-complete`, data || {})
}

export function magListVerifications(taskId) {
  return http.get(`/mag/tasks/${taskId}/verifications`)
}

export function magListThreads(projectId) {
  return http.get(`/mag/projects/${projectId}/threads`)
}

export function magCreateThread(projectId, data) {
  return http.post(`/mag/projects/${projectId}/threads`, data)
}

export function magListMessages(threadId) {
  return http.get(`/mag/threads/${threadId}/messages`)
}

export function magPostMessage(threadId, data) {
  return http.post(`/mag/threads/${threadId}/messages`, data)
}

export function magGetRequirementDoc(projectId) {
  return http.get(`/mag/projects/${projectId}/requirement-doc`)
}

export function magSaveRequirementDoc(projectId, data) {
  return http.put(`/mag/projects/${projectId}/requirement-doc`, data)
}

export function magListRequirementPool(projectId) {
  return http.get(`/mag/projects/${projectId}/requirement-pool`)
}

export function magCreatePoolItem(projectId, data) {
  return http.post(`/mag/projects/${projectId}/requirement-pool`, data)
}

export function magDecidePoolItem(poolItemId, data) {
  return http.post(`/mag/requirement-pool/${poolItemId}/decide`, data)
}

export function magKbPage(params) {
  return http.get('/mag/kb/entries', { params })
}

export function magKbCreate(data) {
  return http.post('/mag/kb/entries', data)
}

export function magListReleases(projectId) {
  return http.get(`/mag/projects/${projectId}/releases`)
}

export function magCreateRelease(projectId, data) {
  return http.post(`/mag/projects/${projectId}/releases`, data)
}

export function magDashboardSnapshot(projectId) {
  return http.get('/mag/dashboard/snapshot', { params: { projectId } })
}

export function magTodosPage(params) {
  return http.get('/mag/todos', { params })
}

export function magListModules(projectId) {
  return http.get(`/mag/projects/${projectId}/modules`)
}

export function magCreateModule(projectId, data) {
  return http.post(`/mag/projects/${projectId}/modules`, data)
}

export function magUpdateModule(projectId, moduleId, data) {
  return http.put(`/mag/projects/${projectId}/modules/${moduleId}`, data)
}

export function magDeleteModule(projectId, moduleId) {
  return http.delete(`/mag/projects/${projectId}/modules/${moduleId}`)
}

export function magImportBlueprint(projectId, data) {
  return http.post(`/mag/projects/${projectId}/modules/import-blueprint`, data)
}

export function magTaskBlock(taskId, data) {
  return http.post(`/mag/tasks/${taskId}/block`, data)
}

export function magTaskRequestNext(taskId, data) {
  return http.post(`/mag/tasks/${taskId}/request-next`, data)
}

export function magListPmAssist(projectId) {
  return http.get(`/mag/projects/${projectId}/pm-assist`)
}

export function magCreatePmAssist(projectId, data) {
  return http.post(`/mag/projects/${projectId}/pm-assist`, data)
}

export function magListImprovements(projectId, agentId) {
  return http.get(`/mag/projects/${projectId}/agents/${agentId}/improvements`)
}

export function magAppendImprovement(projectId, agentId, data) {
  return http.post(`/mag/projects/${projectId}/agents/${agentId}/improvements`, data)
}

export function magListAlerts(projectId) {
  return http.get(`/mag/projects/${projectId}/alerts`)
}

export function magAckAlert(alertId) {
  return http.post(`/mag/alerts/${alertId}/ack`)
}

export function magListScheduledJobs(params) {
  return http.get('/mag/scheduled-jobs', { params })
}

export function magUpsertScheduledJob(data) {
  return http.put('/mag/scheduled-jobs', data)
}

export function magListReqRevisions(projectId) {
  return http.get(`/mag/projects/${projectId}/requirement-doc/revisions`)
}

export function magReqDiff(projectId, version1, version2) {
  return http.get(`/mag/projects/${projectId}/requirement-doc/diff`, {
    params: { version1, version2 },
  })
}

export function magReqAnalyzeChange(projectId, data) {
  return http.post(`/mag/projects/${projectId}/requirement-change/analyze`, data)
}

export function magProductClosePoolItem(poolItemId, data) {
  return http.post(`/mag/requirement-pool/${poolItemId}/product-close`, data)
}

export function magKbGet(id) {
  return http.get(`/mag/kb/entries/${id}`)
}

export function magKbUpdate(id, data) {
  return http.put(`/mag/kb/entries/${id}`, data)
}

export function magKbDelete(id) {
  return http.delete(`/mag/kb/entries/${id}`)
}

export function magListFetchAudit(projectId) {
  return http.get(`/mag/projects/${projectId}/fetch-audit`)
}

export function magRunThread(threadId) {
  return http.post(`/mag/threads/${threadId}/run`)
}

export function magRunAgent(agentId) {
  return http.post(`/mag/agents/${agentId}/run`)
}
