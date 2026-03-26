package com.aiburst.service;

import com.aiburst.dto.LoginRequest;
import com.aiburst.dto.LoginResponse;
import com.aiburst.dto.MenuVO;
import com.aiburst.dto.UserProfile;
import com.aiburst.entity.SysPermission;
import com.aiburst.entity.SysUser;
import com.aiburst.mapper.PermissionMapper;
import com.aiburst.mapper.UserMapper;
import com.aiburst.security.JwtService;
import com.aiburst.security.LoginPrincipal;
import com.aiburst.security.PermissionCacheService;
import com.aiburst.security.SecurityUtils;
import com.aiburst.security.TokenBlacklistService;
import com.aiburst.util.MenuTreeUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PermissionCacheService permissionCacheService;

    public LoginResponse login(LoginRequest req) {
        SysUser user = userMapper.selectByUsername(req.getUsername());
        if (user == null || user.getStatus() != 1) {
            throw new BadCredentialsException("invalid credentials");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("invalid credentials");
        }
        String token = jwtService.createToken(user.getId(), user.getUsername());
        return buildLoginResponse(user, token);
    }

    public LoginResponse me() {
        Long uid = SecurityUtils.currentUserId();
        if (uid == null) {
            throw new BadCredentialsException("not authenticated");
        }
        SysUser user = userMapper.selectById(uid);
        if (user == null || user.getStatus() != 1) {
            throw new BadCredentialsException("invalid user");
        }
        String token = null;
        return buildLoginResponse(user, token);
    }

    private LoginResponse buildLoginResponse(SysUser user, String token) {
        List<String> perms = permissionCacheService.getPermCodes(user.getId());
        List<SysPermission> menuFlat = permissionMapper.selectMenuByUserId(user.getId());
        List<MenuVO> menus = MenuTreeUtil.build(menuFlat);
        UserProfile profile = UserProfile.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
        LoginResponse.LoginResponseBuilder b = LoginResponse.builder()
                .user(profile)
                .permissions(perms)
                .menus(menus);
        if (StringUtils.hasText(token)) {
            b.token(token);
        }
        return b.build();
    }

    public void logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return;
        }
        String raw = header.substring(7).trim();
        try {
            Claims claims = jwtService.parse(raw);
            tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration());
        } catch (Exception ignored) {
            // ignore
        }
        Long uid = SecurityUtils.currentUserId();
        if (uid != null) {
            permissionCacheService.evictUser(uid);
        }
        SecurityContextHolder.clearContext();
    }
}
