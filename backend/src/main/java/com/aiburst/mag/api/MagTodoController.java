package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.dto.PageResult;
import com.aiburst.mag.dto.MagPageQuery;
import com.aiburst.mag.service.MagTodoService;
import com.aiburst.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mag/todos")
@RequiredArgsConstructor
@Tag(name = "mag-todo", description = "MAG 待拍板")
public class MagTodoController {

    private final MagTodoService todoService;

    @GetMapping
    @PreAuthorize("hasAuthority('mag:pool:decide')")
    @Operation(summary = "待拍板聚合")
    public ApiResult<PageResult<Map<String, Object>>> page(@Valid MagPageQuery q) {
        return ApiResult.ok(todoService.page(SecurityUtils.currentUserId(), q));
    }
}
