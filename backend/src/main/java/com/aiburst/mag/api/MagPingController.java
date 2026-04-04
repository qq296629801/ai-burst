package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 占位接口，便于 SpringDoc {@code mag} 分组非空；后续由真实 Controller 替代或保留为探活。
 */
@RestController
@RequestMapping("/api/mag")
@Tag(name = "mag-core", description = "MAG 模块")
public class MagPingController {

    @GetMapping("/ping")
    @Operation(summary = "MAG 模块探活")
    public ApiResult<String> ping() {
        return ApiResult.ok("mag");
    }
}
