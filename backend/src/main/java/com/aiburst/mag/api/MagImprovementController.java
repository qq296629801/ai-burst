package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.dto.MagImprovementCreateRequest;
import com.aiburst.mag.service.MagImprovementLogService;
import com.aiburst.security.SecurityUtils;
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
@RequestMapping("/api/mag/projects/{projectId}/agents/{agentId}/improvements")
@RequiredArgsConstructor
@Tag(name = "mag-improvement", description = "MAG Agent 改进日志")
public class MagImprovementController {

    private final MagImprovementLogService improvementLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "改进日志列表")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long projectId, @PathVariable Long agentId) {
        return ApiResult.ok(improvementLogService.list(projectId, agentId, SecurityUtils.currentUserId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('mag:agent:manage')")
    @Operation(summary = "追加改进记录")
    public ApiResult<Map<String, Object>> append(@PathVariable Long projectId,
                                                 @PathVariable Long agentId,
                                                 @Valid @RequestBody MagImprovementCreateRequest req) {
        return ApiResult.ok(improvementLogService.append(projectId, agentId, req, SecurityUtils.currentUserId()));
    }
}
