// Platform notification module — outbound user notifications (currently SMS via
// Twilio), published as com.danovich.platform:backend-notification and consumed
// by apps as a normal dependency:
//
//     implementation("com.danovich.platform:backend-notification:0.1.0-SNAPSHOT")
//
// Wiring is automatic: PlatformNotificationAutoConfiguration is registered as a
// Spring Boot auto-configuration, so a @SpringBootApplication picks up the
// SmsNotifier bean from properties under `platform.notification.*`.

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
    // The Twilio SDK is the one hard runtime dependency the library owns, exposed
    // transitively so consumers that also handle inbound webhooks get it too.
    api("com.twilio.sdk:twilio:10.1.5")

    // Provided by the host Spring Boot app; compile against, don't bundle.
    compileOnly("org.slf4j:slf4j-api:2.0.13")
    compileOnly("org.springframework.boot:spring-boot:3.3.2")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.3.2")
    // Redis Streams exchange (publisher + durable consumer). The host app brings the
    // runtime via spring-boot-starter-data-redis; we only compile against the API.
    compileOnly("org.springframework.data:spring-data-redis:3.3.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test { useJUnitPlatform() }

// Publish the compiled jar + a POM (so consumers get the Twilio SDK transitively,
// but not the compileOnly Spring deps the host app already provides).
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
