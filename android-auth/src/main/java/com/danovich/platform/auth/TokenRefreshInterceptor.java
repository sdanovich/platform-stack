package com.danovich.platform.auth;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * On a 401, obtains a fresh token via the supplied {@link TokenProvider}, stores
 * it, and retries the request once. A second 401 is returned to the caller rather
 * than looping. The token-exchange endpoint is skipped so a bad client secret
 * surfaces its own 401 instead of triggering a refresh loop.
 *
 * <p>Install this OUTSIDE {@link BearerAuthInterceptor} in the OkHttp chain so the
 * retried request still gets the (new) bearer header attached.
 */
public final class TokenRefreshInterceptor implements Interceptor {

    private final TokenProvider tokenProvider;
    private final String tokenPath;

    public TokenRefreshInterceptor(TokenProvider tokenProvider, String tokenPath) {
        this.tokenProvider = tokenProvider;
        this.tokenPath = tokenPath == null ? "/api/auth/token" : tokenPath;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        if (response.code() != 401 || request.url().encodedPath().equals(tokenPath)) {
            return response;
        }

        // Free the first response body before issuing the retry.
        response.close();

        String fresh = tokenProvider.fetchFreshToken();
        if (fresh == null) {
            // Couldn't refresh; re-issue the original so the caller sees the 401.
            return chain.proceed(request);
        }
        TokenStore.set(fresh);

        Request retried = request.newBuilder()
                .header("Authorization", "Bearer " + fresh)
                .build();
        return chain.proceed(retried);
    }
}
