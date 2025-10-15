import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    application
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.maven.publish)
}

group = "it.saabel"
version = "0.1.0"

application {
    mainClass.set("it.saabel.kotlinnotionclient.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Main dependencies
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.logging)

    // Test dependencies
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.ktor.client.mock)
}

tasks.test {
    useJUnitPlatform()
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    // ignore release candidates when checking for new gradle version
    gradleReleaseChannel = "current"

    // ignore release candidates as upgradable versions from stable versions for all other gradle dependencies
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

// Configure Java plugin for JVM toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Vanniktech Maven Publish Plugin Configuration
mavenPublishing {
    // Publish to Maven Central Portal (new system)
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)

    // Sign all publications with GPG
    signAllPublications()

    // Configure POM metadata
    coordinates("it.saabel", "kotlin-notion-client", "0.1.0")

    pom {
        name.set("Kotlin Notion Client")
        description.set("A modern, type-safe Kotlin client for the Notion API with comprehensive DSL support and coroutine-based operations")
        url.set("https://github.com/jsaabel/kotlin-notion-client")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("jsaabel")
                name.set("Jonas Saabel")
                email.set("jonas@saabel.it")
                url.set("https://saabel.it")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/jsaabel/kotlin-notion-client.git")
            developerConnection.set("scm:git:ssh://github.com/jsaabel/kotlin-notion-client.git")
            url.set("https://github.com/jsaabel/kotlin-notion-client")
        }
    }
}
