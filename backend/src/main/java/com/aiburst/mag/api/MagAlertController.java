package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.service.MagAlertService;
import com.aiburst.rbac.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mag")
@RequiredArgsConstructor
@Tag(name = "mag-alert", description = "MAG 告警")
public class MagAlertController {

    private final MagAlertService alertService;

    @GetMapping("/projects/{projectId}/alerts")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "项目告警列表")
    public ApiResult<List<Map<String, Object>>> listByProject(@PathVariable Long projectId) {
        return ApiResult.ok(alertService.listByProject(projectId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/alerts/{alertId}/ack")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "确认告警已读")
    public ApiResult<Void> acknowledge(@PathVariable Long alertId) {
        alertService.acknowledge(alertId, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }
}
