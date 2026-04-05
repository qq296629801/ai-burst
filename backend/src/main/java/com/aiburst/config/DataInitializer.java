package com.aiburst.config;

import com.aiburst.rbac.entity.SysUser;
import com.aiburst.rbac.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Long ADMIN_ROLE_ID = 1L;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userMapper.selectByUsername("admin") != null) {
            return;
        }
        SysUser u = new SysUser();
        u.setUsername("admin");
        u.setPassword(passwordEncoder.encode("admin123"));
        u.setNickname("管理员");
        u.setStatus(1);
        userMapper.insert(u);
        userMapper.insertUserRole(u.getId(), ADMIN_ROLE_ID);
        log.info("Seeded default user admin / admin123");
    }
}
