package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.dto.MagMessagePostRequest;
import com.aiburst.mag.dto.MagThreadCreateRequest;
import com.aiburst.mag.service.MagCollaborationService;
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
@Tag(name = "mag-collab", description = "MAG 沟通线程")
public class MagCollaborationRestController {

    private final MagCollaborationService collaborationService;

    @GetMapping("/projects/{projectId}/threads")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "线程列表")
    public ApiResult<List<Map<String, Object>>> listThreads(@PathVariable Long projectId) {
        return ApiResult.ok(collaborationService.listThreads(projectId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/projects/{projectId}/threads")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "创建线程")
    public ApiResult<Map<String, Object>> createThread(@PathVariable Long projectId,
                                                       @Valid @RequestBody MagThreadCreateRequest req) {
        return ApiResult.ok(collaborationService.createThread(projectId, req, SecurityUtils.currentUserId()));
    }

    @GetMapping("/threads/{threadId}/messages")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "消息列表")
    public ApiResult<List<Map<String, Object>>> messages(@PathVariable Long threadId) {
        return ApiResult.ok(collaborationService.listMessages(threadId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/threads/{threadId}/messages")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "发送消息")
    public ApiResult<Map<String, Object>> postMessage(@PathVariable Long threadId,
                                                      @Valid @RequestBody MagMessagePostRequest req) {
        return ApiResult.ok(collaborationService.postMessage(threadId, req, SecurityUtils.currentUserId()));
    }

    @PostMapping("/threads/{threadId}/run")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "触发线程 Agent 编排（占位，由 Worker 消费）")
    public ApiResult<Map<String, Object>> runThread(@PathVariable Long threadId) {
        return ApiResult.ok(collaborationService.requestThreadRun(threadId, SecurityUtils.currentUserId()));
    }
}
