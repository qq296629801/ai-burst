package com.aiburst.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aiburst.common.ApiResult;
import com.aiburst.common.ResultCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.aiburst.rbac.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final PermissionCacheService permissionCacheService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String cp = request.getContextPath() == null ? "" : request.getContextPath();
        if (path.startsWith(cp + "/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        try {
            io.jsonwebtoken.Claims claims = jwtService.parse(token);
            String jti = claims.getId();
            if (blacklistService.isBlacklisted(jti)) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ApiResult.fail(ResultCode.UNAUTHORIZED));
                return;
            }
            Long userId = Long.parseLong(claims.getSubject());
            List<String> codes = permissionCacheService.getPermCodes(userId);
            List<SimpleGrantedAuthority> authorities = codes.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            LoginPrincipal principal = new LoginPrincipal(userId, claims.get("username", String.class));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ExpiredJwtException e) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ApiResult.fail(ResultCode.UNAUTHORIZED));
            return;
        } catch (JwtException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ApiResult.fail(ResultCode.UNAUTHORIZED));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeJson(HttpServletResponse response, int status, ApiResult<?> body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
