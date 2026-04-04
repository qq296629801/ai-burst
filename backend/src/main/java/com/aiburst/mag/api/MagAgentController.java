package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.dto.MagAgentUpsertRequest;
import com.aiburst.mag.service.MagAgentService;
import com.aiburst.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mag")
@RequiredArgsConstructor
@Tag(name = "mag-agent", description = "MAG Agent")
public class MagAgentController {

    private final MagAgentService agentService;

    @GetMapping("/projects/{projectId}/agents")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "Agent 列表")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long projectId) {
        return ApiResult.ok(agentService.listByProject(projectId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/projects/{projectId}/agents")
    @PreAuthorize("hasAuthority('mag:agent:manage')")
    @Operation(summary = "创建 Agent")
    public ApiResult<Map<String, Object>> create(@PathVariable Long projectId,
                                                 @Valid @RequestBody MagAgentUpsertRequest req) {
        return ApiResult.ok(agentService.create(projectId, req, SecurityUtils.currentUserId()));
    }

    @PutMapping("/agents/{agentId}")
    @PreAuthorize("hasAuthority('mag:agent:manage')")
    @Operation(summary = "更新 Agent")
    public ApiResult<Map<String, Object>> update(@PathVariable Long agentId,
                                                   @Valid @RequestBody MagAgentUpsertRequest req) {
        return ApiResult.ok(agentService.update(agentId, req, SecurityUtils.currentUserId()));
    }

    @PostMapping("/agents/{agentId}/run")
    @PreAuthorize("hasAuthority('mag:task:operate')")
    @Operation(summary = "触发 Agent 编排（占位，由 Worker 消费）")
    public ApiResult<Map<String, Object>> runAgent(@PathVariable Long agentId) {
        return ApiResult.ok(agentService.requestAgentRun(agentId, SecurityUtils.currentUserId()));
    }
}
