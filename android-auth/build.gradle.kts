// Platform Android auth module — included as a library subproject.
//
// Consuming Android project: include this module and depend on it, e.g.
//     implementation(project(":platform-auth"))
// then build an OkHttpClient with the interceptors (see the platform-auth skill).
//
// This is a plain Java library (no Android framework types used), so it builds as
// a java-library rather than an android-library — keeps it light and testable.

plugins {
    `java-library`
    `maven-publish`
}

group = "com.danovich.platform"
// SNAPSHOT: published on every push to main so consumers tracking the snapshot
// pick up fixes automatically. Tagged releases override this with a fixed version.
version = "0.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories { mavenCentral() }

dependencies {
    // OkHttp is the transport the interceptors plug into; provided by the app.
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")
    compileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.test { useJUnitPlatform() }

// Publish the compiled jar + POM. OkHttp/annotations stay compileOnly (not in the
// POM), so the consuming app provides OkHttp — it already bundles it.
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
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
        // `publishToMavenLocal` (~/.m2) is always available with no credentials,
        // used for local end-to-end verification before anything is pushed.
    }
}
