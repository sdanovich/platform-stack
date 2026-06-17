package com.danovich.platform.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET32 = "0123456789abcdef0123456789abcdef"; // 32 bytes

    @Test
    void issuesAndValidatesToken() {
        JwtService svc = new JwtService("client-secret", SECRET32, 3600);
        String token = svc.issue("app");
        assertNotNull(token);
        assertTrue(svc.isValid(token));
    }

    @Test
    void rejectsGarbageToken() {
        JwtService svc = new JwtService("client-secret", SECRET32, 3600);
        assertFalse(svc.isValid("not-a-jwt"));
        assertFalse(svc.isValid(null));
        assertFalse(svc.isValid(""));
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtService a = new JwtService("cs", SECRET32, 3600);
        JwtService b = new JwtService("cs", "ffffffffffffffffffffffffffffffff", 3600);
        String tokenFromA = a.issue("app");
        assertFalse(b.isValid(tokenFromA), "token signed by A must not validate under B");
    }

    @Test
    void verifiesClientSecret() {
        JwtService svc = new JwtService("right", SECRET32, 3600);
        assertTrue(svc.verifyClientSecret("right"));
        assertFalse(svc.verifyClientSecret("wrong"));
        assertFalse(svc.verifyClientSecret(null));
    }

    @Test
    void rejectsShortJwtSecret() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtService("cs", "too-short", 3600));
    }

    @Test
    void blankClientSecretNeverVerifies() {
        JwtService svc = new JwtService("", SECRET32, 3600);
        assertFalse(svc.verifyClientSecret(""), "empty configured secret must not accept empty input");
    }
}
