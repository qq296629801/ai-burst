package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.service.MagDashboardService;
import com.aiburst.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mag/dashboard")
@RequiredArgsConstructor
@Tag(name = "mag-dashboard", description = "MAG 大屏")
public class MagDashboardController {

    private final MagDashboardService dashboardService;

    @GetMapping("/snapshot")
    @PreAuthorize("hasAuthority('mag:dashboard:view')")
    @Operation(summary = "项目大屏快照")
    public ApiResult<Map<String, Object>> snapshot(@RequestParam Long projectId) {
        return ApiResult.ok(dashboardService.snapshot(projectId, SecurityUtils.currentUserId()));
    }

    @GetMapping("/org-snapshot")
    @PreAuthorize("hasAuthority('mag:dashboard:org')")
    @Operation(summary = "组织级大屏占位")
    public ApiResult<Map<String, Object>> orgSnapshot() {
        Map<String, Object> m = new HashMap<>();
        m.put("projects", java.util.List.of());
        return ApiResult.ok(m);
    }
}
