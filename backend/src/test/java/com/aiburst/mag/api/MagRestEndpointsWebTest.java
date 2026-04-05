package com.aiburst.mag.api;

import com.aiburst.dto.PageResult;
import com.aiburst.mag.testsupport.AbstractMagControllersSliceTest;
import com.aiburst.mag.testsupport.WithMockMagUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 覆盖技术方案 §9 主要 MAG 路径（Mock Service，对应《多Agent协作测试用例》API 层）。
 */
class MagRestEndpointsWebTest extends AbstractMagControllersSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockMagUser(authorities = {"mag:project:list", "mag:task:operate"})
    void tasks_listAndBlockAndRequestNext() throws Exception {
        when(taskService.listByProject(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/mag/tasks/10/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"dep missing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/mag/tasks/10/request-next")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list", "mag:task:operate"})
    void tasks_submitComplete() throws Exception {
        doNothing().when(taskService).submitComplete(eq(11L), any(), anyLong());
        mockMvc.perform(post("/api/mag/tasks/11/submit-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list"})
    void tasks_flowEvents() throws Exception {
        when(taskService.listTaskFlowEvents(eq(10L), anyLong()))
                .thenReturn(List.of(Map.of("eventType", "TASK_DISPATCHED", "summary", "派工")));
        mockMvc.perform(get("/api/mag/tasks/10/flow-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].eventType").value("TASK_DISPATCHED"));
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list"})
    void tasks_executionLogs() throws Exception {
        when(taskService.listTaskExecutionLogs(eq(10L), anyLong()))
                .thenReturn(List.of(Map.of("executionOutcome", "SUCCEEDED", "orchestrationRunId", 1L)));
        mockMvc.perform(get("/api/mag/tasks/10/execution-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].executionOutcome").value("SUCCEEDED"));
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list", "mag:task:dispatch"})
    void tasks_dispatchAndPmReassign() throws Exception {
        when(taskService.dispatch(eq(1L), any(), anyLong())).thenReturn(Map.of("id", 8L));
        mockMvc.perform(post("/api/mag/projects/1/tasks/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"派工项\",\"assigneeAgentId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        when(taskService.pmReassign(eq(8L), any(), anyLong())).thenReturn(Map.of("id", 8L));
        mockMvc.perform(post("/api/mag/tasks/8/pm-reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeAgentId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list"})
    void orchestration_runs_list() throws Exception {
        when(orchestrationRunService.listByProject(eq(1L), anyLong(), anyInt()))
                .thenReturn(List.of(Map.of("id", 1L, "status", "SUCCEEDED")));
        mockMvc.perform(get("/api/mag/projects/1/orchestration-runs").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].status").value("SUCCEEDED"));
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list", "mag:agent:manage", "mag:task:operate"})
    void agents_crudAndRun() throws Exception {
        when(agentService.listByProject(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/agents")).andExpect(status().isOk());

        when(agentService.create(eq(1L), any(), anyLong())).thenReturn(Map.of("id", 9L));
        mockMvc.perform(post("/api/mag/projects/1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleType\":\"BACKEND\",\"name\":\"A1\"}"))
                .andExpect(status().isOk());

        when(agentService.requestAgentRun(eq(9L), anyLong(), any())).thenReturn(Map.of("accepted", true));
        mockMvc.perform(post("/api/mag/agents/9/run")).andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list", "mag:req:edit"})
    void requirement_doc_pool_revisions_diff_analyze() throws Exception {
        when(requirementService.getDoc(eq(1L), anyLong())).thenReturn(Map.of("content", "x"));
        mockMvc.perform(get("/api/mag/projects/1/requirement-doc")).andExpect(status().isOk());

        when(requirementService.listRevisions(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/requirement-doc/revisions")).andExpect(status().isOk());

        when(requirementService.diffRevisions(eq(1L), eq(1), eq(2), anyLong()))
                .thenReturn(Map.of("same", true));
        mockMvc.perform(get("/api/mag/projects/1/requirement-doc/diff")
                        .param("version1", "1")
                        .param("version2", "2"))
                .andExpect(status().isOk());

        when(requirementService.analyzeRequirementChange(eq(1L), any(), anyLong()))
                .thenReturn(Map.of("traceId", "t1"));
        mockMvc.perform(post("/api/mag/projects/1/requirement-change/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"scope\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.traceId").value("t1"));

        when(requirementService.listPool(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/requirement-pool")).andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:pool:decide"})
    void requirement_pool_decide() throws Exception {
        mockMvc.perform(post("/api/mag/requirement-pool/5/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE_AS_IS\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:req:edit"})
    void requirement_pool_productClose() throws Exception {
        mockMvc.perform(post("/api/mag/requirement-pool/7/product-close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conclusionSummary\":\"done\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:kb:manage"})
    void kb_entries_crud() throws Exception {
        when(kbService.page(any(), any())).thenReturn(new PageResult<>(0, List.of()));
        mockMvc.perform(get("/api/mag/kb/entries").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk());

        when(kbService.getById(eq(3L))).thenReturn(Map.of("id", 3L, "title", "t"));
        mockMvc.perform(get("/api/mag/kb/entries/3")).andExpect(status().isOk());

        when(kbService.update(eq(3L), any())).thenReturn(Map.of("id", 3L, "title", "t"));
        mockMvc.perform(put("/api/mag/kb/entries/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/mag/kb/entries/3")).andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list", "mag:kb:blueprint:import"})
    void modules_andBlueprint() throws Exception {
        when(moduleService.list(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/modules")).andExpect(status().isOk());

        when(moduleService.importBlueprint(eq(1L), any(), anyLong())).thenReturn(List.of());
        mockMvc.perform(post("/api/mag/projects/1/modules/import-blueprint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"KB\",\"sourceId\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list", "mag:task:operate"})
    void threads_messages_run() throws Exception {
        when(collaborationService.listThreads(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/threads")).andExpect(status().isOk());

        when(collaborationService.requestThreadRun(eq(2L), anyLong())).thenReturn(Map.of("accepted", true));
        mockMvc.perform(post("/api/mag/threads/2/run")).andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list"})
    void alerts_ack() throws Exception {
        when(alertService.listByProject(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/alerts")).andExpect(status().isOk());

        mockMvc.perform(post("/api/mag/alerts/8/ack")).andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:sched:manage"})
    void scheduledJobs() throws Exception {
        when(scheduledJobService.list(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/scheduled-jobs").param("projectId", "1")).andExpect(status().isOk());

        mockMvc.perform(put("/api/mag/scheduled-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobKey\":\"j1\",\"cronExpr\":\"0 0 * * * ?\",\"enabled\":1,\"projectId\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:audit:fetch:view"})
    void fetchAudit() throws Exception {
        when(fetchAuditService.listByProject(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/fetch-audit")).andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list", "mag:agent:manage"})
    void pmAssist_andImprovements() throws Exception {
        when(pmAssistService.list(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/pm-assist")).andExpect(status().isOk());

        when(improvementLogService.list(eq(1L), eq(2L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/agents/2/improvements")).andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:dashboard:view"})
    void dashboardSnapshot() throws Exception {
        when(dashboardService.snapshot(eq(1L), anyLong())).thenReturn(Map.of("taskCountByState", Map.of()));
        mockMvc.perform(get("/api/mag/dashboard/snapshot").param("projectId", "1")).andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:pool:decide"})
    void todosPage() throws Exception {
        when(todoService.page(anyLong(), any())).thenReturn(new PageResult<>(0, List.of()));
        mockMvc.perform(get("/api/mag/todos").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list"})
    void releases_list() throws Exception {
        when(releaseService.list(eq(1L), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/1/releases")).andExpect(status().isOk());
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list"})
    void work_outputs_aggregated() throws Exception {
        when(workOutputService.listAggregated(eq(1L), anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Map.of("items", List.of(Map.of("kind", "IMPROVEMENT", "summary", "plan"))));
        mockMvc.perform(get("/api/mag/projects/1/work-outputs").param("improvementLimit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].kind").value("IMPROVEMENT"));
    }
}
