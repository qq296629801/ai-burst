package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.service.MagOrchestrationRunService;
import com.aiburst.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mag")
@RequiredArgsConstructor
@Tag(name = "mag-orchestration-run", description = "MAG 编排执行记录")
public class MagOrchestrationRunController {

    private final MagOrchestrationRunService orchestrationRunService;

    @GetMapping("/projects/{projectId}/orchestration-runs")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "编排执行记录（Agent/线程触发 run）")
    public ApiResult<List<Map<String, Object>>> list(
            @PathVariable Long projectId, @RequestParam(defaultValue = "50") int limit) {
        return ApiResult.ok(
                orchestrationRunService.listByProject(projectId, SecurityUtils.currentUserId(), limit));
    }
}
