# platform-login-ui

A parameterized Compose sign-in experience (email + Google + GitHub) plus its
view-model, session store, OAuth relay, and the Retrofit `AuthApi` contract. A
consuming app injects branding, the social-provider client ids, and its own
OkHttp-backed `AuthApi` through a `LoginConfig`, then renders
`PlatformLoginScreen(config)`.

The library owns the screen and the flow; the **consuming app owns the client
ids and how they reach the build.** That handoff is where integration goes
wrong, so read the convention below before wiring a new project.

## Injecting the OAuth client ids (the convention)

`LoginConfig` takes the ids as plain strings — a **blank id disables that
button** (the screen shows "… isn't set up yet"). Apps supply them from their
own `BuildConfig`, never from source:

```kotlin
// app/build.gradle.kts — in defaultConfig, NOT inside a buildTypes { } block
defaultConfig {
    val googleClientId = (project.findProperty("googleServerClientId") as String?) ?: ""
    val githubClientId  = (project.findProperty("githubClientId") as String?) ?: ""
    buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleClientId\"")
    buildConfigField("String", "GITHUB_CLIENT_ID",        "\"$githubClientId\"")
    buildConfigField("String", "GITHUB_REDIRECT_URI",     "\"${'$'}{...}\"")
}
```

```kotlin
// LoginRoute.kt
LoginConfig(
    googleServerClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID,
    githubClientId       = BuildConfig.GITHUB_CLIENT_ID,
    githubRedirectUri    = BuildConfig.GITHUB_REDIRECT_URI,
    /* … */
)
```

Pass the values at build time:
`-PgoogleServerClientId=… -PgithubClientId=… -PgithubRedirectUri=…`.

Two non-obvious rules:

- **Declare the fields in `defaultConfig`, so every build type gets them.** If
  you scope them to `debug { }`, the release build falls back to the empty
  default and *both social buttons silently read as "not set up" in release
  only* — debug looks fine, so the bug hides until you ship. There is nothing
  release-specific about these ids; both build types need the same value.
- **They must be present on the exact `assembleRelease` invocation.** A release
  APK built without `-PgoogleServerClientId=…` bakes in the empty string.

## The release-build gotcha (why a blank id can persist)

> Whenever you change any `-P`-injected `BuildConfig` value these ids come from
> (client ids, redirect uri, base url), rebuild with
> **`./gradlew clean :app:assembleRelease --no-build-cache -P…`** — or the old
> value can silently survive into the APK.

The reason, and why the usual R8 suspects are the wrong lead:

`buildConfigField("String", …)` generates a Java `public static final String`
constant. When Kotlin references such a constant it **inlines the literal at
compile time** — `BuildConfig.GOOGLE_SERVER_CLIENT_ID` becomes a `const-string`
baked directly into the compiled login method. At runtime nothing reads the
`BuildConfig` field; the value already lives in the call site's bytecode.

Two consequences follow, both verified against a real release APK
(`apkanalyzer dex code`): the login method loads the ids with `const-string`
literals and there is **no `sget-object …BuildConfig;->…` field read anywhere**.

1. **R8/ProGuard keep rules do not affect this.** `-keep class …BuildConfig { *; }`
   or disabling R8 full mode (`android.enableR8.fullMode=false`) neither cause
   nor fix a blank client id — the value never travels through a field access
   that R8 could strip or constant-propagate away. R8 runs *after* the Kotlin
   compiler has already inlined the literal. If you find such rules added "to
   fix sign-in," they are cargo-cult; the real lever is the build below.
2. **A stale inline can outlive a value change.** Because the id is inlined at
   compile time and the app's login source rarely changes, Gradle's incremental
   compilation plus the build cache (`org.gradle.caching=true`) can hand back a
   login class that was compiled against an *earlier* (often empty) id. Plain
   `./gradlew clean` is **not** enough — the build cache restores the stale
   compiled class (tell-tale: a "clean" release build that finishes in seconds
   instead of minutes). Only `clean … --no-build-cache` forces a fresh compile
   that inlines the current value.

So a blank-button report in release is almost never a keep-rules problem and
never needs a `consumer-rules.pro` for the ids. Check, in order: (1) was
`-Pgoogle…`/`-Pgithub…` actually passed to `assembleRelease`; (2) are the fields
in `defaultConfig` and not a `debug { }` block; (3) rebuild
`clean --no-build-cache` to clear any stale inline.
