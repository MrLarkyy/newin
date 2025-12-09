plugins {
    id("java-library")
    `maven-publish`
}

repositories {
    mavenCentral()
}

val agentJar by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.0")

    implementation("org.ow2.asm:asm:9.9")
    implementation("org.ow2.asm:asm-commons:9.9")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.slf4j:slf4j-simple:2.0.17")

    implementation("com.google.guava:guava:33.5.0-jre")

    implementation(project(":newin-agent"))

    agentJar(project(":newin-agent", configuration = "agentJarElements"))
}

val embeddedDir = layout.buildDirectory.dir("generated/resources/")

val copyAgentJar by tasks.registering(Copy::class) {
    from(agentJar)
    rename { "agent.jar" }
    into(embeddedDir)
}

sourceSets.main {
    resources.srcDir(embeddedDir)
}

tasks.processResources {
    dependsOn(copyAgentJar)
}

val maven_username = if (env.isPresent("MAVEN_USERNAME")) env.fetch("MAVEN_USERNAME") else ""
val maven_password = if (env.isPresent("MAVEN_PASSWORD")) env.fetch("MAVEN_PASSWORD") else ""

publishing {
    repositories {
        maven {
            name = "aquaticRepository"
            url = uri("https://repo.nekroplex.com/releases")

            credentials {
                username = maven_username
                password = maven_password
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "lol.farsight"
            artifactId = "newin"
            version = "${project.version}"
            from(components["java"])
            artifact(tasks.jar) {
                classifier = "core"
            }
        }
    }
}