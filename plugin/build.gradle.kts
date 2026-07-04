import org.gradle.internal.extensions.core.serviceOf
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.service.DownloadsAPIService.Companion.folia

plugins {
    id("com.gradleup.shadow") version "9.2.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    maven("https://repo.okaeri.cloud/releases")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io/")
}

dependencies {
    implementation(project(":api"))
    rootProject.subprojects.filter { it.path.startsWith(":nms:nms_") }.forEach(::implementation)
    compileOnly("io.papermc.paper:paper-api:${project.findProperty("paper-api.version.base")}")
    implementation("eu.okaeri:okaeri-configs-yaml-bukkit:6.1.0-beta.1")
    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.17")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.17")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("com.github.Jikoo:OpenInv:5.3.0")
    implementation("org.sql2o:sql2o:1.9.1")
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

val supportVersions = listOf(
    "paper-1.21.11",
    "paper-26.1.1",
    "paper-26.1.2",
    "paper-26.2",
    "folia-1.21.11",
    "folia-26.1.2",
)

tasks {
    supportVersions.forEach { supportVersion ->
        val (platform, version) = supportVersion.split("-", limit = 2)
        register<xyz.jpenilla.runpaper.task.RunServer>(supportVersion) {
            group = "run"
            runDirectory(layout.projectDirectory.file("run/$supportVersion").asFile)
            minecraftVersion(version)
            if (platform == "folia") {
                val progressLoggerFactory = project.serviceOf<ProgressLoggerFactory>()
                serverJar(folia(project).get().resolveBuild(progressLoggerFactory,version, DownloadsAPIService.Build.Latest).toFile())
            }
            pluginJars(shadowJar.flatMap { it.archiveFile })
            doFirst {
                layout.projectDirectory.file("run/$supportVersion/eula.txt").asFile.apply {
                    parentFile.mkdirs()
                    writeText("eula=true")
                }
            }
        }
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

val platformVersions = supportVersions
    .map { it.split("-", limit = 2) }
    .groupBy({ it[0].replaceFirstChar { c -> c.uppercase() } }, { it[1] })
    .mapValues { (_, versions) -> versions.distinct().sortedDescending().joinToString(", ") }

tasks.jar {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("original")
    archiveVersion.set(project.version.toString())
}

tasks.shadowJar {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    relocate("eu.okaeri", "${project.group}.libs.okaeri")
    manifest {
        platformVersions.forEach { (platform, versionsStr) ->
            attributes("Support-Versions-$platform" to versionsStr)
        }
    }
    minimize()
    mergeServiceFiles()
    doLast {
        copy {
            from(archiveFile)
            into(rootProject.layout.buildDirectory.dir("libs"))
            rename { rootProject.name + "-" + project.version + ".jar" }
        }
    }
}