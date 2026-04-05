# Fabric 26.1 build dependencies

The Fabric module targets **Minecraft 26.1** with **official** mappings. Some artifacts are **snapshots** or built from source.

## PacketEvents (`com.github.retrooper:packetevents-api:2.12.0+7d7c846-SNAPSHOT`)

- Gradle tries **`https://repo.grim.ac/snapshots`**, then **`mavenLocal()`** last so unpublished versions can be resolved after `publishToMavenLocal` in the PacketEvents tree.
- Optional: **`./gradlew -PMAVEN_LOCAL_OVERRIDE=true`** also prepends `mavenLocal()` inside `exclusive()` helpers (see `buildSrc/exclusive.kt`) for other modules.
- Default builds pin exact snapshot coordinates for reproducibility; override behavior is opt-in via local properties/`MAVEN_LOCAL_OVERRIDE`.
- The Fabric module compiles against `packetevents-api` by default.
- Full PE Fabric runtime jars are validated via harness scripts in `tests/scripts` so defaults stay stable while local testing remains explicit.
- To populate local cache only:

  ```bash
  git clone https://github.com/retrooper/packetevents.git
  cd packetevents && ./gradlew publishToMavenLocal
  ```

## Loader

Use **Fabric Loader 0.18.5+** (see `fabric/gradle.properties` and `libs.versions.toml`).
