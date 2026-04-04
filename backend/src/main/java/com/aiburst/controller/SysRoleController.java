package com.aiburst.controller;

import com.aiburst.common.ApiResult;
import com.aiburst.dto.RoleSaveRequest;
import com.aiburst.service.SysRoleService;
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

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResult<List<Map<String, Object>>> list() {
        return ApiResult.ok(sysRoleService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public ApiResult<Void> create(@Valid @RequestBody RoleSaveRequest req) {
        sysRoleService.save(req);
        return ApiResult.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    public ApiResult<Void> update(@Valid @RequestBody RoleSaveRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("id required");
        }
        sysRoleService.save(req);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        sysRoleService.delete(id);
        return ApiResult.ok();
    }
}
