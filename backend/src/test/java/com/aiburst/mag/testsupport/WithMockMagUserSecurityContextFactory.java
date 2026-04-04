package com.aiburst.mag.testsupport;

import com.aiburst.security.LoginPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.List;

public class WithMockMagUserSecurityContextFactory implements WithSecurityContextFactory<WithMockMagUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockMagUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        List<SimpleGrantedAuthority> auths = Arrays.stream(annotation.authorities())
                .map(SimpleGrantedAuthority::new)
                .toList();
        LoginPrincipal principal = new LoginPrincipal(annotation.userId(), annotation.username());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, auths);
        context.setAuthentication(authentication);
        return context;
    }
}
