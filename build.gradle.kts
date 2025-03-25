plugins {
    id("fabric-loom") version("1.9-SNAPSHOT")
}

version = "${rootProject.extra["mod_version"]}"
group = "${rootProject.extra["maven_group"]}.mod.fabric"

base {
    archivesName = "${project.extra["archives_base_name"]}"
}

loom {
    accessWidenerPath = file("src/main/resources/civianmod.accesswidener")
    runConfigs.configureEach {
        this.programArgs += "--username LocalModTester".split(" ")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.extra["minecraft_version"]}")
    loom {
        @Suppress("UnstableApiUsage")
        mappings(layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${project.extra["parchment_minecraft_version"]}:${project.extra["parchment_mappings_version"]}@zip")
        })
    }

    modImplementation("net.fabricmc:fabric-loader:${project.extra["fabric_loader_version"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.extra["fabric_api_version"]}")

    modImplementation("dev.isxander:yet-another-config-lib:${project.extra["yacl_version"]}")
    modImplementation("com.terraformersmc:modmenu:${project.extra["modmenu_version"]}")

    // This is literally only here to make Minecraft SHUT UP about non-signed messages while testing.
    // https://modrinth.com/mod/no-chat-reports/version/Fabric-1.21.4-v2.11.0
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
            rename { "LICENSE_${project.extra["mod_name"]}" }
        }
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }
    processResources {
        filesMatching("fabric.mod.json") {
            expand(
                "mod_name" to project.extra["mod_name"],
                "mod_version" to project.version,
                "mod_description" to project.extra["mod_description"],
                "copyright_licence" to project.extra["copyright_licence"],

                "mod_home_url" to project.extra["mod_home_url"],
                "mod_source_url" to project.extra["mod_source_url"],
                "mod_issues_url" to project.extra["mod_issues_url"],

                "minecraft_version" to project.extra["minecraft_version"],
                "fabric_loader_version" to project.extra["fabric_loader_version"],

                "modmenu_version" to project.extra["modmenu_version"],
                "yacl_version" to project.extra["yacl_version"],
            )
            filter {
                it.replace(
                    "\"%FABRIC_AUTHORS_ARRAY%\"",
                    groovy.json.JsonBuilder("${project.extra["mod_authors"]}".split(",")).toString()
                )
            }
        }
        filesMatching("assets/civianmod/lang/en_us.json") {
            expand("mod_name" to project.extra["mod_name"])
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
        rename("(.*?)\\.jar", "\$1-fabric.jar")
    }
    build {
        dependsOn(getByName("copyJar"))
    }
}
