# platform-stack

Shared foundation code used across projects (NearMe, BidHound, and new ones),
consumed as a **git submodule** rather than published artifacts. Extracting it
once means a fix lands in one place and every project adopts it on purpose
(`git submodule update --remote`), instead of the same code being re-derived —
and sometimes lost — per project.

## Modules

| Module          | Language | What it is                                                        |
| --------------- | -------- | ----------------------------------------------------------------- |
| `backend-auth`  | Java     | Client-credentials JWT for Spring Boot: service, filter, endpoint, autoconfig |
| `android-auth`  | Java     | OkHttp interceptors + token store for the same scheme on Android  |
| `skill/platform-auth` | —  | The integration recipe (how to wire the modules into a project)   |

First cut is **auth only**. Tunnel discovery and notify/SMS are candidates for
later extraction once a third consumer makes the right seams obvious.

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
