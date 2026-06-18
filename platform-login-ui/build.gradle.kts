// Platform login UI — published as com.danovich.platform:platform-login-ui (AAR).
//
// A parameterized Compose login experience (email + Google + GitHub) plus its
// view-model, session store, OAuth relay, and Retrofit auth contract. A consuming
// app injects branding + provider config + its own OkHttp-backed AuthApi, then
// renders PlatformLoginScreen(config). Versions mirror the BidHound app so the AAR
// is binary-compatible. Plugin versions come from settings.gradle.kts pluginManagement.

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    `maven-publish`
}

group = "com.danovich.platform"
// SNAPSHOT: published on every push to main so consumers tracking the snapshot
// pick up fixes automatically. Tagged releases override this with a fixed version.
version = "0.1.0-SNAPSHOT"

android {
    namespace = "com.danovich.platform.login.ui"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Retrofit/OkHttp/Moshi for the auth wire contract.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Social sign-in: Google via Credential Manager, GitHub via a Custom Tab.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.browser:browser:1.8.0")

    // Shared token store + interceptors from platform-stack (sibling module).
    implementation(project(":android-auth"))
}

// Publish the AAR + a POM. Plain artifact coordinates flow to consumers; the app
// provides its own OkHttp/Compose runtime (same versions as this module).
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
            }
        }
        repositories {
            // GitHub Packages for real cross-project consumption. Credentials come
            // from the environment (CI) or ~/.gradle/gradle.properties — never here.
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/sdanovich/platform-stack")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String?)
                    password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String?)
                }
            }
        }
    }
}
