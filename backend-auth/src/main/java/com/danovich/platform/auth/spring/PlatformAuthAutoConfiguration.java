package com.danovich.platform.auth.spring;

import com.danovich.platform.auth.JwtAuthFilter;
import com.danovich.platform.auth.JwtService;
import com.danovich.platform.auth.TokenController;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Drop-in wiring for the platform auth module. A project enables the whole stack
 * with one annotation:
 *
 * <pre>
 * &#64;Import(PlatformAuthAutoConfiguration.class)
 * </pre>
 *
 * and four properties under {@code platform.auth.*}. This registers the JWT
 * service, the enforcing filter (high priority, so it runs before app filters),
 * and the default token endpoint at {@code POST /api/auth/token}.
 *
 * <p>If a project needs a non-default token path, it can skip importing this and
 * declare the three beans itself — the building blocks have no framework coupling.
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class PlatformAuthAutoConfiguration {

    @Bean
    public JwtService platformJwtService(AuthProperties props) {
        return new JwtService(props.getClientSecret(), props.getJwtSecret(), props.getTtlSeconds());
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> platformJwtAuthFilter(
            JwtService jwtService, AuthProperties props) {
        JwtAuthFilter filter = new JwtAuthFilter(
                jwtService, props.getProtectedPrefix(), props.getPublicPaths());
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filter);
        // Run early, before application filters.
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public TokenController platformTokenController(JwtService jwtService, AuthProperties props) {
        return new TokenController(jwtService, props.getSubject());
    }
}
