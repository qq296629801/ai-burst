package com.aiburst.llm.controller;

import com.aiburst.common.ApiResult;
import com.aiburst.llm.catalog.LlmProviderCatalog;
import com.aiburst.llm.model.LlmProviderMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmMetaController {

    private final LlmProviderCatalog catalog;

    @GetMapping("/providers")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<List<LlmProviderMeta>> providers() {
        return ApiResult.ok(catalog.listAll());
    }
}
