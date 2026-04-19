package com.expensetracker.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    @Test
    void corsConfigurationAllowsGitHubPagesAndPreflightMethods() {
        SecurityConfig securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(
                securityConfig,
                "allowedOriginPatterns",
                "http://localhost:*,http://127.0.0.1:*,https://sharoshosho.github.io"
        );

        CorsConfiguration corsConfiguration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/auth/login"));

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOriginPatterns()).contains("https://sharoshosho.github.io");
        assertThat(corsConfiguration.getAllowedMethods()).contains("OPTIONS", "POST");
        assertThat(corsConfiguration.getAllowCredentials()).isTrue();
    }
}
