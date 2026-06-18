// Platform login module — published as com.danovich.platform:backend-login.
//
// User-facing auth on top of backend-auth's client-credentials JWT: email/password
// register+login, token refresh, and Google/GitHub social sign-in. Owns the User
// and RefreshToken entities + their schema. A consuming app supplies only config
// (oauth.*, platform.auth.*) and an optional UserCreatedCallback bean.

plugins {
    `java-library`
    `maven-publish`
}

group = "com.danovich.platform"
// SNAPSHOT: published on every push to main so consumers tracking the snapshot
// pick up fixes automatically. Tagged releases override this with a fixed version.
version = "0.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories { mavenCentral() }

dependencies {
    // The JWT service/filter/token-endpoint this layer builds on (transitive to
    // consumers, so depending on backend-login pulls backend-auth + jjwt too).
    api(project(":backend-auth"))

    // Google ID-token verification — the login module owns this dependency now.
    api("com.google.api-client:google-api-client:2.6.0")

    // Provided by the host Spring Boot app; compile against, don't bundle.
    compileOnly("org.springframework:spring-web:6.1.11")
    compileOnly("org.springframework:spring-webmvc:6.1.11")
    compileOnly("org.springframework:spring-context:6.1.11")
    compileOnly("org.springframework.boot:spring-boot:3.3.2")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.3.2")
    compileOnly("org.springframework.data:spring-data-jpa:3.3.2")
    compileOnly("org.springframework.security:spring-security-crypto:6.3.1")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.17.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test { useJUnitPlatform() }

// Publish the compiled jar + a POM (consumers get backend-auth + google-api-client
// transitively, but not the compileOnly Spring/JPA deps the host app provides).
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
