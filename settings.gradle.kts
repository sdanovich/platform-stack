// Root build so platform-stack is independently buildable and publishable
// (`./gradlew publish` / `publishToMavenLocal`). Consuming projects no longer
// include these as source subprojects — they depend on the published artifacts.

// Plugin versions for the android-library module (:platform-login-ui), so its
// build.gradle.kts can apply the plugins without repeating versions. Pinned to
// match the BidHound app for a binary-compatible AAR. The java-library modules
// need none of this.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.library") version "8.5.0"
        id("org.jetbrains.kotlin.android") version "1.9.24"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    }
}

rootProject.name = "platform-stack"

include(":backend-auth")
include(":android-auth")
include(":backend-login")
include(":backend-notification")
include(":platform-login-ui")
