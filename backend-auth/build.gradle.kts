// Platform auth module — included by a parent project as a Gradle subproject.
//
// In the consuming project's settings.gradle(.kts):
//     includeBuild("platform-stack/backend-auth")   // or include + project path
// and depend on it:
//     implementation("com.danovich.platform:backend-auth")
//
// The parent supplies the Spring Boot BOM / plugin; this module only declares
// the libraries it directly needs, as `compileOnly` where the host app already
// brings them (Spring Web, servlet API) so versions don't collide.

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
    // jjwt is the one hard runtime dependency the library owns.
    api("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Provided by the host Spring Boot app; compile against, don't bundle.
    compileOnly("org.springframework:spring-web:6.1.11")
    compileOnly("org.springframework:spring-webmvc:6.1.11")
    compileOnly("org.springframework.boot:spring-boot:3.3.2")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.3.2")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test { useJUnitPlatform() }

// Publish the compiled jar + a POM (so consumers get jjwt transitively, but not
// the compileOnly Spring/servlet deps the host app already provides).
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
