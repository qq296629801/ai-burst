package com.aiburst.mag.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@TestConfiguration
@EnableMethodSecurity(prePostEnabled = true)
public class MagMethodSecurityTestConfig {
}
