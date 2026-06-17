package com.danovich.platform.auth;

/** Response for a successful token exchange. */
public record TokenResponse(String token, long expiresInSeconds) {
}
