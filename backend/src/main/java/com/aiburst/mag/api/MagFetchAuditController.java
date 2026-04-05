package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.service.MagFetchAuditService;
import com.aiburst.rbac.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mag/projects/{projectId}/fetch-audit")
@RequiredArgsConstructor
@Tag(name = "mag-fetch-audit", description = "MAG 外网检索审计")
public class MagFetchAuditController {

    private final MagFetchAuditService fetchAuditService;

    @GetMapping
    @PreAuthorize("hasAuthority('mag:audit:fetch:view')")
    @Operation(summary = "项目维度的外网抓取审计记录")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long projectId) {
        return ApiResult.ok(fetchAuditService.listByProject(projectId, SecurityUtils.currentUserId()));
    }
}
