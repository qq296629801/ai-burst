package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.dto.MagSubmitCompleteRequest;
import com.aiburst.mag.dto.MagTaskBlockRequest;
import com.aiburst.mag.dto.MagTaskCreateRequest;
import com.aiburst.mag.dto.MagTaskDispatchRequest;
import com.aiburst.mag.dto.MagTaskPmReassignRequest;
import com.aiburst.mag.dto.MagTaskRequestNextRequest;
import com.aiburst.mag.dto.MagTaskVerifyDecisionRequest;
import com.aiburst.mag.service.MagTaskService;
import com.aiburst.rbac.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mag")
@RequiredArgsConstructor
@Tag(name = "mag-task", description = "MAG 任务")
public class MagTaskController {

    private final MagTaskService taskService;

    @GetMapping("/projects/{projectId}/tasks")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "任务列表")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long projectId) {
        return ApiResult.ok(taskService.listByProject(projectId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/projects/{projectId}/tasks")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "创建任务（可空执行人；项目经理派工请用 dispatch）")
    public ApiResult<Map<String, Object>> create(@PathVariable Long projectId,
                                                 @Valid @RequestBody MagTaskCreateRequest req) {
        return ApiResult.ok(taskService.create(projectId, req, SecurityUtils.currentUserId()));
    }

    @PostMapping("/projects/{projectId}/tasks/dispatch")
    @PreAuthorize("hasAuthority('mag:task:dispatch')")
    @Operation(summary = "项目经理派工：创建任务并指定执行 Agent")
    public ApiResult<Map<String, Object>> dispatch(@PathVariable Long projectId,
                                                     @Valid @RequestBody MagTaskDispatchRequest req) {
        return ApiResult.ok(taskService.dispatch(projectId, req, SecurityUtils.currentUserId()));
    }

    @PostMapping("/tasks/{taskId}/pm-reassign")
    @PreAuthorize("hasAuthority('mag:task:dispatch')")
    @Operation(summary = "项目经理改派执行 Agent")
    public ApiResult<Map<String, Object>> pmReassign(@PathVariable Long taskId,
                                                     @Valid @RequestBody MagTaskPmReassignRequest req) {
        return ApiResult.ok(taskService.pmReassign(taskId, req, SecurityUtils.currentUserId()));
    }

    @PostMapping("/tasks/{taskId}/start")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "任务开始执行 PENDING→IN_PROGRESS")
    public ApiResult<Void> start(@PathVariable Long taskId) {
        taskService.start(taskId, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }

    @PostMapping("/tasks/{taskId}/submit-complete")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "申报完成")
    public ApiResult<Void> submitComplete(@PathVariable Long taskId,
                                          @RequestBody(required = false) MagSubmitCompleteRequest req) {
        if (req == null) {
            req = new MagSubmitCompleteRequest();
        }
        taskService.submitComplete(taskId, req, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }

    @PostMapping("/tasks/{taskId}/begin-verify")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "待核查 → 核查中（未自动进入核查时可手工调用）")
    public ApiResult<Void> beginVerify(@PathVariable Long taskId) {
        taskService.beginVerify(taskId, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }

    @PostMapping("/tasks/{taskId}/verify-decision")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "提交核查结论：PASS→DONE，FAIL→IN_PROGRESS；写入 mag_task_verification")
    public ApiResult<Void> verifyDecision(
            @PathVariable Long taskId, @Valid @RequestBody MagTaskVerifyDecisionRequest req) {
        taskService.submitVerificationDecision(taskId, req, SecurityUtils.currentUserId(), false);
        return ApiResult.ok();
    }

    @PostMapping("/tasks/{taskId}/block")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "结构化阻塞（写入 block 字段与协调线程消息）")
    public ApiResult<Void> block(@PathVariable Long taskId, @Valid @RequestBody MagTaskBlockRequest req) {
        taskService.block(taskId, req, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }

    @PostMapping("/tasks/{taskId}/request-next")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "子 Agent 要活（协调线程消息）")
    public ApiResult<Void> requestNext(@PathVariable Long taskId,
                                       @Valid @RequestBody MagTaskRequestNextRequest req) {
        taskService.requestNext(taskId, req, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }

    @GetMapping("/tasks/{taskId}/verifications")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "核查历史")
    public ApiResult<List<Map<String, Object>>> verifications(@PathVariable Long taskId) {
        return ApiResult.ok(taskService.listVerifications(taskId, SecurityUtils.currentUserId()));
    }

    @GetMapping("/tasks/{taskId}/flow-events")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "任务流程事件（时间线/可视化）")
    public ApiResult<List<Map<String, Object>>> taskFlowEvents(@PathVariable Long taskId) {
        return ApiResult.ok(taskService.listTaskFlowEvents(taskId, SecurityUtils.currentUserId()));
    }

    @GetMapping("/tasks/{taskId}/execution-logs")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "任务 Agent 编排执行记录（成功/失败/触发被拒）")
    public ApiResult<List<Map<String, Object>>> taskExecutionLogs(@PathVariable Long taskId) {
        return ApiResult.ok(taskService.listTaskExecutionLogs(taskId, SecurityUtils.currentUserId()));
    }
}
