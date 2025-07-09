plugins {
//    id("java")
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
//    testImplementation(platform("org.junit:junit-bom:5.10.0"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.logging)
    implementation("io.ktor:ktor-client-cio-jvm:3.2.1") // TODO: Why is this not included in libs?
}

tasks.test {
    useJUnitPlatform()
}
