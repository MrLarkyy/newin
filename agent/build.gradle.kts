plugins {
    id("java-library")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.0")
}

tasks.jar {
    manifest {
        attributes(
            "Agent-Class" to "lol.farsight.newin.agent.AgentEntrypoint",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
        )
    }
}

val agentJar = tasks.named<Jar>("jar")

configurations {
    create("agentJarElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(
                Usage.USAGE_ATTRIBUTE,
                objects.named(Usage.JAVA_RUNTIME)
            )
        }
        outgoing.artifact(agentJar)
    }
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
                classifier = "agent"
            }
        }
    }
}
