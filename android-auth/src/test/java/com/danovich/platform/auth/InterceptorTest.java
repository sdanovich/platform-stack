package com.danovich.platform.auth;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class InterceptorTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        TokenStore.clear();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        TokenStore.clear();
    }

    @Test
    void attachesBearerWhenTokenPresent() throws Exception {
        TokenStore.set("abc123");
        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new BearerAuthInterceptor("/api/auth/token"))
                .build();
        try (Response r = client.newCall(
                new Request.Builder().url(server.url("/api/feed")).build()).execute()) {
            assertEquals(200, r.code());
        }
        RecordedRequest req = server.takeRequest();
        assertEquals("Bearer abc123", req.getHeader("Authorization"));
    }

    @Test
    void refreshesAndRetriesOn401() throws Exception {
        // First response 401, second 200 — refresh interceptor should retry once
        // with the freshly-provided token attached by the inner bearer interceptor.
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(new MockResponse().setResponseCode(200));

        TokenProvider provider = () -> "fresh-token";

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new TokenRefreshInterceptor(provider, "/api/auth/token")) // outer
                .addInterceptor(new BearerAuthInterceptor("/api/auth/token"))             // inner
                .build();

        try (Response r = client.newCall(
                new Request.Builder().url(server.url("/api/feed")).build()).execute()) {
            assertEquals(200, r.code(), "retry after refresh should succeed");
        }

        server.takeRequest(); // first (401) attempt
        RecordedRequest retried = server.takeRequest();
        assertEquals("Bearer fresh-token", retried.getHeader("Authorization"),
                "retried request must carry the refreshed token");
        assertEquals("fresh-token", TokenStore.get(), "store updated with fresh token");
    }

    @Test
    void doesNotRefreshTokenEndpointItself() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));

        TokenProvider provider = () -> { throw new AssertionError("must not refresh on token path"); };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new TokenRefreshInterceptor(provider, "/api/auth/token"))
                .build();

        try (Response r = client.newCall(
                new Request.Builder().url(server.url("/api/auth/token")).build()).execute()) {
            assertEquals(401, r.code(), "a 401 from the token endpoint surfaces as-is");
        }
    }
}
