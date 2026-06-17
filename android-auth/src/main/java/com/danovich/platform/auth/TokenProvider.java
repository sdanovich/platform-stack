package com.danovich.platform.auth;

/**
 * Supplies a freshly-minted JWT on demand. The project implements this by calling
 * its own token endpoint with the shared client secret — that call typically goes
 * through the project's own HTTP client (so it follows the project's base-URL /
 * tunnel logic), which is exactly why the library does not hardcode it here.
 *
 * <p>Implementations must be safe to call from an OkHttp interceptor thread and
 * should return {@code null} on failure rather than throwing.
 */
public interface TokenProvider {

    /** @return a new bearer token, or {@code null} if one couldn't be obtained. */
    String fetchFreshToken();
}
