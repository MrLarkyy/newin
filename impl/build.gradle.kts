plugins {
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.2.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

dependencies {
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")

    implementation("org.ow2.asm:asm:9.9")
    implementation(project(":newin-api"))
}

tasks.jar {
    manifest {
        attributes(
            "Agent-Class" to "farsight.lol.agent.AgentEntrypoint",
            "Can-Retransform-Classes" to true,
            "Can-Redefine-Classes" to true
        )
    }
}

tasks {
    runServer {
        minecraftVersion("1.21.10")
    }

    shadowJar {
        archiveFileName.set("Newin-${project.version}.jar")
        archiveClassifier.set("")
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}