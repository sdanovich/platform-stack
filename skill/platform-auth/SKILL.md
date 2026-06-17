---
name: platform-auth
description: Integration guide for the danovich platform-stack auth module — client-credentials JWT shared across projects (NearMe, BidHound, and new ones). Use this skill whenever wiring authentication into a Spring Boot backend or an Android app that should use the shared platform-stack auth code, when adding the auth submodule to a new project, when a user says "add auth like NearMe/BidHound" or "use the platform auth", or when debugging a 401 / token-refresh / Bearer-header problem in a project that consumes platform-stack. Trigger this even if the user just says "lock down the API", "add a token to the backend", or "the app can't authenticate", as long as the project uses (or should use) the shared platform-auth module.
---

# Platform Auth — integration guide

The platform-stack `backend-auth` and `android-auth` modules implement **client-credentials
JWT**: an app exchanges a shared secret for a short-lived HS256 token and sends it as a
Bearer header on every protected call. It authenticates the *app*, not individual users —
the right weight for a single-client backend (one phone, one secret).

This skill is the **integration recipe**. The library owns the code; this owns the wiring —
which is where integration actually goes wrong (lost files, missing init calls, wrong
interceptor order, forgetting to exempt the token endpoint). Follow the steps in order.

## When this applies

Use the shared module when a project has a backend that should reject unauthenticated
`/api/**` calls and a single client (typically the project's own Android app) that holds one
shared secret. If the project needs per-user accounts, OAuth, or third-party login, this
module is the wrong tool — say so rather than forcing it.

## Add the submodule

The modules are consumed as **source via git submodule**, not published artifacts:

```bash
git submodule add https://github.com/sdanovich/platform-stack vendor/platform-stack
git submodule update --init --recursive
```

Pin and update deliberately — a submodule tracks a specific commit, so a project only moves
to new auth code when you run `git submodule update --remote`. That is the point: bug fixes
land in one place, and projects adopt them on purpose.

## Backend wiring (Spring Boot, Java/Kotlin)

The backend module is Java but works unchanged from a Kotlin Spring Boot app.

1. **Include the module** in `settings.gradle(.kts)` and depend on it:
   ```kotlin
   include(":backend-auth")
   project(":backend-auth").projectDir =
       file("vendor/platform-stack/backend-auth")
   // build.gradle(.kts):
   implementation(project(":backend-auth"))
   ```

2. **Import the autoconfiguration.** The module ships an
   `AutoConfiguration.imports` file, so on Spring Boot it loads automatically once
   the module is on the classpath — no `@Import` needed. If your project disables
   autoconfiguration or you want it explicit, add to the main app or a config class:
   ```java
   @Import(PlatformAuthAutoConfiguration.class)
   ```
   Either way this registers three beans: the `JwtService`, the enforcing
   `JwtAuthFilter` (high priority, runs before app filters), and the token endpoint
   at `POST /api/auth/token`.

3. **Set the four properties** — secrets from env only, never committed:
   ```yaml
   platform:
     auth:
       client-secret: ${MYAPP_AUTH_CLIENT_SECRET:}
       jwt-secret: ${MYAPP_JWT_SECRET:}        # MUST be >= 32 bytes or startup fails
       ttl-seconds: 3600
       protected-prefix: /api/
       subject: myapp-app
       public-paths: [/api/auth/token]
   ```

4. **Add the env vars** to `.env.example` and `docker-compose.yml` (pass-through, with
   empty defaults so a missing secret degrades gracefully rather than baking a value in).

### The two backend gotchas

- **The token endpoint MUST be in `public-paths`.** It lives under `/api/`, so the filter
  would protect it — but you need a token to pass the filter, and you get a token by calling
  it. If it is not exempt, the client can never authenticate at all. CORS preflight
  (`OPTIONS`) is exempted automatically; the token path is not.
- **`jwt-secret` must be at least 32 bytes.** HS256 key construction throws below that, and
  the app fails fast at startup. Use a real random 32+ byte string, not a short passphrase.

## Android wiring (OkHttp)

The Android module is plain Java (no Android types), so it builds as a `java-library`.

1. **Include the module** and depend on it (`implementation(project(":platform-auth"))`,
   pointing `projectDir` at `vendor/platform-stack/android-auth`).

2. **Implement `TokenProvider`** — this is the one piece the library cannot supply, because
   fetching a token means calling *this project's* token endpoint through *this project's*
   HTTP client (so it follows the project's own base-URL / tunnel logic). Keep it simple and
   non-throwing:
   ```java
   TokenProvider provider = () -> {
       try {
           // call POST {base}/api/auth/token with {clientSecret} via the project's
           // own client; return the token string, or null on any failure
           return myAuthApi.token(new TokenRequest(BuildConfig.AUTH_CLIENT_SECRET)).token();
       } catch (Exception e) {
           return null;
       }
   };
   ```

3. **Build the OkHttpClient with both interceptors, in the right order:**
   ```java
   OkHttpClient client = new OkHttpClient.Builder()
       .addInterceptor(new TokenRefreshInterceptor(provider, "/api/auth/token")) // OUTER
       .addInterceptor(new BearerAuthInterceptor("/api/auth/token"))             // INNER
       .build();
   ```

4. **Prime an initial token** before the first protected call (on a 401 the refresh
   interceptor covers expiry, but priming avoids a guaranteed-401 on every cold start):
   ```java
   if (TokenStore.get() == null) TokenStore.set(provider.fetchFreshToken());
   ```

5. **Pass the client secret at build time**, not in source:
   `-PauthClientSecret=<secret>` → a `BuildConfig` field that must equal the backend's
   `MYAPP_AUTH_CLIENT_SECRET`.

### The Android gotcha: interceptor order

`TokenRefreshInterceptor` must be added **before** `BearerAuthInterceptor` so it sits OUTER
in the chain. OkHttp runs application interceptors top-to-bottom on the way out, so the
refresh interceptor wraps the bearer one: when it retries after a 401, the retry flows back
*through* the bearer interceptor and picks up the new token. Reverse the order and the retry
goes out with no header — a silent, confusing auth failure. If a project also has tunnel /
host-selection interceptors, host-selection stays INNERMOST (it stamps the final URL last).

## Verifying it works

```bash
# 1. Token exchange (CLIENT_SECRET = the backend's MYAPP_AUTH_CLIENT_SECRET)
TOKEN=$(curl -s -X POST localhost:8080/api/auth/token \
  -H 'content-type: application/json' \
  -d '{"clientSecret":"'"$CLIENT_SECRET"'"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

# 2. Protected call without a token -> 401
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/feed          # 401

# 3. Same call with the token -> 200
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/feed \
  -H "Authorization: Bearer $TOKEN"                                        # 200
```

If step 2 returns 200, the filter isn't enforcing (check the import and `protected-prefix`).
If step 1 returns 401, the client secret doesn't match or the token path isn't in
`public-paths`. If step 3 returns 401 with a valid token, suspect a too-short `jwt-secret` or
a clock-skew expiry.

## What the library owns vs. what the project owns

Keep this boundary clear when extending:

- **Library (don't fork per project):** `JwtService`, `JwtAuthFilter`, `TokenController`,
  the autoconfiguration; Android `TokenStore`, `BearerAuthInterceptor`,
  `TokenRefreshInterceptor`, the `TokenProvider` interface.
- **Project (supplies each time):** the secrets and properties; the `TokenProvider`
  implementation; the OkHttpClient assembly; the build-time client-secret field. These
  depend on the project's own HTTP client and config, so they live in the project.

If you find yourself copying library files into a project to tweak them, stop — add a
configuration point to the library instead, so every project still shares one implementation.
```
