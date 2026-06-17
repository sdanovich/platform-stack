package com.danovich.platform.auth.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a project configures to use the platform auth module. All values
 * come from the project's environment / application config — nothing is baked
 * into the library, and secrets never live in the submodule.
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * platform:
 *   auth:
 *     client-secret: ${MYAPP_AUTH_CLIENT_SECRET:}
 *     jwt-secret: ${MYAPP_JWT_SECRET:}          # >= 32 bytes
 *     ttl-seconds: 3600
 *     protected-prefix: /api/
 *     subject: myapp-app
 *     public-paths: [/api/auth/token]
 * </pre>
 */
@ConfigurationProperties(prefix = "platform.auth")
public class AuthProperties {

    /** Shared secret an app presents to obtain a token. */
    private String clientSecret = "";

    /** HS256 signing key material; must be >= 32 bytes. */
    private String jwtSecret = "";

    /** Token lifetime in seconds. */
    private long ttlSeconds = 3600;

    /** Path prefix that requires a token. */
    private String protectedPrefix = "/api/";

    /** Subject claim placed in issued tokens. */
    private String subject = "app";

    /** Exact paths under the prefix that bypass auth (must include the token endpoint). */
    private List<String> publicPaths = new ArrayList<>(List.of("/api/auth/token"));

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String v) { this.clientSecret = v; }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String v) { this.jwtSecret = v; }

    public long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(long v) { this.ttlSeconds = v; }

    public String getProtectedPrefix() { return protectedPrefix; }
    public void setProtectedPrefix(String v) { this.protectedPrefix = v; }

    public String getSubject() { return subject; }
    public void setSubject(String v) { this.subject = v; }

    public List<String> getPublicPaths() { return publicPaths; }
    public void setPublicPaths(List<String> v) { this.publicPaths = v; }
}
