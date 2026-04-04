package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.dto.PageResult;
import com.aiburst.mag.dto.MagKbEntryCreateRequest;
import com.aiburst.mag.dto.MagKbUpdateRequest;
import com.aiburst.mag.dto.MagPageQuery;
import com.aiburst.mag.service.MagKbService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mag/kb/entries")
@RequiredArgsConstructor
@Tag(name = "mag-kb", description = "MAG 知识库")
public class MagKbController {

    private final MagKbService kbService;

    @GetMapping
    @PreAuthorize("hasAuthority('mag:kb:manage')")
    @Operation(summary = "知识库分页")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(required = false) String keyword,
                                                          @Valid MagPageQuery q) {
        return ApiResult.ok(kbService.page(keyword, q));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('mag:kb:manage')")
    @Operation(summary = "新建知识库条目")
    public ApiResult<Map<String, Object>> create(@Valid @RequestBody MagKbEntryCreateRequest req) {
        return ApiResult.ok(kbService.create(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('mag:kb:manage')")
    @Operation(summary = "知识库条目详情")
    public ApiResult<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResult.ok(kbService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('mag:kb:manage')")
    @Operation(summary = "更新知识库条目（归档回流条目只读）")
    public ApiResult<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody MagKbUpdateRequest req) {
        return ApiResult.ok(kbService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('mag:kb:manage')")
    @Operation(summary = "删除知识库条目（归档回流条目不可删）")
    public ApiResult<Void> delete(@PathVariable Long id) {
        kbService.delete(id);
        return ApiResult.ok();
    }
}
