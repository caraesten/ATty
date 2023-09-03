import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

group = "me.carahurtle"
version = "0.4"

repositories {
    mavenCentral()
    maven (url = "https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-network:2.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.github.uakihir0:bsky4j:0.5.2") {
        exclude("com.github.uakihir0", "JLogger")
        exclude("com.github.uakihir0", "JHttpClient")
    }
    implementation("com.github.uakihir0:JLogger:1.4")
    implementation("com.github.uakihir0:JHttpClient:1.1.9")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    val port: String by project
    val logFile: String by project

    mainClass.set("com.atty.MainKt")

    applicationDefaultJvmArgs = listOf("-Dport=$port", "-DlogFile=$logFile")
}