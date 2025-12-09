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

// we don't need to publish to maven here
