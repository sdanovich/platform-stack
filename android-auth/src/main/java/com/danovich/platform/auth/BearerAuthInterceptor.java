package com.danovich.platform.auth;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Attaches {@code Authorization: Bearer <jwt>} to every outgoing request, except
 * calls to the token-exchange endpoint itself (which has no token yet and would
 * loop). The exempt path is configurable because projects mount the endpoint
 * differently.
 */
public final class BearerAuthInterceptor implements Interceptor {

    private final String tokenPath;

    /** @param tokenPath the request path that must NOT carry a bearer token. */
    public BearerAuthInterceptor(String tokenPath) {
        this.tokenPath = tokenPath == null ? "/api/auth/token" : tokenPath;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        if (request.url().encodedPath().equals(tokenPath)) {
            return chain.proceed(request);
        }
        String token = TokenStore.get();
        if (token == null) {
            return chain.proceed(request);
        }
        return chain.proceed(
                request.newBuilder().header("Authorization", "Bearer " + token).build());
    }
}
