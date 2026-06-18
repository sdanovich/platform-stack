package com.danovich.platform.login;

/** Auth request/response shapes shared by email, social, and refresh endpoints. */
public final class AuthDtos {

    public record AuthRequest(String email, String password) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record GoogleRequest(String idToken) {
    }

    public record GitHubRequest(String code) {
    }

    public record AuthResponse(String token, long expiresInSeconds, String userId,
                               String email, String refreshToken) {
    }

    private AuthDtos() {
    }
}
