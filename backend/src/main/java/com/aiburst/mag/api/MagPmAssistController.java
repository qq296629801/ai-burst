package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.dto.MagPmAssistCreateRequest;
import com.aiburst.mag.service.MagPmAssistService;
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
@RequestMapping("/api/mag/projects/{projectId}/pm-assist")
@RequiredArgsConstructor
@Tag(name = "mag-pm-assist", description = "MAG 项目经理协助记录")
public class MagPmAssistController {

    private final MagPmAssistService pmAssistService;

    @GetMapping
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "协助记录列表")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long projectId) {
        return ApiResult.ok(pmAssistService.list(projectId, SecurityUtils.currentUserId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('mag:agent:manage')")
    @Operation(summary = "新增协助记录")
    public ApiResult<Map<String, Object>> create(@PathVariable Long projectId,
                                                 @Valid @RequestBody MagPmAssistCreateRequest req) {
        return ApiResult.ok(pmAssistService.create(projectId, req, SecurityUtils.currentUserId()));
    }
}
