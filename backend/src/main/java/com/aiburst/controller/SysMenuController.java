package com.aiburst.controller;

import com.aiburst.common.ApiResult;
import com.aiburst.dto.MenuSaveRequest;
import com.aiburst.dto.MenuVO;
import com.aiburst.service.SysMenuService;
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
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public ApiResult<List<MenuVO>> tree() {
        return ApiResult.ok(sysMenuService.treeAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public ApiResult<Void> create(@Valid @RequestBody MenuSaveRequest req) {
        sysMenuService.save(req);
        return ApiResult.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public ApiResult<Void> update(@Valid @RequestBody MenuSaveRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("id required");
        }
        sysMenuService.save(req);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        sysMenuService.delete(id);
        return ApiResult.ok();
    }
}
