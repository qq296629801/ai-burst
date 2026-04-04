package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.dto.PageResult;
import com.aiburst.mag.dto.MagMemberAddRequest;
import com.aiburst.mag.dto.MagPageQuery;
import com.aiburst.mag.dto.MagProjectCreateRequest;
import com.aiburst.mag.dto.MagProjectUpdateRequest;
import com.aiburst.mag.service.MagMemberService;
import com.aiburst.mag.service.MagProjectService;
import com.aiburst.security.SecurityUtils;
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
@RequestMapping("/api/mag/projects")
@RequiredArgsConstructor
@Tag(name = "mag-project", description = "MAG 项目与成员")
public class MagProjectController {

    private final MagProjectService projectService;
    private final MagMemberService memberService;

    @GetMapping
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "项目列表")
    public ApiResult<PageResult<Map<String, Object>>> page(@Valid MagPageQuery q) {
        return ApiResult.ok(projectService.page(q));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('mag:project:manage')")
    @Operation(summary = "创建项目")
    public ApiResult<Map<String, Object>> create(@Valid @RequestBody MagProjectCreateRequest req) {
        return ApiResult.ok(projectService.create(req));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "项目详情")
    public ApiResult<Map<String, Object>> get(@PathVariable Long projectId) {
        return ApiResult.ok(projectService.get(projectId, SecurityUtils.currentUserId()));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("hasAuthority('mag:project:manage')")
    @Operation(summary = "更新项目")
    public ApiResult<Void> update(@PathVariable Long projectId, @Valid @RequestBody MagProjectUpdateRequest req) {
        projectService.update(projectId, req);
        return ApiResult.ok();
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasAuthority('mag:project:manage')")
    @Operation(summary = "归档项目")
    public ApiResult<Void> archive(@PathVariable Long projectId) {
        projectService.archive(projectId);
        return ApiResult.ok();
    }

    @GetMapping("/{projectId}/members")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "成员列表")
    public ApiResult<List<Map<String, Object>>> members(@PathVariable Long projectId) {
        return ApiResult.ok(memberService.list(projectId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/{projectId}/members")
    @PreAuthorize("hasAuthority('mag:project:manage')")
    @Operation(summary = "添加成员")
    public ApiResult<Void> addMember(@PathVariable Long projectId, @Valid @RequestBody MagMemberAddRequest req) {
        memberService.add(projectId, req, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("hasAuthority('mag:project:manage')")
    @Operation(summary = "移除成员")
    public ApiResult<Void> removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        memberService.remove(projectId, userId, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }
}
