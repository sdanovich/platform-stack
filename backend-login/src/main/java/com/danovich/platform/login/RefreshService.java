package com.danovich.platform.login;

import com.danovich.platform.login.domain.RefreshToken;
import com.danovich.platform.login.repo.RefreshTokenRepo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and rotates opaque refresh tokens. Only the SHA-256 hash is stored; the
 * raw token is handed to the client once. Rotation revokes the presented token and
 * mints a new one, so a stolen-then-used token is invalidated on the next refresh.
 * Wired as a bean by {@code LoginAutoConfiguration} with
 * {@code platform.auth.refresh.ttl-seconds}.
 */
public class RefreshService {

    private final RefreshTokenRepo repo;
    private final long ttlSeconds;
    private final SecureRandom rng = new SecureRandom();

    public RefreshService(RefreshTokenRepo repo, long ttlSeconds) {
        this.repo = repo;
        this.ttlSeconds = ttlSeconds;
    }

    /** Create a refresh token for a user. Returns the raw token (store only its hash). */
    public String issue(UUID userId) {
        byte[] buf = new byte[32];
        rng.nextBytes(buf);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setTokenHash(sha256(raw));
        rt.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        repo.save(rt);
        return raw;
    }

    /** Validate a raw token; if good, revoke it and issue a replacement. */
    public Optional<Rotated> rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        RefreshToken rt = repo.findByTokenHash(sha256(rawToken)).orElse(null);
        if (rt == null || rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        rt.setRevoked(true);
        repo.save(rt);
        return Optional.of(new Rotated(rt.getUserId(), issue(rt.getUserId())));
    }

    public record Rotated(UUID userId, String refreshToken) {
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(d);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
