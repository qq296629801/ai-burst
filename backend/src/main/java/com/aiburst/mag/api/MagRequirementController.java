package com.aiburst.mag.api;

import com.aiburst.common.ApiResult;
import com.aiburst.mag.dto.MagPoolDecideRequest;
import com.aiburst.mag.dto.MagPoolItemCreateRequest;
import com.aiburst.mag.dto.MagPoolProductCloseRequest;
import com.aiburst.mag.dto.MagRequirementChangeAnalyzeRequest;
import com.aiburst.mag.dto.MagRequirementSaveRequest;
import com.aiburst.mag.service.MagRequirementService;
import com.aiburst.rbac.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mag")
@RequiredArgsConstructor
@Tag(name = "mag-requirement", description = "MAG 需求与需求池")
public class MagRequirementController {

    private final MagRequirementService requirementService;

    @GetMapping("/projects/{projectId}/requirement-doc")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "需求文档当前版本")
    public ApiResult<Map<String, Object>> getDoc(@PathVariable Long projectId) {
        return ApiResult.ok(requirementService.getDoc(projectId, SecurityUtils.currentUserId()));
    }

    @PutMapping("/projects/{projectId}/requirement-doc")
    @PreAuthorize("hasAuthority('mag:req:edit')")
    @Operation(summary = "保存需求新版本")
    public ApiResult<Map<String, Object>> saveDoc(@PathVariable Long projectId,
                                                  @Valid @RequestBody MagRequirementSaveRequest req) {
        return ApiResult.ok(requirementService.saveDoc(projectId, req, SecurityUtils.currentUserId()));
    }

    @GetMapping("/projects/{projectId}/requirement-pool")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "需求池列表")
    public ApiResult<List<Map<String, Object>>> listPool(@PathVariable Long projectId) {
        return ApiResult.ok(requirementService.listPool(projectId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/projects/{projectId}/requirement-pool")
    @PreAuthorize("hasAuthority('mag:req:edit')")
    @Operation(summary = "新增需求池项（自动并入需求文档，不经待用户拍板）")
    public ApiResult<Map<String, Object>> createPoolItem(@PathVariable Long projectId,
                                                         @Valid @RequestBody MagPoolItemCreateRequest req) {
        return ApiResult.ok(requirementService.createPoolItem(projectId, req, SecurityUtils.currentUserId()));
    }

    @PostMapping("/requirement-pool/{poolItemId}/decide")
    @PreAuthorize("hasAuthority('mag:pool:decide')")
    @Operation(summary = "用户拍板")
    public ApiResult<Void> decide(@PathVariable Long poolItemId, @Valid @RequestBody MagPoolDecideRequest req) {
        requirementService.decide(poolItemId, req, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }

    @PostMapping("/requirement-pool/{poolItemId}/product-close")
    @PreAuthorize("hasAuthority('mag:req:edit')")
    @Operation(summary = "产品侧关闭需求池项并记录结论")
    public ApiResult<Void> productClose(@PathVariable Long poolItemId,
                                       @Valid @RequestBody MagPoolProductCloseRequest req) {
        requirementService.productClosePoolItem(poolItemId, req, SecurityUtils.currentUserId());
        return ApiResult.ok();
    }

    @GetMapping("/projects/{projectId}/requirement-doc/revisions")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "需求文档版本列表")
    public ApiResult<List<Map<String, Object>>> listRevisions(@PathVariable Long projectId) {
        return ApiResult.ok(requirementService.listRevisions(projectId, SecurityUtils.currentUserId()));
    }

    @GetMapping("/projects/{projectId}/requirement-doc/diff")
    @PreAuthorize("hasAuthority('mag:project:list')")
    @Operation(summary = "两版本正文对比")
    public ApiResult<Map<String, Object>> diffRevisions(@PathVariable Long projectId,
                                                        @RequestParam int version1,
                                                        @RequestParam int version2) {
        return ApiResult.ok(requirementService.diffRevisions(projectId, version1, version2, SecurityUtils.currentUserId()));
    }

    @PostMapping("/projects/{projectId}/requirement-change/analyze")
    @PreAuthorize("hasAuthority('mag:req:edit')")
    @Operation(summary = "变更影响分析（协调线程留痕 + 返回 traceId）")
    public ApiResult<Map<String, Object>> analyzeChange(@PathVariable Long projectId,
                                                        @Valid @RequestBody MagRequirementChangeAnalyzeRequest req) {
        return ApiResult.ok(requirementService.analyzeRequirementChange(projectId, req, SecurityUtils.currentUserId()));
    }
}
