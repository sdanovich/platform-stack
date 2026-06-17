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
}

group = "com.danovich.platform"
version = "0.1.0"

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
