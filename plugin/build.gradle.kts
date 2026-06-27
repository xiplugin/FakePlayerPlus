plugins {
    id("com.gradleup.shadow") version "9.2.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    maven("https://repo.okaeri.cloud/releases")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io/")
}

val nmsProjects = rootProject.subprojects.filter { it.path.startsWith(":nms:nms_") }

dependencies {
    implementation(project(":api"))
    nmsProjects.forEach(::implementation)
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

// Add standalone Minecraft/Paper versions here (e.g., "1.20.4") to
// test them quickly without creating a local NMS module.
// Tasks will be registered automatically.
val testVersions = listOf(
    "26.2"
)

tasks {
    nmsProjects.map { it.name.substringAfter("nms_v").replace('_', '.') }.plus(testVersions).forEach { version ->
        register<xyz.jpenilla.runpaper.task.RunServer>("runServer_$version") {
            group = "run paper"
            runDirectory(layout.projectDirectory.dir("run_$version").asFile)
            minecraftVersion(version)
            pluginJars(shadowJar.flatMap { it.archiveFile })
            doFirst {
                layout.projectDirectory.file("run_$version/eula.txt").asFile.apply {
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