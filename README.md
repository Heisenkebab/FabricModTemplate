# ModTemplate

A Fabric mod template for building one mod for several Minecraft versions (currently 1.21.11 and 26.3) from a single codebase, using [Stonecutter](https://stonecutter.kikugie.dev/). Releases are published to Modrinth and CurseForge by GitHub Actions.

## Requirements

- Any JDK to start Gradle. Gradle runs on Java 25 and downloads the right JDK for each Minecraft version (21 for 1.21.x, 25 for 26.x) if needed.
- IntelliJ IDEA with the Minecraft Development and Stonecutter plugins is recommended.

## Project layout

| Path | Purpose |
| --- | --- |
| `src/main` | Common code: `TemplateMain`, common mixins, `fabric.mod.json` |
| `src/client` | Client-only code: `TemplateMainClient`, data generation, client mixins |
| `stonecutter.properties.toml` | Mod metadata, dependency versions and per-version settings |
| `settings.gradle.kts` | Plugin versions and the list of supported Minecraft versions |
| `stonecutter.gradle.kts` | The active version and Stonecutter swaps |
| `build.gradle.kts` | Build logic for each version, including publishing |
| `versions/<mc>/` | Generated per-version build directories |
| `run/` | Shared run directory for all versions |

## Making it your own

1. In `stonecutter.properties.toml`, set `mod.id`, `mod.name`, `mod.group` and `mod.version`.
2. Rename the `net.heisenkebab.template` package and the entrypoint classes, then update `entrypoints` in `fabric.mod.json`.
3. Rename `template.mixins.json` and `template.client.mixins.json` to match your mod id, and update the `package` inside them and the `mixins` list in `fabric.mod.json`.
4. Add an icon at `src/main/resources/assets/<mod id>/icon.png` and update `icon` in `fabric.mod.json`.
5. Fill in `description`, `authors`, `contact` and the copyright holder in `LICENSE`.

## Building

```sh
./gradlew build             # build every version
./gradlew buildAndCollect   # build and copy all jars to build/libs/<mod version>/
./gradlew runClient         # run the active version (see below)
```

## Multiple Minecraft versions

The source code is written for the **active** version set in `stonecutter.gradle.kts`. Code that differs between versions uses Stonecutter comments:

```java
//? if >=1.21.5 {
newApiCall();
//?} else
/*oldApiCall();*/
```

Switch the active version with the `Set active project to …` Gradle tasks, or the Stonecutter IntelliJ plugin. This rewrites those comments in the sources. Run `Reset active project` before committing, so the sources are back on `vcsVersion` (currently `1.21.11`).

The swaps `/*$ mod_version*/` and `/*$ minecraft*/` insert the mod version and the current Minecraft version into Java sources.

### Adding a version

1. Add it to `versions(...)` in `settings.gradle.kts`.
2. Add a matching table to `stonecutter.properties.toml` with `mod.mc_compat`, `mod.mc_releases` and `deps.fabric_api`. Look up matching versions at <https://fabricmc.net/develop>.

```toml
["1.21.10"]
mod.mc_compat = "1.21.10"
mod.mc_releases = ["1.21.10"]
deps.fabric_api = "..."
```

- `mod.mc_compat` is the Minecraft version range written into `fabric.mod.json`.
- `mod.mc_releases` lists the game versions the upload is tagged with on Modrinth and CurseForge.

## Kotlin (optional)

Kotlin is off by default. To use it, set `mod.kotlin = true` in `stonecutter.properties.toml` and put Kotlin sources in `src/main/kotlin` and `src/client/kotlin`. Java and Kotlin can be mixed; keep mixins in Java.

When enabled, the build applies the Kotlin plugin, depends on `fabric-language-kotlin` (added to `fabric.mod.json` and to the Modrinth/CurseForge uploads), and targets the same Java version as each Minecraft version. Stonecutter comments work in `.kt` files too.

To update Kotlin, bump `deps.fabric_kotlin` and set the Kotlin plugin version in `settings.gradle.kts` to the Kotlin version in its name (e.g. `1.14.2+kotlin.2.4.21` → `2.4.21`).

## Publishing

Publishing uses [mod-publish-plugin](https://modmuss50.github.io/mod-publish-plugin/) and uploads one file for each Minecraft version, plus its sources jar.

### Setup

1. Put your project IDs in `stonecutter.properties.toml`. A platform is skipped while its ID is empty.
   ```toml
   publish.modrinth_id = "AbCdEf12"
   publish.curseforge_id = "123456"
   ```
2. Add the repository secrets `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` under **Settings → Secrets and variables → Actions**. Never commit tokens.

### Releasing

1. Bump `mod.version` in `stonecutter.properties.toml` and commit. The version uploaded comes from this setting, not from the release tag.
2. Create a GitHub release. Its description becomes the changelog.
3. The `Publish` workflow builds and uploads every version. You can also start it by hand from the Actions tab.

The release type follows `mod.version`: versions containing `alpha` are marked alpha, `beta` or `rc` are marked beta, and anything else is stable.

### Publishing locally

Export the tokens and run the task:

```sh
export MODRINTH_TOKEN=...
export CURSEFORGE_TOKEN=...
CHANGELOG="Fixed things" ./gradlew publishMods
```

Without either token, `publishMods` does a dry run: it prints what it would upload and uploads nothing.

## License

MIT, see [LICENSE.txt](LICENSE).
