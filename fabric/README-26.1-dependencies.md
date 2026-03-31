# Fabric 26.1 build dependencies

The Fabric module targets **Minecraft 26.1** with **official** mappings. Some artifacts are **snapshots** or built from source.

## PacketEvents (`com.github.retrooper:packetevents-*:2.12.0-SNAPSHOT`)

- Gradle tries **`https://repo.grim.ac/snapshots`**, then **`mavenLocal()`** last so unpublished versions can be resolved after `publishToMavenLocal` in the PacketEvents tree.
- Optional: **`./gradlew -PMAVEN_LOCAL_OVERRIDE=true`** also prepends `mavenLocal()` inside `exclusive()` helpers (see `buildSrc/exclusive.kt`) for other modules.
- To populate local cache only:

  ```bash
  git clone https://github.com/retrooper/packetevents.git
  cd packetevents && ./gradlew publishToMavenLocal
  ```

## cloud-fabric (`org.incendo:cloud-fabric:2.0.0-SNAPSHOT`)

- Resolved from **Sonatype Maven Snapshots** (`central.sonatype.com/repository/maven-snapshots/`) for group `org.incendo`, and/or
- Publish locally from [cloud-minecraft-modded](https://github.com/Incendo/cloud-minecraft-modded): `./gradlew :cloud-fabric:publishToMavenLocal`, then build Grim with **`-PMAVEN_LOCAL_OVERRIDE=true`**.

## Loader

Use **Fabric Loader 0.18.5+** (see `fabric/gradle.properties` and `libs.versions.toml`).
