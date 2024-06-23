plugins {
    kotlin("jvm") version "1.9.24"
    application
}

group = "dev.idot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.yaml:snakeyaml:2.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}