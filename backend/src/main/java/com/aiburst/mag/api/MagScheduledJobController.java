package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.dto.MagScheduledJobUpsertRequest;
import com.aiburst.mag.service.MagScheduledJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mag/scheduled-jobs")
@RequiredArgsConstructor
@Tag(name = "mag-scheduled-job", description = "MAG 定时任务配置")
public class MagScheduledJobController {

    private final MagScheduledJobService scheduledJobService;

    @GetMapping
    @PreAuthorize("hasAuthority('mag:sched:manage')")
    @Operation(summary = "定时任务配置列表（可选按项目过滤）")
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) Long projectId) {
        return ApiResult.ok(scheduledJobService.list(projectId));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('mag:sched:manage')")
    @Operation(summary = "创建或更新定时任务配置")
    public ApiResult<Map<String, Object>> upsert(@Valid @RequestBody MagScheduledJobUpsertRequest req) {
        return ApiResult.ok(scheduledJobService.upsert(req));
    }
}
