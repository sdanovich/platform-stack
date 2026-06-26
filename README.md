# platform-stack

Shared foundation code used across projects (NearMe, BidHound, and new ones),
consumed as a **git submodule** rather than published artifacts. Extracting it
once means a fix lands in one place and every project adopts it on purpose
(`git submodule update --remote`), instead of the same code being re-derived —
and sometimes lost — per project.

## Modules

| Module                 | Language | What it is                                                        |
| ---------------------- | -------- | ----------------------------------------------------------------- |
| `backend-auth`         | Java     | Client-credentials JWT for Spring Boot: service, filter, endpoint, autoconfig |
| `backend-login`        | Java     | Per-user accounts: email/social sign-in, refresh, `@CurrentUser`  |
| `backend-notification` | Java     | Outbound notification contract (publish → gate → Redis stream → deliver via SMS) |
| `android-auth`         | Java     | OkHttp interceptors + token store for the auth scheme on Android  |
| `platform-login-ui`    | Kotlin   | Parameterized Compose sign-in UI + `AuthApi` contract            |
| `skill/platform-auth`  | —        | The integration recipe (how to wire the modules into a project)   |

## backend-notification — the contract

A reusable, app-agnostic notification pipeline. An app **publishes** a message; the module
**delivers** it. The module never knows *why* a message was sent.

- **`NotificationRequest(recipientRef, message)`** — what an app publishes.
- **`NotificationGate`** *(app implements)* — `destinationIfEnabled(recipientRef)`: is this
  recipient enabled, and where to? Empty = ignore. A permissive default is used if the app
  declares none.
- **`NotificationPublisher`** *(module)* — applies the gate **at publish** and, if enabled,
  XADDs a self-contained `{to, text}` record onto a **Redis stream**.
- **`Notifier`** *(module, swappable)* — the transport: `TextBeeNotifier` or `SmsNotifier`
  (Twilio), chosen by `platform.notification.sms.provider`.
- **`NotificationStreamConsumer`** *(module)* — a durable consumer group (at-least-once)
  that reads the stream and delivers via the `Notifier`. Because the gate already ran at
  publish, the consumer is a pure deliverer and can run as a standalone service.

Wiring is automatic via `PlatformNotificationAutoConfiguration` once a `RedisConnectionFactory`
is present; configure under `platform.notification.*` (see `NotificationProperties`).

## Using it in a project

Add as a submodule, then follow the `platform-auth` skill for the wiring:

```bash
git submodule add https://github.com/sdanovich/platform-stack vendor/platform-stack
git submodule update --init --recursive
```

The skill (`skill/platform-auth/SKILL.md`) is the authoritative step-by-step for
backend and Android wiring, including the gotchas that bite during integration
(token endpoint must be public, jwt-secret >= 32 bytes, interceptor order).

## The library/project boundary

The library owns stable code with no per-project variation. Each project owns
only what depends on its own config and HTTP client: the secrets/properties, the
`TokenProvider` implementation, and the client assembly. If you ever need to fork
a library file into a project to tweak it, add a configuration point to the
library instead — that keeps one shared implementation.

## Building / testing

Each module is a `java-library` Gradle subproject. The consuming project's build
includes them; there's no standalone publish step. Unit tests live under each
module's `src/test` (JUnit 5; the Android module uses OkHttp MockWebServer to
assert interceptor behavior). Run them via the consuming project's Gradle, or a
thin root build that includes both modules.
# platform-stack
