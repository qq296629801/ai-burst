package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.dto.MagReleaseCreateRequest;
import com.aiburst.mag.service.MagReleaseService;
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
@RequestMapping("/api/mag/projects/{projectId}/releases")
@RequiredArgsConstructor
@Tag(name = "mag-release", description = "MAG 发版归档")
public class MagReleaseController {

    private final MagReleaseService releaseService;

    @GetMapping
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "归档列表")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long projectId) {
        return ApiResult.ok(releaseService.list(projectId, SecurityUtils.currentUserId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('mag:release:archive')")
    @Operation(summary = "创建归档")
    public ApiResult<Map<String, Object>> create(@PathVariable Long projectId,
                                                 @Valid @RequestBody MagReleaseCreateRequest req) {
        return ApiResult.ok(releaseService.create(projectId, req, SecurityUtils.currentUserId()));
    }
}
