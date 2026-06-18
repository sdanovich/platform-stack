package com.danovich.platform.login.spring;

import com.danovich.platform.auth.JwtService;
import com.danovich.platform.login.AuthController;
import com.danovich.platform.login.CurrentUserArgumentResolver;
import com.danovich.platform.login.GitHubClient;
import com.danovich.platform.login.GoogleVerifier;
import com.danovich.platform.login.RefreshService;
import com.danovich.platform.login.SocialAuthController;
import com.danovich.platform.login.UserCreatedCallback;
import com.danovich.platform.login.repo.RefreshTokenRepo;
import com.danovich.platform.login.repo.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Wires the login module into a host Spring Boot app: the auth controllers,
 * Google/GitHub clients, refresh service, bcrypt encoder, and the {@code @CurrentUser}
 * argument resolver — all from config (oauth.*, platform.auth.*). Beans are declared
 * explicitly (no component scan) so the library never pulls in unexpected beans.
 *
 * <p>The app is responsible only for: making the module's JPA entities + repositories
 * visible (entity/repository scan must include {@code com.danovich.platform.login.*}),
 * supplying config, and optionally a {@link UserCreatedCallback} bean.
 */
@AutoConfiguration
public class LoginAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Default no-op; an app overrides it by declaring its own UserCreatedCallback bean. */
    @Bean
    @ConditionalOnMissingBean
    public UserCreatedCallback userCreatedCallback() {
        return (userId, email, provider) -> {
        };
    }

    @Bean
    public GoogleVerifier googleVerifier(@Value("${oauth.google.client-id:}") String clientId) {
        return new GoogleVerifier(clientId);
    }

    @Bean
    public GitHubClient gitHubClient(
            @Value("${oauth.github.client-id:}") String clientId,
            @Value("${oauth.github.client-secret:}") String clientSecret,
            @Value("${oauth.github.token-url:https://github.com/login/oauth/access_token}") String tokenUrl,
            @Value("${oauth.github.api-base:https://api.github.com}") String apiBase,
            @Value("${oauth.github.user-agent:platform-login}") String userAgent) {
        return new GitHubClient(clientId, clientSecret, tokenUrl, apiBase, userAgent);
    }

    @Bean
    public RefreshService refreshService(RefreshTokenRepo repo,
            @Value("${platform.auth.refresh.ttl-seconds:2592000}") long ttlSeconds) {
        return new RefreshService(repo, ttlSeconds);
    }

    @Bean
    public AuthController authController(UserRepo userRepo, BCryptPasswordEncoder encoder,
            JwtService jwt, RefreshService refreshService, UserCreatedCallback onUserCreated) {
        return new AuthController(userRepo, encoder, jwt, refreshService, onUserCreated);
    }

    @Bean
    public SocialAuthController socialAuthController(GoogleVerifier google, GitHubClient github,
            UserRepo userRepo, UserCreatedCallback onUserCreated, AuthController auth) {
        return new SocialAuthController(google, github, userRepo, onUserCreated, auth);
    }

    /** Registers the {@code @CurrentUser UUID} resolver, keyed by the shared HS256 secret. */
    @Bean
    public WebMvcConfigurer currentUserArgumentResolverConfigurer(
            @Value("${platform.auth.jwt-secret:}") String jwtSecret) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentUserArgumentResolver(jwtSecret));
            }
        };
    }
}
