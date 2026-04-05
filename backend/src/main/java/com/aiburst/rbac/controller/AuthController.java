package com.aiburst.rbac.controller;

import com.aiburst.common.ApiResult;
import com.aiburst.dto.LoginRequest;
import com.aiburst.dto.LoginResponse;
import com.aiburst.rbac.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResult.ok(authService.login(req));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<LoginResponse> me() {
        return ApiResult.ok(authService.me());
    }
}
