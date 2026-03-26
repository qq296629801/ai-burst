package com.aiburst.controller;

import com.aiburst.common.ApiResult;
import com.aiburst.dto.PageResult;
import com.aiburst.dto.UserPageQuery;
import com.aiburst.dto.UserSaveRequest;
import com.aiburst.service.SysUserService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResult<PageResult<Map<String, Object>>> page(UserPageQuery q) {
        return ApiResult.ok(sysUserService.page(q));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public ApiResult<Void> create(@Valid @RequestBody UserSaveRequest req) {
        sysUserService.save(req);
        return ApiResult.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    public ApiResult<Void> update(@Valid @RequestBody UserSaveRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("id required");
        }
        sysUserService.save(req);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:resetPwd')")
    public ApiResult<Void> resetPwd(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String pwd = body.get("password");
        if (pwd == null || pwd.length() < 6) {
            throw new IllegalArgumentException("password min length 6");
        }
        sysUserService.resetPassword(id, pwd);
        return ApiResult.ok();
    }
}
