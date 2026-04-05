import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./http', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ code: 0, data: null })),
    post: vi.fn(() => Promise.resolve({ code: 0, data: null })),
    put: vi.fn(() => Promise.resolve({ code: 0, data: null })),
    delete: vi.fn(() => Promise.resolve({ code: 0, data: null })),
  },
}))

import http from './http'
import * as mag from './mag'

describe('mag API（路径与方法与 OpenAPI / 测试用例对齐）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('项目分页与创建', async () => {
    await mag.magProjectsPage({ pageNum: 1, pageSize: 10 })
    expect(http.get).toHaveBeenCalledWith('/mag/projects', { params: { pageNum: 1, pageSize: 10 } })

    await mag.magCreateProject({ name: 'P' })
    expect(http.post).toHaveBeenCalledWith('/mag/projects', { name: 'P' })
  })

  it('成员与任务', async () => {
    await mag.magListMembers(3)
    expect(http.get).toHaveBeenCalledWith('/mag/projects/3/members')

    await mag.magTaskBlock(9, { reason: 'x' })
    expect(http.post).toHaveBeenCalledWith('/mag/tasks/9/block', { reason: 'x' })

    await mag.magTaskRequestNext(9, { agentId: 2 })
    expect(http.post).toHaveBeenCalledWith('/mag/tasks/9/request-next', { agentId: 2 })
  })

  it('需求文档与变更分析', async () => {
    await mag.magGetRequirementDoc(1)
    expect(http.get).toHaveBeenCalledWith('/mag/projects/1/requirement-doc')

    await mag.magReqDiff(1, 1, 2)
    expect(http.get).toHaveBeenCalledWith('/mag/projects/1/requirement-doc/diff', {
      params: { version1: 1, version2: 2 },
    })

    await mag.magReqAnalyzeChange(1, { changeSummary: 'scope' })
    expect(http.post).toHaveBeenCalledWith('/mag/projects/1/requirement-change/analyze', { changeSummary: 'scope' })
  })

  it('知识库 CRUD', async () => {
    await mag.magKbPage({ pageNum: 1, pageSize: 20 })
    expect(http.get).toHaveBeenCalledWith('/mag/kb/entries', { params: { pageNum: 1, pageSize: 20 } })

    await mag.magKbUpdate(3, { title: 't', body: 'b' })
    expect(http.put).toHaveBeenCalledWith('/mag/kb/entries/3', { title: 't', body: 'b' })

    await mag.magKbDelete(3)
    expect(http.delete).toHaveBeenCalledWith('/mag/kb/entries/3')
  })

  it('模块蓝图、协作线程与编排触发', async () => {
    await mag.magImportBlueprint(1, { sourceType: 'KB', sourceId: 10 })
    expect(http.post).toHaveBeenCalledWith('/mag/projects/1/modules/import-blueprint', {
      sourceType: 'KB',
      sourceId: 10,
    })

    await mag.magRunThread(7)
    expect(http.post).toHaveBeenCalledWith('/mag/threads/7/run')

    await mag.magRunAgent(4)
    expect(http.post).toHaveBeenCalledWith('/mag/agents/4/run', {})
  })

  it('任务申报完成 submit-complete', async () => {
    await mag.magSubmitComplete(11, { rowVersion: 2 })
    expect(http.post).toHaveBeenCalledWith('/mag/tasks/11/submit-complete', { rowVersion: 2 })
  })

  it('大屏、定时任务与编排', async () => {
    await mag.magDashboardSnapshot(12)
    expect(http.get).toHaveBeenCalledWith('/mag/dashboard/snapshot', { params: { projectId: 12 } })

    await mag.magListOrchestrationRuns(3, { limit: 30 })
    expect(http.get).toHaveBeenCalledWith('/mag/projects/3/orchestration-runs', { params: { limit: 30 } })

    await mag.magUpsertScheduledJob({ jobKey: 'j', cronExpr: '0 0 * * * ?', enabled: 1 })
    expect(http.put).toHaveBeenCalledWith('/mag/scheduled-jobs', {
      jobKey: 'j',
      cronExpr: '0 0 * * * ?',
      enabled: 1,
    })

  })
})
