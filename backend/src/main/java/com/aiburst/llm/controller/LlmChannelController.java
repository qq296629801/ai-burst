package com.aiburst.llm.controller;

import com.aiburst.common.ApiResult;
import com.aiburst.llm.dto.LlmChannelSaveRequest;
import com.aiburst.llm.dto.LlmChannelVO;
import com.aiburst.llm.service.LlmChannelService;
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

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/llm/channels")
@RequiredArgsConstructor
public class LlmChannelController {

    private final LlmChannelService llmChannelService;

    @GetMapping
    @PreAuthorize("hasAuthority('llm:channel:list')")
    public ApiResult<List<LlmChannelVO>> list() {
        return ApiResult.ok(llmChannelService.listMine());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('llm:channel:add')")
    public ApiResult<Void> create(@Valid @RequestBody LlmChannelSaveRequest req) {
        llmChannelService.save(req);
        return ApiResult.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('llm:channel:edit')")
    public ApiResult<Void> update(@Valid @RequestBody LlmChannelSaveRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("id required");
        }
        llmChannelService.save(req);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('llm:channel:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        llmChannelService.delete(id);
        return ApiResult.ok();
    }
}
