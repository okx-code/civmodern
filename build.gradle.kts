plugins {
    id("fabric-loom") version("1.9-SNAPSHOT")
}

private val mod_name = "CivianMod"
private val mod_version = "1.21.4-6"
private val mod_group = "uk.protonull.civianmod"

private val mod_description = "Civ utilities"
private val mod_authors = listOf("Okx", "Protonull")
private val mod_copyright = "GPLv3"
private val mod_home_url = "https://github.com/Protonull/CivianMod"
private val mod_source_url = "https://github.com/Protonull/CivianMod"
private val mod_issues_url = "https://github.com/Protonull/CivianMod/issues"

private val dep_minecraft_version = "1.21.4"
// https://parchmentmc.org/docs/getting-started
private val dep_parchment_minecraft_version = "1.21.4"
private val dep_parchment_mappings_version = "2025.03.23"
// https://fabricmc.net/versions.html
private val dep_fabric_loader_version = "0.16.10"
private val dep_fabric_api_version = "0.119.2+1.21.4"
// https://maven.isxander.dev/#/releases/dev/isxander/yet-another-config-lib/
// https://modrinth.com/mod/yacl/versions?c=release&l=fabric
private val dep_yacl_version = "3.6.6+1.21.4-fabric"
// https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/
// https://modrinth.com/mod/modmenu/versions?c=release&l=fabric
private val dep_modmenu_version = "13.0.3"

version = mod_version
group = "$mod_group.mod.fabric"

base {
    archivesName = mod_name
}

loom {
    accessWidenerPath = file("src/main/resources/civianmod.accesswidener")
    runConfigs.configureEach {
        this.programArgs += "--username LocalModTester".split(" ")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$dep_minecraft_version")
    loom {
        @Suppress("UnstableApiUsage")
        mappings(layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-$dep_parchment_minecraft_version:$dep_parchment_mappings_version@zip")
        })
    }

    modImplementation("net.fabricmc:fabric-loader:$dep_fabric_loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$dep_fabric_api_version")

    modImplementation("dev.isxander:yet-another-config-lib:$dep_yacl_version")
    modImplementation("com.terraformersmc:modmenu:$dep_modmenu_version")

    // This is literally only here to make Minecraft SHUT UP about non-signed messages while testing.
    // https://modrinth.com/mod/no-chat-reports/versions?c=release&l=fabric
    modLocalRuntime("maven.modrinth:no-chat-reports:9xt05630")
}

repositories {
    maven(url = "https://maven.parchmentmc.org") {
        name = "ParchmentMC"
    }
    maven(url = "https://api.modrinth.com/maven") {
        name = "Modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }
    // For YACL
    maven("https://maven.isxander.dev/releases/") {
        name = "Xander Maven"
    }
    // For ModMenu
    maven(url = "https://maven.terraformersmc.com/") {
        name = "Terraformers"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    jar {
        from(file("LICENSE.txt")) {
            rename { "LICENSE_$mod_name" }
        }
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }
    processResources {
        filesMatching("fabric.mod.json") {
            expand(
                "mod_name" to mod_name,
                "mod_version" to mod_version,
                "mod_description" to mod_description,
                "mod_copyright" to mod_copyright,

                "mod_home_url" to mod_home_url,
                "mod_source_url" to mod_source_url,
                "mod_issues_url" to mod_issues_url,

                "minecraft_version" to dep_minecraft_version,
                "fabric_loader_version" to dep_fabric_loader_version,

                "modmenu_version" to dep_modmenu_version,
                "yacl_version" to dep_yacl_version,
            )
            filter {
                it.replace(
                    "\"%FABRIC_AUTHORS_ARRAY%\"",
                    groovy.json.JsonBuilder(mod_authors).toString()
                )
            }
        }
        filesMatching("assets/civianmod/lang/*.json") {
            expand(
                "mod_name" to mod_name,
            )
        }
    }
    register<Delete>("cleanJar") {
        delete(fileTree("./dist") {
            include("*.jar")
        })
    }
    register<Copy>("copyJar") {
        dependsOn(getByName("cleanJar"))
        from(getByName("remapJar"))
        into("./dist")
    }
    build {
        dependsOn(getByName("copyJar"))
    }
}
