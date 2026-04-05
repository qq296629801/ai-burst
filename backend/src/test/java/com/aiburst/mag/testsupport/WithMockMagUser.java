package com.aiburst.mag.testsupport;

import com.aiburst.rbac.security.LoginPrincipal;
import com.aiburst.rbac.security.SecurityUtils;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * 将 {@link LoginPrincipal} 放入 SecurityContext，供 {@link SecurityUtils#currentUserId()} 使用。
 */
@Target({TYPE, METHOD})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockMagUserSecurityContextFactory.class)
public @interface WithMockMagUser {

    long userId() default 1L;

    String username() default "tester";

    /** Spring Security 权限，如 mag:project:list */
    String[] authorities() default {};
}
