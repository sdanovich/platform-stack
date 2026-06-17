package com.danovich.platform.auth;

/** Request body for the token exchange: the shared client secret. */
public record TokenRequest(String clientSecret) {
}
