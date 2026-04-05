package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.service.MagWorkOutputService;
import com.aiburst.rbac.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mag")
@RequiredArgsConstructor
@Tag(name = "mag-work-output", description = "MAG Agent 产出物聚合")
public class MagWorkOutputController {

    private final MagWorkOutputService workOutputService;

    @GetMapping("/projects/{projectId}/work-outputs")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(
            summary = "项目产出物聚合视图",
            description =
                    "合并 mag_agent_improvement_log（开发/测试等工具落库）、需求池项、需求文档版本预览；按时间倒序。")
    public ApiResult<Map<String, Object>> list(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "300") int improvementLimit,
            @RequestParam(defaultValue = "200") int poolLimit,
            @RequestParam(defaultValue = "30") int revisionLimit) {
        return ApiResult.ok(
                workOutputService.listAggregated(
                        projectId,
                        SecurityUtils.currentUserId(),
                        improvementLimit,
                        poolLimit,
                        revisionLimit));
    }
}
