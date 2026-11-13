plugins {
    // Applies the correct Loom variant based on the Minecraft version
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

// Don't set group here: Stonecutter version projects must not share coordinates
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val useKotlin = sc.properties.get<Boolean>("mod.kotlin")
if (useKotlin) apply(plugin = "org.jetbrains.kotlin.jvm")

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    else -> JavaVersion.VERSION_1_8
}

repositories {
    // Add repositories for other mods you depend on here
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    // Keep using mod* configurations even on 26.1+, loom-back-compat converts them
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
    if (useKotlin) modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.fabric_kotlin")}")
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runConfigs.all {
        generateRunConfig = true
        // Share one run directory between all versions
        runDirectory = rootProject.file("run")
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

if (useKotlin) extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(requiredJava.majorVersion)
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to project.version.toString(),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "loader" to sc.properties.get<String>("deps.fabric_loader"),
        )
        props.forEach { (key, value) -> inputs.property(key, value) }
        inputs.property("kotlin", useKotlin)
        filesMatching("fabric.mod.json") {
            expand(props)
            // Adds the fabric-language-kotlin dependency after the fabric-api line
            if (useKotlin) filter { line ->
                if ("\"fabric-api\"" in line) "$line\n    \"fabric-language-kotlin\": \"*\"," else line
            }
        }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        inputs.property("java", mixinJava)
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    named<ProcessResources>("processClientResources") {
        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        inputs.property("java", mixinJava)
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    withType<Jar>().configureEach {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from(rootProject.file("LICENSE")) { rename { "LICENSE_$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies them to build/libs/{mod version}/"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
    }
}

publishMods {
    val modVersion = property("mod.version") as String
    file = loomx.modJar.flatMap { it.archiveFile }
    additionalFiles.from(loomx.modSourcesJar.flatMap { it.archiveFile })
    displayName = "${property("mod.name")} $modVersion for ${sc.current.version}"
    version = project.version.toString()
    changelog = providers.environmentVariable("CHANGELOG").orElse("")
    type = when {
        "alpha" in modVersion -> ALPHA
        "beta" in modVersion || "rc" in modVersion -> BETA
        else -> STABLE
    }
    modLoaders.add("fabric")
    // Uploads nothing unless the token for each platform is set
    dryRun = providers.environmentVariable("MODRINTH_TOKEN").orNull == null
        && providers.environmentVariable("CURSEFORGE_TOKEN").orNull == null

    val mcReleases = sc.properties.raw("mod", "mc_releases").asList().map { it.asPrimitive().content.toString() }

    // Platforms are skipped while their project id is left empty in stonecutter.properties.toml
    val modrinthId = sc.properties.get<String>("publish.modrinth_id")
    if (modrinthId.isNotBlank()) modrinth {
        projectId = modrinthId
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(mcReleases)
        requires("fabric-api")
        if (useKotlin) requires("fabric-language-kotlin")
    }

    val curseforgeId = sc.properties.get<String>("publish.curseforge_id")
    if (curseforgeId.isNotBlank()) curseforge {
        projectId = curseforgeId
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(mcReleases)
        javaVersions.add(requiredJava)
        client = true
        server = true
        requires("fabric-api")
        if (useKotlin) requires("fabric-language-kotlin")
    }
}
