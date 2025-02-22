import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

group = "com.atty"
version = "0.12"

repositories {
    mavenCentral()
    maven (url = "https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("work.socialhub.kbsky:core:0.3.0")
    implementation("work.socialhub.kbsky:auth:0.3.0")
    implementation("work.socialhub.kbsky:stream:0.3.0")
    implementation("com.github.uakihir0:JLogger:1.4")
    implementation("com.github.uakihir0:JHttpClient:1.1.9")
    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("com.sshtools:jsixel:0.0.2")
    implementation("com.sshtools:jsixel-awt:0.0.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "15"
}

application {
    val port: String by project
    val logFile: String by project

    mainClass.set("com.atty.MainKt")

    applicationDefaultJvmArgs = listOf("-Dport=$port", "-DlogFile=$logFile")
}