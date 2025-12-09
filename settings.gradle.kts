pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "newin"

include("agent", "core")

project(":agent").name = "newin-agent"
project(":core").name = "newin-core"

