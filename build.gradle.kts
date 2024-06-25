plugins {
    kotlin("jvm") version "1.9.24"
    application
    `maven-publish`
}

group = "dev.idot"
version = "1.0-SNAPSHOT"

fun RepositoryHandler.local() {
    maven(file("D:/AppData/Maven/repository"))
}

repositories {
    local()
    mavenCentral()
}

dependencies {
    implementation("org.yaml:snakeyaml:2.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications.create<MavenPublication>("plugin") {
        artifact(
            tasks.register("mainJar", Jar::class) {
                archiveClassifier.set("")
                from(sourceSets["main"].output)
            }.get()
        )
        artifact(
            tasks.register("sourceJar", Jar::class) {
                archiveClassifier.set("sources")
                from(sourceSets["main"].allSource)
            }.get()
        )
    }
    repositories {
        local()
    }
}

kotlin {
    jvmToolchain(8)
}