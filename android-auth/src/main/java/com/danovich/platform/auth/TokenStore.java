package com.danovich.platform.auth;

/**
 * In-memory holder for the current JWT. The token deliberately does not persist
 * to disk — it is short-lived and cheaply re-obtained from the client secret, so
 * keeping it only in memory limits exposure. Mirrors NearMe.
 */
public final class TokenStore {

    private static volatile String token;

    private TokenStore() {
    }

    public static String get() {
        return token;
    }

    public static void set(String value) {
        token = value;
    }

    public static void clear() {
        token = null;
    }
}
