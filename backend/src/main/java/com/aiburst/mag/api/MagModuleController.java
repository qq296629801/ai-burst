package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.dto.MagImportBlueprintRequest;
import com.aiburst.mag.dto.MagModuleUpsertRequest;
import com.aiburst.mag.service.MagModuleService;
import com.aiburst.rbac.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/mag/projects/{projectId}/modules")
@RequiredArgsConstructor
@Tag(name = "mag-module", description = "MAG 功能模块树")
public class MagModuleController {

    private final MagModuleService moduleService;

    @GetMapping
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "模块树列表")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long projectId) {
        return ApiResult.ok(moduleService.list(projectId, SecurityUtils.currentUserId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('mag:agent:manage')")
    @Operation(summary = "新建模块")
    public ApiResult<Map<String, Object>> create(@PathVariable Long projectId,
                                                 @Valid @RequestBody MagModuleUpsertRequest req) {
        return ApiResult.ok(moduleService.create(projectId, req, SecurityUtils.currentUserId()));
    }

    @PutMapping("/{moduleId}")
    @PreAuthorize("hasAuthority('mag:agent:manage')")
    @Operation(summary = "更新模块")
    public ApiResult<Map<String, Object>> update(@PathVariable Long moduleId,
                                                 @Valid @RequestBody MagModuleUpsertRequest req) {
        return ApiResult.ok(moduleService.update(moduleId, req, SecurityUtils.currentUserId()));
    }

    @DeleteMapping("/{moduleId}")
    @PreAuthorize("hasAuthority('mag:agent:manage')")
    @Operation(summary = "删除模块")
    public ApiResult<Void> delete(@PathVariable Long moduleId) {
        moduleService.delete(moduleId, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }

    @PostMapping("/import-blueprint")
    @PreAuthorize("hasAuthority('mag:kb:blueprint:import')")
    @Operation(summary = "从归档或知识库条目导入模块蓝图")
    public ApiResult<List<Map<String, Object>>> importBlueprint(@PathVariable Long projectId,
                                                               @Valid @RequestBody MagImportBlueprintRequest req) {
        return ApiResult.ok(moduleService.importBlueprint(projectId, req, SecurityUtils.currentUserId()));
    }
}
