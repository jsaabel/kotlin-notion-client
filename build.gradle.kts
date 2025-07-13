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

repositories {
    mavenCentral()
}

dependencies {
    // Main dependencies
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
    // Exclude integration and manual tests by default for fast unit test runs
    systemProperty("kotest.tags.exclude", "Integration | Manual")
}

// Task for running only integration tests
tasks.register<Test>("integrationTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    systemProperty("kotest.tags.include", "Integration") 
    group = "verification"
    description = "Runs integration tests against live Notion API (requires NOTION_API_TOKEN and NOTION_TEST_PAGE_ID)"
}

// Task for running all tests (unit + integration)
tasks.register<Test>("testAll") {
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    group = "verification"  
    description = "Runs all tests including integration tests"
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
