package com.pyin.center.auth.web;

import com.pyin.center.auth.authentication.AdminRequestAuthenticator;
import com.pyin.center.auth.authentication.ClientRequestAuthenticator;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthWebMvcConfiguration implements WebMvcConfigurer {

    private final AdminRequestAuthenticator adminRequestAuthenticator;
    private final ClientRequestAuthenticator clientRequestAuthenticator;

    public AuthWebMvcConfiguration(
            AdminRequestAuthenticator adminRequestAuthenticator,
            ClientRequestAuthenticator clientRequestAuthenticator
    ) {
        this.adminRequestAuthenticator = adminRequestAuthenticator;
        this.clientRequestAuthenticator = clientRequestAuthenticator;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor(adminRequestAuthenticator))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/core/plugin-nodes/**",
                        "/api/public/**"
                );

        registry.addInterceptor(new ClientAuthInterceptor(clientRequestAuthenticator))
                .addPathPatterns("/open/**")
                .excludePathPatterns(
                        "/open/auth/token",
                        "/open/public/**"
                );
    }
}
