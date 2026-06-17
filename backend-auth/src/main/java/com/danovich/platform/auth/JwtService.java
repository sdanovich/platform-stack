package com.danovich.platform.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Client-credentials JWT service. An app exchanges a shared client secret for a
 * short-lived HS256 token, then presents it as a Bearer credential on every
 * protected call. This authenticates the <em>app</em>, not individual end users —
 * the right weight for a single-client backend (one phone, one secret).
 *
 * <p>Extracted from NearMe so every project shares one implementation instead of
 * re-deriving it. The library owns the token logic; the project owns the secrets
 * (see {@link AuthProperties}) and which paths are public (see {@link JwtAuthFilter}).
 *
 * <p>This class has no framework dependencies — construct it directly, or let the
 * Spring autoconfiguration in {@code spring/} wire it from properties.
 */
public final class JwtService {

    private final String clientSecret;
    private final SecretKey signingKey;
    private final long ttlSeconds;

    /**
     * @param clientSecret the shared secret an app must present to obtain a token;
     *                     must be non-blank in any real deployment
     * @param jwtSecret    HS256 signing key material; <strong>must be at least 32
     *                     bytes</strong> (256 bits) or HS256 key construction fails
     * @param ttlSeconds   token lifetime in seconds
     */
    public JwtService(String clientSecret, String jwtSecret, long ttlSeconds) {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "jwtSecret must be at least 32 bytes for HS256");
        }
        this.clientSecret = clientSecret == null ? "" : clientSecret;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    /** Constant-time-ish check that the presented secret matches the configured one. */
    public boolean verifyClientSecret(String candidate) {
        return !clientSecret.isEmpty()
                && candidate != null
                && constantTimeEquals(clientSecret, candidate);
    }

    /** Issue a freshly signed token. Returns the compact JWT string. */
    public String issue(String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject == null ? "app" : subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlSeconds * 1000L))
                .signWith(signingKey)
                .compact();
    }

    /** @return token TTL in seconds, for echoing back to the client. */
    public long getTtlSeconds() {
        return ttlSeconds;
    }

    /** Verify signature and expiry. Never throws — invalid tokens return false. */
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ab.length != bb.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < ab.length; i++) {
            diff |= ab[i] ^ bb[i];
        }
        return diff == 0;
    }
}
