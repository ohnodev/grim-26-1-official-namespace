val minecraft_version: String by project
val fabric_version: String by project

plugins {
    `maven-publish`
    alias(libs.plugins.fabric.loom)
    grim.`base-conventions`
    grim.`jij-conventions`
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    compileOnly("me.lucko:fabric-permissions-api:0.7.0")

    // cloud-fabric 2.0.0-SNAPSHOT (26.1 / official mappings): Sonatype snapshots below, or publish
    // Incendo cloud-minecraft-modded to mavenLocal — see fabric/README-26.1-dependencies.md
    implementation("org.incendo:cloud-fabric:2.0.0-SNAPSHOT") {
        exclude(group = "net.fabricmc.fabric-api")
        exclude(group = "net.fabricmc", module = "fabric-loader")
    }

    implementation(libs.fabric.loader)

    // PacketEvents 2.12.x for MC 26.1: repo.grim.ac/snapshots and/or mavenLocal — see README
    implementation("com.github.retrooper:packetevents-fabric:2.12.0-SNAPSHOT") {
        exclude(group = "net.fabricmc.fabric-api")
        exclude(group = "net.fabricmc", module = "fabric-loader")
    }
    compileOnly("com.github.retrooper:packetevents-fabric-common:2.12.0-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-fabric-official:2.12.0-SNAPSHOT") {
        exclude(group = "net.fabricmc.fabric-api")
        exclude(group = "net.fabricmc", module = "fabric-loader")
    }

    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")

    implementation(project(":common"))
}

// Remote-first resolution; mavenLocal last (or only when MAVEN_LOCAL_OVERRIDE) so CI/dev machines
// don’t silently pick stale local artifacts. PacketEvents/cloud snapshot sources: README-26.1-dependencies.md
repositories {
    exclusive("https://maven.fabricmc.net/") {
        includeGroup("net.fabricmc")
        includeGroup("net.fabricmc.fabric-api")
    }

    exclusive("https://repo.grim.ac/snapshots") {
        includeGroup("ac.grim.grimac")
    }
    maven("https://repo.grim.ac/snapshots") {
        mavenContent { snapshotsOnly() }
        content {
            includeGroup("com.github.retrooper")
        }
    }

    exclusive("https://jitpack.io", { mavenContent { releasesOnly() } }) {
        includeGroup("com.github.Fallen-Breath.conditional-mixin")
    }

    exclusive("https://repo.viaversion.com", { mavenContent { releasesOnly() } }) {
        includeGroup("com.viaversion")
    }

    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }

    exclusive("https://repo.opencollab.dev/maven-releases/", { mavenContent { releasesOnly() } }) {
        includeGroup("org.geysermc.api")
    }

    exclusive("https://repo.opencollab.dev/maven-snapshots/", { mavenContent { snapshotsOnly() } }) {
        includeGroup("org.geysermc.floodgate")
        includeGroup("org.geysermc.cumulus")
        includeModule("org.geysermc", "common")
        includeModule("org.geysermc", "geyser-parent")
    }

    exclusive(mavenCentral()) { includeGroup("me.lucko") }

    mavenCentral()

    maven("https://central.sonatype.com/repository/maven-snapshots/") {
        mavenContent { snapshotsOnly() }
        content {
            includeGroup("org.incendo")
        }
    }

    // Last: local publish (e.g. ./gradlew publishToMavenLocal in packetevents) when remotes lack the jar
    mavenLocal()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["jar"])
}

tasks {
    jar {
        archiveBaseName.set("${rootProject.name}-fabric")
        archiveVersion.set(rootProject.version as String)
    }
}
