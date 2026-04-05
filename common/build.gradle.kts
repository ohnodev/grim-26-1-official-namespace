import versioning.BuildConfig

plugins {
    `maven-publish`
    grim.`base-conventions`
}

repositories {
    exclusive("https://repo.grim.ac/snapshots") {
        includeGroup("ac.grim.grimac")
    }
    // PacketEvents snapshots: same host but not exclusiveContent-locked so mavenLocal() can still resolve
    maven("https://repo.grim.ac/snapshots") {
        mavenContent { snapshotsOnly() }
        content {
            includeGroup("com.github.retrooper")
        }
    }

    // ViaVersion
    exclusive("https://repo.viaversion.com", { mavenContent { releasesOnly() } }) {
        includeGroup("com.viaversion")
    }

    // Configuralize
    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }

    // Cumulus
    exclusive("https://repo.opencollab.dev/maven-releases/", { mavenContent { releasesOnly() } }) {
        includeGroup("org.geysermc.api")
    }

    mavenCentral()

    // Optional local fallback: only when explicitly enabled via MAVEN_LOCAL_OVERRIDE.
    if (BuildConfig.mavenLocalOverride) {
        mavenLocal()
    }
}


dependencies {
    if (BuildConfig.shadePE) {
        api(libs.packetevents.api)
    } else {
        compileOnly(libs.packetevents.api)
    }
    api(libs.configuralize) {
        artifact {
            classifier = "slim"
        }
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    api(libs.guava)
    api(libs.gson)
    // Bump snakeyaml (transitive dep of configuralize) 1.29 -> 2.2+ for geyser-fabric
    api(libs.snakeyaml)
    api(libs.fastutil)
    api(libs.adventure.text.minimessage)
    api(libs.jetbrains.annotations)
    api(libs.hikaricp)

    api(libs.grim.api)
    api(libs.grim.internal)
    compileOnly(libs.grim.internal.shims)

    compileOnly(libs.geyser.base.api) {
        isTransitive = false // messes with guava otherwise
    }

    compileOnly(libs.checker.qual)
    compileOnly(libs.jsr305)
    compileOnly(libs.viaversion)
    compileOnly(libs.netty)
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}
