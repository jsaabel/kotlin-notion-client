import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    application
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinter)
}

group = "no.saabelit"
version = "0.0.1-SNAPSHOT"

application {
    mainClass.set("no.saabelit.kotlinnotionclient.MainKt")
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
