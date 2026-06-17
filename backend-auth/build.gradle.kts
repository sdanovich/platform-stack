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
}

group = "com.danovich.platform"
version = "0.1.0"

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
