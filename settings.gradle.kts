pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies the correct Loom variant per Minecraft version (needed once 26.1+ is added)
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("me.modmuss50.mod-publish-plugin") version "2.2.1" apply false
}

stonecutter {
    create(rootProject) {
        // Add more Minecraft versions here, plus a matching table in stonecutter.properties.toml
        versions("1.21.11")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "ModTemplate"
